"""Structured, human-readable logging for the AI engine.

One consistent line format across every component, a correlation id threaded
through each request, and a ``timed`` helper that brackets an operation with
``> start`` / ``< end (Nms)`` lines so a flow is trivial to follow in the log.

Format:
    HH:MM:SS | LEVEL | component.name      | cid=ab12cd34 | message

Component names mirror the interaction map, e.g. ``fastapi.chat``,
``svc.qdrant``, ``svc.openai``, ``svc.embedder``, ``fastapi.relevance``.
"""
from __future__ import annotations

import logging
import sys
import time
import uuid
from contextlib import contextmanager
from contextvars import ContextVar

# ── correlation id (per request, propagated across log lines) ─────────────────
_correlation_id: ContextVar[str] = ContextVar("correlation_id", default="-")


def set_correlation_id(cid: str) -> None:
    _correlation_id.set(cid or "-")


def get_correlation_id() -> str:
    return _correlation_id.get()


def new_correlation_id() -> str:
    return uuid.uuid4().hex[:8]


class _CorrelationFilter(logging.Filter):
    """Inject the current correlation id onto every record as ``cid``."""

    def filter(self, record: logging.LogRecord) -> bool:
        record.cid = get_correlation_id()
        return True


_FORMAT = "%(asctime)s | %(levelname)-5s | %(name)-20s | cid=%(cid)s | %(message)s"
_DATEFMT = "%H:%M:%S"
_configured = False


def setup_logging(level: int = logging.INFO) -> None:
    """Install the shared handler/format on the root logger (idempotent)."""
    global _configured
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter(_FORMAT, datefmt=_DATEFMT))
    handler.addFilter(_CorrelationFilter())

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(level)

    # Quieten chatty third-party loggers so our lines stay readable.
    for noisy in ("httpx", "httpcore", "urllib3", "openai", "uvicorn.access"):
        logging.getLogger(noisy).setLevel(logging.WARNING)

    _configured = True


def get_logger(name: str) -> logging.Logger:
    return logging.getLogger(name)


def _fmt(fields: dict) -> str:
    return " ".join(f"{k}={v}" for k, v in fields.items() if v is not None)


class _Span:
    """Collects fields to emit on the closing log line of a ``timed`` block."""

    def __init__(self) -> None:
        self.fields: dict = {}

    def set(self, **kw) -> None:
        self.fields.update(kw)


@contextmanager
def timed(logger: logging.Logger, op: str, **start_fields):
    """Bracket an operation with start/end log lines and a duration.

        with timed(log, "search", q=query, limit=5) as span:
            hits = do_search()
            span.set(hits=len(hits))
    """
    start = _fmt(start_fields)
    logger.info(f"{op} >{(' ' + start) if start else ''}")
    span = _Span()
    t0 = time.perf_counter()
    try:
        yield span
    except Exception as exc:  # noqa: BLE001 - we re-raise after logging
        ms = int((time.perf_counter() - t0) * 1000)
        logger.error(f"{op} FAILED {type(exc).__name__}: {exc} ({ms}ms)")
        raise
    else:
        ms = int((time.perf_counter() - t0) * 1000)
        end = _fmt(span.fields)
        logger.info(f"{op} <{(' ' + end) if end else ''} ({ms}ms)")
