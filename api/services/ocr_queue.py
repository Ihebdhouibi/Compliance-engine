"""
In-process OCR job queue (Step 1).

A small, transparent asyncio queue with N worker tasks. Each job runs the
engine in a thread (run_in_executor) so the event loop stays free.

When step 5 lands we swap this file for a Redis/Celery-backed implementation;
the public surface (`enqueue`, `get_status`, `start_workers`, `stop_workers`)
stays the same.
"""
from __future__ import annotations

import asyncio
import logging
import os
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional

import httpx
from dotenv import load_dotenv

from api.services.ocr_engine import get_engine, OcrResult

# Ensure .env is loaded before reading OCR_* vars, regardless of import order.
load_dotenv()

logger = logging.getLogger(__name__)

NUM_WORKERS = int(os.getenv("OCR_NUM_WORKERS", "2"))
CALLBACK_URL = os.getenv("OCR_CALLBACK_URL", "")
CALLBACK_SECRET = os.getenv("OCR_CALLBACK_SECRET", "")
logger.info(
    "[ocr-queue] config: workers=%d callback_url=%s callback_secret_len=%d",
    NUM_WORKERS,
    CALLBACK_URL or "(unset)",
    len(CALLBACK_SECRET),
)


@dataclass
class OcrJob:
    id: str
    audit_id: int
    media_id: int
    file_path: str
    mime: Optional[str] = None
    status: str = "PENDING"                  # PENDING | RUNNING | DONE | FAILED
    result: Optional[OcrResult] = None
    error: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.utcnow)
    updated_at: datetime = field(default_factory=datetime.utcnow)


class OcrQueue:
    def __init__(self) -> None:
        self._queue: asyncio.Queue[OcrJob] = asyncio.Queue()
        self._jobs: dict[str, OcrJob] = {}
        # idempotency: (audit_id, media_id) → job_id
        self._index: dict[tuple[int, int], str] = {}
        self._workers: list[asyncio.Task] = []
        self._stopping = False

    # ── public API ────────────────────────────────────────────────────────

    async def enqueue(
        self,
        audit_id: int,
        media_id: int,
        file_path: str,
        mime: Optional[str] = None,
    ) -> OcrJob:
        key = (audit_id, media_id)
        if key in self._index:
            existing = self._jobs[self._index[key]]
            if existing.status in ("PENDING", "RUNNING", "DONE"):
                return existing
        job = OcrJob(
            id=str(uuid.uuid4()),
            audit_id=audit_id,
            media_id=media_id,
            file_path=file_path,
            mime=mime,
        )
        self._jobs[job.id] = job
        self._index[key] = job.id
        await self._queue.put(job)
        logger.info(
            "[OCR-QUEUE] enqueued job=%s audit=%s media=%s file=%s",
            job.id, audit_id, media_id, file_path,
        )
        return job

    def get(self, job_id: str) -> Optional[OcrJob]:
        return self._jobs.get(job_id)

    def list_for_audit(self, audit_id: int) -> list[OcrJob]:
        return [j for j in self._jobs.values() if j.audit_id == audit_id]

    # ── lifecycle ─────────────────────────────────────────────────────────

    def start_workers(self) -> None:
        if self._workers:
            return
        engine = get_engine()
        engine.initialize()
        loop = asyncio.get_event_loop()
        for i in range(NUM_WORKERS):
            self._workers.append(loop.create_task(self._worker(i)))
        logger.info("[OCR-QUEUE] started %d worker(s)", NUM_WORKERS)

    async def stop_workers(self) -> None:
        self._stopping = True
        for t in self._workers:
            t.cancel()
        for t in self._workers:
            try:
                await t
            except asyncio.CancelledError:
                pass
        self._workers.clear()
        logger.info("[OCR-QUEUE] workers stopped")

    # ── worker loop ───────────────────────────────────────────────────────

    async def _worker(self, idx: int) -> None:
        engine = get_engine()
        loop = asyncio.get_event_loop()
        logger.info("[OCR-WORKER-%d] started", idx)
        while not self._stopping:
            job = await self._queue.get()
            job.status = "RUNNING"
            job.updated_at = datetime.utcnow()
            try:
                # Run blocking OCR in a thread so the event loop stays responsive.
                result = await loop.run_in_executor(
                    None, engine.recognize, job.file_path, job.mime
                )
                job.result = result
                job.status = "DONE"
            except Exception as e:                       # noqa: BLE001
                logger.exception("[OCR-WORKER-%d] job %s failed", idx, job.id)
                job.status = "FAILED"
                job.error = str(e)
            finally:
                job.updated_at = datetime.utcnow()
                self._queue.task_done()
                await self._notify_callback(job)

    # ── Spring callback ───────────────────────────────────────────────────

    async def _notify_callback(self, job: OcrJob) -> None:
        if not CALLBACK_URL:
            return
        payload = {
            "jobId":     job.id,
            "auditId":   job.audit_id,
            "mediaId":   job.media_id,
            "status":    job.status,
            "error":     job.error,
            "pageCount": job.result.page_count if job.result else 0,
            "engine":    job.result.engine if job.result else None,
            "elapsedMs": job.result.elapsed_ms if job.result else 0,
            "text":      job.result.full_text if job.result else None,
        }
        headers = {"X-OCR-Secret": CALLBACK_SECRET} if CALLBACK_SECRET else {}
        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                await client.post(CALLBACK_URL, json=payload, headers=headers)
        except Exception as e:                            # noqa: BLE001
            logger.warning("[OCR-QUEUE] callback failed for %s: %s", job.id, e)


# ── module-level singleton ────────────────────────────────────────────────────
_queue: OcrQueue | None = None


def get_queue() -> OcrQueue:
    global _queue
    if _queue is None:
        _queue = OcrQueue()
    return _queue
