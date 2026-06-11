from fastapi import APIRouter

from api.logging_config import get_logger, timed
from api.models.schemas import ChatRequest, ChatResponse, RuleResult
from api.services.qdrant_service import QdrantService
from api.services.llm_service import chat as llm_chat

router = APIRouter(prefix="/chat", tags=["Chat Assistant"])

qdrant = QdrantService()
log = get_logger("fastapi.chat")


@router.post("/", response_model=ChatResponse)
def chat(req: ChatRequest):
    """RAG-powered chat: retrieve relevant RICS rules then generate a grounded answer via GPT."""
    with timed(log, "chat", msg_chars=len(req.message), limit=req.limit,
               section=req.section or "-", ctx_chars=len(req.context or "")) as span:
        hits = qdrant.search(
            query=req.message,
            limit=req.limit,
            section=req.section,
        )
        answer = llm_chat(user_message=req.message, rules=hits, audit_context=req.context)
        span.set(answer_chars=len(answer or ""), sources=len(hits))
        return ChatResponse(
            answer=answer,
            sources=[RuleResult(**h) for h in hits],
        )
