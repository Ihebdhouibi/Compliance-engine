"""OCR HTTP routes (Step 1)."""
from __future__ import annotations

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from api.services.ocr_queue import get_queue

router = APIRouter(prefix="/ocr", tags=["OCR"])


class OcrJobRequest(BaseModel):
    audit_id: int = Field(..., description="AuditRequest.id from Spring")
    media_id: int = Field(..., description="MediaModel.id")
    file_path: str = Field(..., description="Absolute path or URL the worker can read")
    mime:     str | None = Field(None, description="Optional MIME hint")


class OcrJobStatus(BaseModel):
    id:        str
    audit_id:  int
    media_id:  int
    status:    str
    error:     str | None
    page_count: int = 0
    engine:    str | None = None
    elapsed_ms: int = 0


def _to_status(job) -> OcrJobStatus:
    return OcrJobStatus(
        id=job.id,
        audit_id=job.audit_id,
        media_id=job.media_id,
        status=job.status,
        error=job.error,
        page_count=job.result.page_count if job.result else 0,
        engine=job.result.engine if job.result else None,
        elapsed_ms=job.result.elapsed_ms if job.result else 0,
    )


@router.post("/jobs", response_model=OcrJobStatus, status_code=202)
async def enqueue_job(req: OcrJobRequest):
    job = await get_queue().enqueue(
        audit_id=req.audit_id,
        media_id=req.media_id,
        file_path=req.file_path,
        mime=req.mime,
    )
    return _to_status(job)


@router.get("/jobs/{job_id}", response_model=OcrJobStatus)
def get_job(job_id: str):
    job = get_queue().get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job not found")
    return _to_status(job)


@router.get("/audits/{audit_id}/jobs", response_model=list[OcrJobStatus])
def list_jobs_for_audit(audit_id: int):
    return [_to_status(j) for j in get_queue().list_for_audit(audit_id)]
