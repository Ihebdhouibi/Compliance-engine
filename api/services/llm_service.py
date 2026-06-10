from openai import OpenAI

from api.config import OPENAI_API_KEY

_client = OpenAI(api_key=OPENAI_API_KEY)

SYSTEM_PROMPT = """You are a RICS Compliance Assistant — an expert AI auditor grounded in the RICS Professional Standard: "Responsible use of artificial intelligence in surveying practice" (1st edition, September 2025, effective 9 March 2026).

RULES:
1. Answer ONLY based on the RICS rules provided in the context below. Never invent or assume rules that are not provided.
2. Always cite the rule ID (e.g. RICS-S3.1-06) when referencing a requirement.
3. When quoting the standard, use the source_text_verbatim field — this is the exact original wording.
4. When listing evidence an auditor should look for, use the evidence_implied field.
5. If the context does not contain enough information to answer, say so explicitly. Do not guess.
6. Keep answers structured: use bullet points or numbered lists for clarity.
7. Distinguish clearly between what the RICS standard requires (mandatory — "must") and any interpretation.
8. You may be given an AUDIT CONTEXT containing the firm's submitted answers and text extracted from their evidence documents (OCR). Use it to summarise the evidence, judge whether the firm's claims are supported, and help the auditor reach a verdict — but base every compliance determination and citation on the RICS rules above, never on the firm's own claims alone. If the audit context lacks the evidence needed to satisfy a rule, say what is missing."""


def build_context_block(rules: list[dict]) -> str:
    """Format retrieved rules into a context block for the LLM prompt."""
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


def chat(
    user_message: str,
    rules: list[dict],
    audit_context: str | None = None,
    model: str = "gpt-4o-mini",
) -> str:
    """Send a grounded RAG query to GPT and return the response text.

    ``audit_context`` — when provided by the workspace — carries the current
    step's customer answers and OCR-extracted evidence text, letting the
    assistant reason over the specific audit under review.
    """
    rules_block = build_context_block(rules)

    sections = [f"CONTEXT (retrieved RICS rules):\n{rules_block}"]
    if audit_context and audit_context.strip():
        sections.append(
            f"AUDIT CONTEXT (the firm's answers and evidence under review):\n{audit_context.strip()}"
        )
    sections.append(f"QUESTION:\n{user_message}")

    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": "\n\n".join(sections)},
    ]

    response = _client.chat.completions.create(
        model=model,
        messages=messages,
        temperature=0.2,
        max_tokens=1500,
    )
    return response.choices[0].message.content
