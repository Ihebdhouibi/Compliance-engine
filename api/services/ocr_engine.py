"""
OCR engine service — Step 2 (real PaddleOCR).

Exposes a single process-wide engine instance, initialized once at FastAPI
startup via the lifespan handler in main.py. Supports:

    * Images   (.jpg .jpeg .png .bmp .tif .tiff .webp)  — single page OCR
    * PDFs     (.pdf)                                    — page-by-page OCR (PyMuPDF rasterize)
    * DOCX     (.docx)                                   — paragraphs + tables direct text;
                                                           embedded images OCR'd as extra pages

Heavy imports (paddleocr / paddle / fitz / docx) are deferred until
``initialize()`` so module-load stays cheap. Windows-specific oneDNN/PIR
env flags are applied BEFORE paddleocr is imported.
"""
from __future__ import annotations

import logging
import os
import platform
import tempfile
import threading
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

logger = logging.getLogger(__name__)


# ── public dataclasses ────────────────────────────────────────────────────────
@dataclass
class OcrPage:
    page: int
    text: str
    confidence: float = 0.0


@dataclass
class OcrResult:
    pages: list[OcrPage] = field(default_factory=list)
    engine: str = "paddleocr"
    elapsed_ms: int = 0

    @property
    def full_text(self) -> str:
        return "\n\n".join(p.text for p in self.pages if p.text)

    @property
    def page_count(self) -> int:
        return len(self.pages)


# ── mime / extension helpers ──────────────────────────────────────────────────
_IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".bmp", ".tif", ".tiff", ".webp"}
_PDF_EXTS = {".pdf"}
_DOCX_EXTS = {".docx"}


def _kind(file_path: str, mime: Optional[str]) -> str:
    ext = os.path.splitext(file_path)[1].lower()
    if ext in _IMAGE_EXTS:
        return "image"
    if ext in _PDF_EXTS:
        return "pdf"
    if ext in _DOCX_EXTS:
        return "docx"
    if mime:
        m = mime.lower()
        if m.startswith("image/"):
            return "image"
        if m == "application/pdf":
            return "pdf"
        if "wordprocessingml" in m or "officedocument" in m:
            return "docx"
    return "unknown"


# ── env preparation (must run BEFORE paddleocr import) ────────────────────────
def _apply_paddle_env_flags() -> None:
    """Configure oneDNN / PIR flags before paddle is imported.

    On Windows the stock paddlepaddle 3.x wheel has known PIR+oneDNN
    issues for some predictors. We default oneDNN OFF on Windows to keep
    the first-cut integration robust; override with PADDLE_USE_MKLDNN=1.
    """
    if platform.system() == "Windows":
        use_mkldnn = os.environ.get("PADDLE_USE_MKLDNN", "0")
        os.environ.setdefault("FLAGS_use_mkldnn", use_mkldnn)
        os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", use_mkldnn)
        os.environ.setdefault("FLAGS_enable_pir_in_executor", "0")
        os.environ.setdefault("FLAGS_enable_pir_api", "0")


