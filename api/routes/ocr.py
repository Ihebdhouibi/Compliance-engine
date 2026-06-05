"""OCR HTTP routes (Step 1)."""
from __future__ import annotations

import os
import tempfile
import uuid
from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from api.services.ocr_queue import get_queue
from api.services.ocr_engine import get_engine

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


# ----------------------------------------------------------------------
# NEW: Direct file upload endpoint (synchronous, prints result to terminal)
# ----------------------------------------------------------------------
@router.post("/upload")
async def upload_and_ocr(file: UploadFile = File(...)):
    """
    Accept a file upload, run OCR synchronously, print the extracted text
    to the server terminal, and return the text in the JSON response.
    Useful for debugging and for Angular integration without the queue.
    """
    engine = get_engine()
    engine.initialize()

    # Save uploaded file to a temporary location
    suffix = os.path.splitext(file.filename)[1]
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        # Run OCR
        result = engine.recognize(tmp_path, mime=file.content_type)

        # Print to terminal for immediate visibility
        print("\n" + "=" * 60)
        print(f"OCR RESULT for file: {file.filename}")
        print("=" * 60)
        print(result.full_text)
        print("=" * 60 + "\n")

        # Return the extracted text to the client
        return {
            "filename": file.filename,
            "text": result.full_text,
            "page_count": result.page_count,
            "elapsed_ms": result.elapsed_ms,
        }
    except Exception as e:
        print(f"OCR error for {file.filename}: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        # Clean up temporary file
        try:
            os.unlink(tmp_path)
        except OSError:
            pass