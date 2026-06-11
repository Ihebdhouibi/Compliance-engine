from fastapi import APIRouter, HTTPException

from api.logging_config import get_logger
from api.models.schemas import (
    SearchRequest,
    SearchResponse,
    RuleResult,
    RuleDetailResponse,
    SectionSummary,
    EvidenceCheckRequest,
    EvidenceCheckResponse,
    EvidenceItem,
)
from api.services.qdrant_service import QdrantService

router = APIRouter(prefix="/rules", tags=["Rules & Search"])

qdrant = QdrantService()
log = get_logger("fastapi.rules")


@router.post("/search", response_model=SearchResponse)
def search_rules(req: SearchRequest):
    """Semantic search across the RICS knowledge base with optional filters."""
    log.info(f"search > {req.query!r} limit={req.limit} section={req.section or '-'}")
    hits = qdrant.search(
        query=req.query,
        limit=req.limit,
        section=req.section,
        applies_to=req.applies_to,
        entry_type=req.entry_type,
    )
    log.debug("search < " + ", ".join(f"{h['rule_id']}({h['score']:.2f})" for h in hits))
    return SearchResponse(
        query=req.query,
        results=[RuleResult(**h) for h in hits],
        total=len(hits),
    )


@router.get("/{rule_id}", response_model=RuleDetailResponse)
def get_rule(rule_id: str):
    """Fetch a single rule/definition by its ID (e.g. RICS-S3.1-06, GLOSS-01)."""
    payload = qdrant.get_rule(rule_id)
    if not payload:
        raise HTTPException(status_code=404, detail=f"Rule '{rule_id}' not found")
    related = qdrant.get_related(rule_id)
    return RuleDetailResponse(rule_id=rule_id, payload=payload, related=related)


@router.get("/", response_model=list[SectionSummary])
def list_sections():
    """List all RICS sections with rule counts."""
    return qdrant.list_sections()


@router.post("/evidence-check", response_model=EvidenceCheckResponse)
def evidence_check(req: EvidenceCheckRequest):
    """Retrieve source text and implied evidence for a compliance area."""
    log.info(f"evidence-check > {req.query!r} limit={req.limit}")
    hits = qdrant.search(query=req.query, limit=req.limit, entry_type="rule")
    evidence = []
    for h in hits:
        p = h["payload"]
        evidence.append(
            EvidenceItem(
                rule_id=h["rule_id"],
                section=p.get("section", ""),
                requirement_text=p.get("requirement_text", ""),
                source_text_verbatim=p.get("source_text_verbatim", ""),
                evidence_implied=p.get("evidence_implied", ""),
                score=h["score"],
            )
        )
    return EvidenceCheckResponse(query=req.query, evidence=evidence)