# ── engine ────────────────────────────────────────────────────────────────────
class OcrEngine:
    """Real PaddleOCR engine. Single instance per process; workers serialize
    ``predict()`` calls via the asyncio executor in ``ocr_queue``."""

    def __init__(self) -> None:
        self._initialized = False
        self._ocr = None
        self._lang = os.environ.get("OCR_LANG", "en")
        self._text_det_box_thresh = 0.5
        self._auto_rotate = True
        self._pdf_dpi = int(os.environ.get("OCR_PDF_DPI", "200"))
        self._temp_dir = os.environ.get("OCR_TEMP_DIR") or os.path.join(
            tempfile.gettempdir(), "compliance-engine-ocr"
        )
        # PaddleOCR's underlying C++ predictor is NOT thread-safe.
        # All predict() calls must be serialized across worker threads,
        # otherwise tensors get clobbered ("Tensor holds no memory",
        # "EventStatus shall be not SCHEDULED"). The asyncio queue still
        # parallelizes I/O (PDF rasterize, file ops); only inference is
        # mutually exclusive.
        self._predict_lock = threading.Lock()

    # ── lifecycle ────────────────────────────────────────────────────────────
    def initialize(self) -> None:
        if self._initialized:
            return

        os.makedirs(self._temp_dir, exist_ok=True)
        _apply_paddle_env_flags()

        t0 = time.time()
        logger.info(
            "[OCR] Initializing PaddleOCR (lang=%s, det_box_thresh=%.2f, auto_rotate=%s)...",
            self._lang, self._text_det_box_thresh, self._auto_rotate,
        )

        from paddleocr import PaddleOCR  # noqa: WPS433  -- heavy lazy import

        # Use mobile models by default (~5x faster than server models on CPU
        # with negligible accuracy loss for evidence-document use cases).
        # Override via OCR_DET_MODEL / OCR_REC_MODEL env vars.
        det_model = os.environ.get("OCR_DET_MODEL", "PP-OCRv5_mobile_det")
        rec_model = os.environ.get("OCR_REC_MODEL", "PP-OCRv5_mobile_rec")

        self._ocr = PaddleOCR(
            lang=self._lang,
            text_detection_model_name=det_model,
            text_recognition_model_name=rec_model,
            use_doc_orientation_classify=self._auto_rotate,
            use_doc_unwarping=False,
            use_textline_orientation=False,
            text_det_box_thresh=self._text_det_box_thresh,
        )

        try:
            self._warmup()
        except Exception as warm_err:  # noqa: BLE001
            logger.warning("[OCR] Warmup failed (continuing): %r", warm_err)

        self._initialized = True
        logger.info("[OCR] PaddleOCR ready in %.2fs", time.time() - t0)

    def _warmup(self) -> None:
        import numpy as np  # noqa: WPS433
        from PIL import Image, ImageDraw  # noqa: WPS433

        img = Image.new("RGB", (320, 80), "white")
        draw = ImageDraw.Draw(img)
        draw.text((10, 20), "OCR warmup", fill="black")
        arr = np.array(img)
        self._ocr.predict(arr)

    # ── public API ───────────────────────────────────────────────────────────
    def recognize(self, file_path: str, mime: Optional[str] = None) -> OcrResult:
        if not self._initialized:
            raise RuntimeError("OcrEngine not initialized — call initialize() first.")
        if not os.path.exists(file_path):
            raise FileNotFoundError(file_path)

        # Serialize the ENTIRE recognition for one file. PaddleX's pipeline
        # chains multiple sub-models (doc orient → det → rec), and they
        # share internal predictor state. Locking only individual predict()
        # calls still lets a second job interleave between sub-models and
        # corrupt the first job's tensors. One job at a time is the safe
        # contract; the queue's parallelism still buys us I/O overlap
        # (rasterize page N+1 while page N is being recognized? no — same
        # engine — but uploads/callbacks/file ops do overlap).
        with self._predict_lock:
            return self._recognize_locked(file_path, mime)

    def _recognize_locked(self, file_path: str, mime: Optional[str]) -> OcrResult:
        start = time.time()
        kind = _kind(file_path, mime)
        logger.info("[OCR] recognize start file=%s kind=%s mime=%s", file_path, kind, mime)

        if kind == "image":
            pages = self._ocr_image(file_path)
        elif kind == "pdf":
            pages = self._ocr_pdf(file_path)
        elif kind == "docx":
            pages = self._ocr_docx(file_path)
        else:
            raise ValueError(f"Unsupported file type for OCR: {file_path} (mime={mime})")

        elapsed = int((time.time() - start) * 1000)
        logger.info(
            "[OCR] recognize done file=%s pages=%d elapsed=%dms",
            file_path, len(pages), elapsed,
        )
        return OcrResult(pages=pages, engine="paddleocr", elapsed_ms=elapsed)

    # ── per-kind extractors ──────────────────────────────────────────────────
    def _ocr_image(self, file_path: str) -> list[OcrPage]:
        result = self._ocr.predict(file_path)
        text, conf = _flatten_paddle_result(result)
        return [OcrPage(page=1, text=text, confidence=conf)]

    def _ocr_pdf(self, file_path: str) -> list[OcrPage]:
        import fitz  # PyMuPDF  # noqa: WPS433

        pages: list[OcrPage] = []
        doc = fitz.open(file_path)
        try:
            zoom = self._pdf_dpi / 72.0
            mat = fitz.Matrix(zoom, zoom)
            for i, pdf_page in enumerate(doc, start=1):
                pix = pdf_page.get_pixmap(matrix=mat, alpha=False)
                tmp_path = os.path.join(
                    self._temp_dir, f"pdfpage_{uuid.uuid4().hex}.png"
                )
                pix.save(tmp_path)
                try:
                    result = self._ocr.predict(tmp_path)
                    text, conf = _flatten_paddle_result(result)
                    pages.append(OcrPage(page=i, text=text, confidence=conf))
                finally:
                    try:
                        os.remove(tmp_path)
                    except OSError:
                        pass
        finally:
            doc.close()
        return pages

    def _ocr_docx(self, file_path: str) -> list[OcrPage]:
        from docx import Document  # python-docx  # noqa: WPS433

        pages: list[OcrPage] = []
        doc = Document(file_path)

        chunks: list[str] = []
        for para in doc.paragraphs:
            t = (para.text or "").strip()
            if t:
                chunks.append(t)
        for table in doc.tables:
            for row in table.rows:
                cells = [(c.text or "").strip() for c in row.cells]
                joined = " | ".join(c for c in cells if c)
                if joined:
                    chunks.append(joined)
        if chunks:
            pages.append(OcrPage(page=1, text="\n".join(chunks), confidence=1.0))

        page_index = len(pages) + 1
        try:
            rels = doc.part._rels  # python-docx internal — stable across releases
            for rel in rels.values():
                if "image" not in getattr(rel, "reltype", ""):
                    continue
                try:
                    blob = rel.target_part.blob
                except Exception:  # noqa: BLE001
                    continue
                tmp_path = os.path.join(
                    self._temp_dir, f"docximg_{uuid.uuid4().hex}.png"
                )
                with open(tmp_path, "wb") as f:
                    f.write(blob)
                try:
                    result = self._ocr.predict(tmp_path)
                    text, conf = _flatten_paddle_result(result)
                    if text:
                        pages.append(OcrPage(page=page_index, text=text, confidence=conf))
                        page_index += 1
                finally:
                    try:
                        os.remove(tmp_path)
                    except OSError:
                        pass
        except Exception as e:  # noqa: BLE001
            logger.warning("[OCR] DOCX embedded-image extraction failed: %r", e)

        if not pages:
            pages.append(OcrPage(page=1, text="", confidence=0.0))
        return pages


