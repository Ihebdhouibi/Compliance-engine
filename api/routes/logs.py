"""Ingest endpoint for browser logs.

The Angular client ships its component logs here; each entry is re-emitted
through a ``ng.*`` logger so it lands in ``angular.log`` (the browser's own
log file), carrying the correlation id the browser recorded.
"""
import logging

from fastapi import APIRouter
from pydantic import BaseModel, Field

from api.logging_config import get_correlation_id, set_correlation_id

router = APIRouter(tags=["Client Logs"])

_LEVELS = {
    "debug": logging.DEBUG,
    "info": logging.INFO,
    "warn": logging.WARNING,
    "warning": logging.WARNING,
    "error": logging.ERROR,
}


class ClientLogEntry(BaseModel):
    level: str = "info"
    component: str = Field("ng.client", max_length=80)
    message: str = Field("", max_length=12000)
    cid: str | None = None
    data: str | None = Field(None, max_length=12000)


class ClientLogBatch(BaseModel):
    entries: list[ClientLogEntry] = Field(default_factory=list)


@router.post("/logs")
def ingest(batch: ClientLogBatch):
    """Persist a batch of browser log entries to angular.log."""
    saved = get_correlation_id()
    try:
        for e in batch.entries:
            component = e.component if e.component.startswith("ng.") else f"ng.{e.component}"
            set_correlation_id(e.cid or "-")
            suffix = f" {e.data}" if e.data else ""
            logging.getLogger(component).log(
                _LEVELS.get(e.level.lower(), logging.INFO), f"{e.message}{suffix}"
            )
    finally:
        set_correlation_id(saved)
    return {"received": len(batch.entries)}
