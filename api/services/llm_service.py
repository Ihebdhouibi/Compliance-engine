import os

from api.config import QWEN_BASE_URL, QWEN_MODEL
from api.logging_config import get_logger, timed


_log = get_logger("svc.qwen")

# ── Ultra‑short system prompt ────────────────────────────────────────
SYSTEM_PROMPT = """You are a RICS Compliance Assistant. Base answers ONLY on provided rules. Cite rule IDs. Use verbatim text when quoting. If info missing, say so. Be concise."""

def build_context_block(rules: list[dict]) -> str:
    parts = []
    for r in rules:
        p = r["payload"]
        block = f"""--- Rule {p.get('rule_id', '?')} (Section {p.get('section', '?')} — {p.get('section_title', '')}) ---
Requirement: {p.get('requirement_text', '')}
Original RICS text: {p.get('source_text_verbatim', '')}
Evidence implied: {p.get('evidence_implied', '')}
Applies to: {p.get('applies_to', '')}
Trigger: {p.get('trigger', '')}
Related rules: {', '.join(p.get('related_rules', []))}"""
        parts.append(block)
    return "\n\n".join(parts)

def chat(user_message: str, rules: list[dict], audit_context: str | None = None) -> str:
    rules_block = build_context_block(rules)

    # ── CRITICAL: Truncate audit_context to 1500 characters ──
    if audit_context and len(audit_context) > 1500:
        audit_context = audit_context[:1500] + "... [truncated]"

    sections = [f"CONTEXT (retrieved RICS rules):\n{rules_block}"]
    if audit_context and audit_context.strip():
        sections.append(f"AUDIT CONTEXT (summary):\n{audit_context.strip()}")
    sections.append(f"QUESTION:\n{user_message}")

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": "\n\n".join(sections)},
    ]

    with timed(_log, "chat.completion", model=QWEN_MODEL, rules=len(rules),
               has_audit_ctx=bool(audit_context and audit_context.strip())) as span:
        response = _client.chat.completions.create(
            model=QWEN_MODEL,
            messages=messages,
            temperature=0.2,        # Deterministic for speed
            max_tokens=1500,
            timeout=900.0,
        )
        answer = response.choices[0].message.content
        usage = getattr(response, "usage", None)
        span.set(answer_chars=len(answer or ""),
                 tokens=getattr(usage, "total_tokens", None))
        return answer
