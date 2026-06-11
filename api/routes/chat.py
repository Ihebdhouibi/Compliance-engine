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
        log.debug(f"chat.message > {req.message!r}")
        if req.context:
            log.debug(f"chat.context > {req.context!r}")
        hits = qdrant.search(
            query=req.message,
            limit=req.limit,
            section=req.section,
        )
        log.debug("chat.sources > " + ", ".join(f"{h['rule_id']}({h['score']:.2f})" for h in hits))
        answer = llm_chat(user_message=req.message, rules=hits, audit_context=req.context)
        log.debug(f"chat.answer < {answer!r}")
        span.set(answer_chars=len(answer or ""), sources=len(hits))
        return ChatResponse(
            answer=answer,
            sources=[RuleResult(**h) for h in hits],
        )
