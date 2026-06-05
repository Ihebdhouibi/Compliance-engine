"""
OCR engine service using RapidOCR (ONNX Runtime).
Supports images and PDFs (converted to images). DOCX requires python-docx (optional).
"""
from __future__ import annotations

import logging
import os
import tempfile
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

import fitz  # PyMuPDF

logger = logging.getLogger(__name__)

# Lazy import of RapidOCR
_OCR = None

def _get_ocr():
    global _OCR
    if _OCR is None:
        from rapidocr_onnxruntime import RapidOCR
        _OCR = RapidOCR()
    return _OCR


@dataclass
class OcrPage:
    page: int
    text: str
    confidence: float = 0.0


@dataclass
class OcrResult:
    pages: list[OcrPage] = field(default_factory=list)
    engine: str = "rapidocr"
    elapsed_ms: int = 0

    @property
    def full_text(self) -> str:
        return "\n\n".join(p.text for p in self.pages if p.text)

    @property
    def page_count(self) -> int:
        return len(self.pages)


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


class OcrEngine:
    def __init__(self) -> None:
        self._initialized = False
        self._temp_dir = os.environ.get("OCR_TEMP_DIR") or os.path.join(
            tempfile.gettempdir(), "compliance-engine-ocr"
        )
        self._pdf_dpi = int(os.environ.get("OCR_PDF_DPI", "300"))  # increased DPI

    def initialize(self) -> None:
        if self._initialized:
            return
        os.makedirs(self._temp_dir, exist_ok=True)
        try:
            _get_ocr()
        except Exception as e:
            logger.warning(f"OCR warmup failed: {e}")
        self._initialized = True
        logger.info("RapidOCR engine initialized")

    def recognize(self, file_path: str, mime: Optional[str] = None) -> OcrResult:
        if not self._initialized:
            self.initialize()
        if not os.path.exists(file_path):
            raise FileNotFoundError(file_path)

        start = time.time()
        kind = _kind(file_path, mime)
        logger.info(f"OCR start file={file_path} kind={kind}")

        if kind == "image":
            pages = self._ocr_image(file_path)
        elif kind == "pdf":
            pages = self._ocr_pdf(file_path)
        elif kind == "docx":
            pages = self._ocr_docx(file_path)
        else:
            raise ValueError(f"Unsupported file type: {file_path} (mime={mime})")

        elapsed = int((time.time() - start) * 1000)
        return OcrResult(pages=pages, engine="rapidocr", elapsed_ms=elapsed)

    def _ocr_image(self, file_path: str) -> list[OcrPage]:
        result, _ = _get_ocr()(file_path)
        text = self._flatten_rapid_result(result)
        return [OcrPage(page=1, text=text, confidence=0.0)]

    def _ocr_pdf(self, file_path: str) -> list[OcrPage]:
        """Hybrid method: extract native text if available, otherwise fallback to OCR."""
        pages = []
        doc = fitz.open(file_path)
        try:
            for i, pdf_page in enumerate(doc, start=1):
                # First try to get native text directly
                text = pdf_page.get_text().strip()
                if text:
                    pages.append(OcrPage(page=i, text=text, confidence=1.0))
                else:
                    # Fallback to image OCR
                    zoom = self._pdf_dpi / 72.0
                    mat = fitz.Matrix(zoom, zoom)
                    pix = pdf_page.get_pixmap(matrix=mat, alpha=False)
                    tmp_img = os.path.join(self._temp_dir, f"pdfpage_{uuid.uuid4().hex}.png")
                    pix.save(tmp_img)
                    try:
                        result, _ = _get_ocr()(tmp_img)
                        ocr_text = self._flatten_rapid_result(result)
                        pages.append(OcrPage(page=i, text=ocr_text, confidence=0.0))
                    finally:
                        try:
                            os.remove(tmp_img)
                        except OSError:
                            pass
        finally:
            doc.close()
        return pages

    def _ocr_docx(self, file_path: str) -> list[OcrPage]:
        try:
            from docx import Document
        except ImportError:
            logger.warning("python-docx not installed. DOCX OCR unavailable.")
            return [OcrPage(page=1, text="", confidence=0.0)]

        pages = []
        doc = Document(file_path)
        chunks = []
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

        # Extract images from DOCX and OCR them
        img_idx = len(pages) + 1
        try:
            rels = doc.part._rels
            for rel in rels.values():
                if "image" not in getattr(rel, "reltype", ""):
                    continue
                try:
                    blob = rel.target_part.blob
                except Exception:
                    continue
                tmp_img = os.path.join(self._temp_dir, f"docximg_{uuid.uuid4().hex}.png")
                with open(tmp_img, "wb") as f:
                    f.write(blob)
                try:
                    result, _ = _get_ocr()(tmp_img)
                    text = self._flatten_rapid_result(result)
                    if text:
                        pages.append(OcrPage(page=img_idx, text=text, confidence=0.0))
                        img_idx += 1
                finally:
                    try:
                        os.remove(tmp_img)
                    except OSError:
                        pass
        except Exception as e:
            logger.warning(f"DOCX image extraction failed: {e}")

        if not pages:
            pages.append(OcrPage(page=1, text="", confidence=0.0))
        return pages

    @staticmethod
    def _flatten_rapid_result(result) -> str:
        if not result:
            return ""
        texts = []
        for line in result:
            if not line or len(line) < 2:
                continue
            text_info = line[1]
            if isinstance(text_info, (tuple, list)) and len(text_info) >= 1:
                text = str(text_info[0])
                if text:
                    texts.append(text)
        return "\n".join(texts)


# Singleton
_engine: OcrEngine | None = None

def get_engine() -> OcrEngine:
    global _engine
    if _engine is None:
        _engine = OcrEngine()
    return _engine