from fastapi import APIRouter

from api.logging_config import get_logger, timed
from api.models.schemas import RelevanceRequest, RelevanceResponse, RelevanceSegment
from api.services.relevance_service import score_relevance

router = APIRouter(prefix="/relevance", tags=["Evidence Relevance"])
log = get_logger("fastapi.relevance")


@router.post("/highlight", response_model=RelevanceResponse)
def highlight(req: RelevanceRequest):
    """Score each sentence of an evidence document against an audit question.

    Returns per-sentence relevance with character offsets so the workspace can
    shade the most relevant lines of the extracted text.
    """
    with timed(log, "highlight", text_chars=len(req.text), query_chars=len(req.query)) as span:
        log.debug(f"relevance.query > {req.query!r}")
        segments, truncated = score_relevance(req.text, req.query)
        strong = sum(1 for s in segments if s["level"] >= 2)
        for s in (x for x in segments if x["level"] >= 2):
            log.debug(f"relevance.match [{s['score']:.2f}] {req.text[s['start']:s['end']].strip()!r}")
        span.set(segments=len(segments), strong=strong, truncated=truncated)
        return RelevanceResponse(
            query=req.query,
            segments=[RelevanceSegment(**s) for s in segments],
            truncated=truncated,
        )
