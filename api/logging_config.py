"""Structured, human-readable logging for the AI engine.

One consistent line format across every component, a correlation id threaded
through each request, and a ``timed`` helper that brackets an operation with
``> start`` / ``< end (Nms)`` lines so a flow is trivial to follow in the log.

Format:
    HH:MM:SS | LEVEL | component.name      | cid=ab12cd34 | message

Component names mirror the interaction map, e.g. ``fastapi.chat``,
``svc.qdrant``, ``svc``, ``svc.embedder``, ``fastapi.relevance``.
"""
from __future__ import annotations

import logging
import os
import sys
import time
import uuid
from contextlib import contextmanager
from contextvars import ContextVar
from logging.handlers import RotatingFileHandler

# Per-component log files live here (gitignored). Each service writes its own
# file so the logs read as if each microservice kept its own log.
LOG_DIR = os.environ.get("LOG_DIR", "logs")
_MAX_BYTES = 10 * 1024 * 1024
_BACKUPS = 5

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


class _NameFilter(logging.Filter):
    """Route records to a file by logger-name prefix (include / exclude)."""

    def __init__(self, include: tuple = (), exclude: tuple = ()):
        super().__init__()
        self.include = include
        self.exclude = exclude

    def filter(self, record: logging.LogRecord) -> bool:
        name = record.name
        if any(name.startswith(p) for p in self.exclude):
            return False
        if self.include:
            return any(name.startswith(p) for p in self.include)
        return True


_FORMAT = "%(asctime)s | %(levelname)-5s | %(name)-20s | cid=%(cid)s | %(message)s"
_DATEFMT = "%H:%M:%S"
_configured = False


def _file_handler(filename: str, include=(), exclude=()) -> RotatingFileHandler:
    h = RotatingFileHandler(
        os.path.join(LOG_DIR, filename), maxBytes=_MAX_BYTES,
        backupCount=_BACKUPS, encoding="utf-8",
    )
    h.setFormatter(logging.Formatter(_FORMAT, datefmt=_DATEFMT))
    h.addFilter(_CorrelationFilter())
    h.addFilter(_NameFilter(include=include, exclude=exclude))
    return h


def setup_logging(level: int = logging.DEBUG) -> None:
    """Install console + per-component file handlers on the root logger.

    Files (under ``LOG_DIR``):
      * ``qdrant.log``  — vector-store calls (``svc.qdrant``)
      * ``angular.log`` — browser logs shipped to ``POST /logs`` (``ng.*``)
      * ``fastapi.log`` — everything else in this process
    """
    os.makedirs(LOG_DIR, exist_ok=True)

    console = logging.StreamHandler(sys.stdout)
    console.setFormatter(logging.Formatter(_FORMAT, datefmt=_DATEFMT))
    console.addFilter(_CorrelationFilter())
    console.addFilter(_NameFilter(exclude=("ng.",)))  # browser logs only to file

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(console)
    root.addHandler(_file_handler("qdrant.log", include=("svc.qdrant",)))
    root.addHandler(_file_handler("angular.log", include=("ng.",)))
    root.addHandler(_file_handler("fastapi.log", exclude=("svc.qdrant", "ng.")))
    root.setLevel(level)

    


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
