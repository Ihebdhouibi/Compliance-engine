from pydantic import BaseModel, Field


# ── Request models ────────────────────────────────────────────────────────────

class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000, description="Natural-language search query")
    limit: int = Field(5, ge=1, le=20, description="Max results to return")
    section: str | None = Field(None, description="Filter by RICS section (e.g. '3.1')")
    applies_to: str | None = Field(None, description="Filter by audience (members, regulated_firms, members_and_firms)")
    entry_type: str | None = Field(None, description="Filter by type (rule, glossary, document_definition, scope_context)")


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000, description="Auditor's question")
    limit: int = Field(5, ge=1, le=10, description="Number of rules to retrieve for context")
    section: str | None = Field(None, description="Optional section filter")
    context: str | None = Field(
        None,
        max_length=24000,
        description="Optional audit context (current step, customer answers, evidence/OCR text) "
                    "to ground the assistant in the audit under review",
    )


class EvidenceCheckRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000, description="Describe the compliance area to check")
    limit: int = Field(5, ge=1, le=10)


# ── Response models ───────────────────────────────────────────────────────────

class RuleResult(BaseModel):
    rule_id: str
    score: float
    entry_type: str
    payload: dict


class SearchResponse(BaseModel):
    query: str
    results: list[RuleResult]
    total: int


class EvidenceItem(BaseModel):
    rule_id: str
    section: str
    requirement_text: str
    source_text_verbatim: str
    evidence_implied: str
    score: float


class EvidenceCheckResponse(BaseModel):
    query: str
    evidence: list[EvidenceItem]


class ChatResponse(BaseModel):
    answer: str
    sources: list[RuleResult]


class RuleDetailResponse(BaseModel):
    rule_id: str
    payload: dict
    related: list[dict] = []


class SectionSummary(BaseModel):
    section: str
    section_title: str
    rule_count: int