# ── helpers ──────────────────────────────────────────────────────────────────
def _flatten_paddle_result(result) -> tuple[str, float]:
    """Extract joined text + mean confidence from a PaddleOCR 3.x predict() result."""
    if not result:
        return "", 0.0

    item = result[0]
    texts = None
    scores = None

    # Path A: dict-like access (PaddleOCR 3.5+).
    try:
        texts = item["rec_texts"]  # type: ignore[index]
        try:
            scores = item["rec_scores"]  # type: ignore[index]
        except Exception:  # noqa: BLE001
            scores = None
    except Exception:  # noqa: BLE001
        texts = None

    # Path B: .json['res'] dict.
    if texts is None and hasattr(item, "json"):
        try:
            res = item.json.get("res") or {}
            texts = res.get("rec_texts") or res.get("texts")
            scores = res.get("rec_scores") or res.get("scores")
        except Exception:  # noqa: BLE001
            texts = None

    # Path C: legacy 2.x — list[ [box, (text, score)] ]
    if texts is None:
        try:
            lines = item if isinstance(item, list) else []
            texts = []
            scores = []
            for line in lines:
                if not line:
                    continue
                _, ts = line[0], line[1]
                if isinstance(ts, (tuple, list)) and len(ts) >= 2:
                    texts.append(str(ts[0]))
                    scores.append(float(ts[1]))
        except Exception:  # noqa: BLE001
            texts = []

    texts = [str(t) for t in (texts or []) if t]
    if not texts:
        return "", 0.0

    joined = "\n".join(texts)
    mean_conf = 0.0
    if scores:
        try:
            mean_conf = float(sum(float(s) for s in scores) / len(scores))
        except Exception:  # noqa: BLE001
            mean_conf = 0.0
    return joined, mean_conf


# ── module-level singleton ───────────────────────────────────────────────────
_engine: OcrEngine | None = None


def get_engine() -> OcrEngine:
    global _engine
    if _engine is None:
        _engine = OcrEngine()
    return _engine
