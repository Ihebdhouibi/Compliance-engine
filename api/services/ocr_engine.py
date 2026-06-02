"""
OCR engine service – disabled when ENABLE_OCR=false.
No heavy dependencies (fitz, docx) are imported unless OCR is enabled.
"""
from __future__ import annotations

import logging
import os
import tempfile
import threading
import time
import uuid
from dataclasses import dataclass, field
from typing import Optional

logger = logging.getLogger(__name__)


@dataclass
class OcrPage:
    page: int
    text: str
    confidence: float = 0.0


@dataclass
class OcrResult:
    pages: list[OcrPage] = field(default_factory=list)
    engine: str = "none"
    elapsed_ms: int = 0

    @property
    def full_text(self) -> str:
        return "\n\n".join(p.text for p in self.pages if p.text)

    @property
    def page_count(self) -> int:
        return len(self.pages)


class OcrEngine:
    def __init__(self) -> None:
        self._initialized = False
        self._temp_dir = os.environ.get("OCR_TEMP_DIR") or os.path.join(
            tempfile.gettempdir(), "compliance-engine-ocr"
        )

    def initialize(self) -> None:
        if self._initialized:
            return
        if os.environ.get("ENABLE_OCR", "true").lower() != "true":
            logger.info("OCR is disabled (ENABLE_OCR=false)")
            self._initialized = True   # mark as initialized (but no real engine)
            return

        # If OCR were enabled, we would lazy‑import fitz/docx here.
        raise RuntimeError("OCR not implemented – set ENABLE_OCR=false to use the API without OCR.")

    def recognize(self, file_path: str, mime: Optional[str] = None) -> OcrResult:
        if not self._initialized:
            self.initialize()
        # OCR disabled → return empty result
        return OcrResult(pages=[], engine="none", elapsed_ms=0)


_engine: OcrEngine | None = None
def get_engine() -> OcrEngine:
    global _engine
    if _engine is None:
        _engine = OcrEngine()
    return _engine
