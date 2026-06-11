from fastapi import APIRouter

from api.models.schemas import RelevanceRequest, RelevanceResponse, RelevanceSegment
from api.services.relevance_service import score_relevance

router = APIRouter(prefix="/relevance", tags=["Evidence Relevance"])


@router.post("/highlight", response_model=RelevanceResponse)
def highlight(req: RelevanceRequest):
    """Score each sentence of an evidence document against an audit question.

    Returns per-sentence relevance with character offsets so the workspace can
    shade the most relevant lines of the extracted text.
    """
    segments, truncated = score_relevance(req.text, req.query)
    return RelevanceResponse(
        query=req.query,
        segments=[RelevanceSegment(**s) for s in segments],
        truncated=truncated,
    )
