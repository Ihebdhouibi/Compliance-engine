"""
Convert the client-delivered Level 1 and Level 2 questionnaire xlsx files
into JSON resources consumed by the Spring Boot seeders.

Outputs:
  src/main/resources/knowledge_base/level1_questionnaire.v1.json
  src/main/resources/knowledge_base/level2_rics_questions.v1.json
  src/main/resources/knowledge_base/level1_option_lists.v1.json

Run with the project venv:
  & venv\\Scripts\\python.exe scripts\\xlsx_to_template_json.py
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "Docs"
OUT_DIR = ROOT / "src" / "main" / "resources" / "knowledge_base"
OUT_DIR.mkdir(parents=True, exist_ok=True)

L1_XLSX = DOCS / "responsible_ai_level_1_questionnaire_regenerated.xlsx"
L2_XLSX = DOCS / "rics_level_2_audit_questions_workbook.xlsx"


# ─────────────────────────────── helpers ────────────────────────────────

def _clean(v: Any) -> Any:
    if v is None:
        return None
    if isinstance(v, str):
        s = v.strip()
        return s if s else None
    return v


def _slug(name: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "_", (name or "").lower()).strip("_")
    return s or "field"


def _read_sheet_rows(wb, sheet_name: str) -> list[dict]:
    """Read a sheet with the first row as headers."""
    ws = wb[sheet_name]
    rows = list(ws.iter_rows(values_only=True))
    if not rows:
        return []
    headers = [(_clean(h) or "").strip() for h in rows[0]]
    out: list[dict] = []
    for r in rows[1:]:
        if not any(_clean(c) is not None for c in r):
            continue
        out.append({headers[i]: _clean(r[i]) for i in range(len(headers))})
    return out


# ──────────────────────────── field-type mapping ────────────────────────

# Workbook response-type string → Spring FieldType enum value.
RESPONSE_TO_FIELDTYPE = {
    "single select": "RADIO",
    "multi-select": "MULTI_CHECKBOX",
    "free text": "TEXT",
    "long text": "TEXTAREA",
    "numeric/band": "TEXT",
    "numeric": "NUMBER",
    "year": "NUMBER",
    "url": "TEXT",
    "email": "EMAIL",
    "date": "DATE",
    "auto date": "DATE",
    "country dropdown": "DROPDOWN",
}


def map_field_type(response_type: str | None, label: str | None = None) -> str:
    if not response_type:
        return "TEXT"
    key = response_type.strip().lower()
    if key in RESPONSE_TO_FIELDTYPE:
        return RESPONSE_TO_FIELDTYPE[key]
    # Heuristics for niche labels
    if "select" in key and "multi" in key:
        return "MULTI_CHECKBOX"
    if "select" in key:
        return "RADIO"
    if "text" in key and "long" in key:
        return "TEXTAREA"
    if "text" in key:
        return "TEXT"
    return "TEXT"


# ─────────────────────────── Level 1 conversion ─────────────────────────

def convert_level1() -> tuple[dict, dict]:
    wb = load_workbook(L1_XLSX, data_only=True)

    # ── Option lists from the "Dropdown Lists" sheet
    ws = wb["Dropdown Lists"]
    raw = list(ws.iter_rows(values_only=True))
    headers = [(_clean(h) or "").strip() for h in raw[0]]
    option_lists: dict[str, list[str]] = {h: [] for h in headers if h}
    for r in raw[1:]:
        for idx, h in enumerate(headers):
            if not h or idx >= len(r):
                continue
            v = _clean(r[idx])
            if v is not None:
                option_lists[h].append(str(v))

    option_lists_payload = {
        "version": 1,
        "lists": [
            {"key": k, "items": [{"value": v, "label": v} for v in vals]}
            for k, vals in option_lists.items()
            if vals
        ],
    }

    # ── Sections
    sections = {row["Section ID"]: row for row in _read_sheet_rows(wb, "Sections")}

    # ── Questions
    questions = _read_sheet_rows(wb, "Level 1 Questions")

    # Group by section, preserve order
    grouped: dict[str, list[dict]] = {}
    section_order: list[str] = []
    for q in questions:
        sid = q.get("Section ID") or "S0"
        if sid not in grouped:
            grouped[sid] = []
            section_order.append(sid)
        grouped[sid].append(q)

    steps_payload = []
    step_order = 1
    for sid in section_order:
        sec_meta = sections.get(sid, {})
        fields_payload = []
        field_order = 1
        for q in grouped[sid]:
            label = q.get("Question") or ""
            response_type = q.get("Response Type") or ""
            field_type = map_field_type(response_type, label)
            option_source = q.get("Dropdown / Option Source")
            field_key = q.get("Field Name") or _slug(label)[:60]

            # Required mapping: "Yes" → true, "Conditional"/"No" → false
            required_raw = (q.get("Required?") or "").strip().lower()
            required = required_raw == "yes"

            # Visibility rule from Developer Notes "Show if X.Y = Yes"
            visibility = _parse_visibility(q.get("Developer Notes"), questions)

            field = {
                "fieldKey": field_key,
                "label": label,
                "placeholder": q.get("Developer Notes") or None,
                "fieldType": field_type,
                "required": required,
                "fieldOrder": field_order,
                "optionSourceKey": option_source if option_source else None,
                "visibilityRule": visibility,
                "routingTags": _routing_tags(q),
            }
            fields_payload.append(field)
            field_order += 1

        steps_payload.append(
            {
                "sectionId": sid,
                "stepOrder": step_order,
                "title": sec_meta.get("Section Name") or sid,
                "description": sec_meta.get("Purpose") or "",
                "fields": fields_payload,
            }
        )
        step_order += 1

    template_payload = {
        "version": 1,
        "auditType": "AI_READINESS_REVIEW",
        "level": "LEVEL_1",
        "title": "Responsible AI – Level 1 Firm Profile",
        "description": "Captures firm profile and market-insight data. Output is a "
        "routing profile that determines package, Level 2 pathway, "
        "active modules and evidence depth.",
        "steps": steps_payload,
    }

    return template_payload, option_lists_payload


def _routing_tags(q: dict) -> list[str]:
    tags: list[str] = []
    use = (q.get("Routing / Insight Use") or "").strip()
    if use:
        tags.append("use:" + use)
    sid = q.get("Section ID")
    if sid:
        tags.append("section:" + sid)
    return tags


# Build a lookup of "1.8" → field_key once
def _question_index(all_questions: list[dict]) -> dict[str, str]:
    idx = {}
    for q in all_questions:
        num = str(q.get("Question No.") or "").strip()
        key = q.get("Field Name")
        if num and key:
            idx[num] = key
    return idx


_VIS_RE = re.compile(r"show\s+if\s+([\d\.]+)\s*=\s*([^\n]+)", re.IGNORECASE)


def _parse_visibility(notes: Any, all_questions: list[dict]) -> dict | None:
    if not notes:
        return None
    m = _VIS_RE.search(str(notes))
    if not m:
        return None
    qnum = m.group(1).strip()
    values_str = m.group(2).strip().rstrip(".")
    # "Yes/Partially" → ["Yes", "Partially"]; "Yes" → ["Yes"]
    parts = [p.strip() for p in re.split(r"[\/,]", values_str) if p.strip()]
    qindex = _question_index(all_questions)
    field_key = qindex.get(qnum)
    if not field_key:
        return None
    return {
        "all": [
            {
                "fieldKey": field_key,
                "op": "in",
                "values": parts,
            }
        ]
    }


# ─────────────────────────── Level 2 conversion ─────────────────────────

def convert_level2() -> dict:
    wb = load_workbook(L2_XLSX, data_only=True)
    rows = _read_sheet_rows(wb, "RICS Level 2 Questions")

    # Group by Category → one step per category
    grouped: dict[str, list[dict]] = {}
    cat_order: list[str] = []
    for r in rows:
        cat = r.get("Category") or "Uncategorised"
        if cat not in grouped:
            grouped[cat] = []
            cat_order.append(cat)
        grouped[cat].append(r)

    steps_payload = []
    step_order = 1
    for cat in cat_order:
        fields_payload = []
        field_order = 1
        for r in grouped[cat]:
            qid = r.get("QID")
            label = r.get("Audit Question") or ""
            field_key = f"q{qid}" if qid is not None else _slug(label)[:60]

            # Question = RADIO with the workbook's response option set
            response_field = {
                "fieldKey": field_key,
                "label": label,
                "placeholder": None,
                "fieldType": "RADIO",
                "required": True,
                "fieldOrder": field_order,
                "optionSourceKey": "L2_Response",
                "ricsClause": r.get("RICS Clause"),
                "module": r.get("Level 2 Module"),
                "category": cat,
                "applicabilityTrigger": r.get("Applicability / Trigger"),
                "evidenceDepth": r.get("Evidence Depth"),
                "priority": r.get("Question Priority"),
                "expectedEvidence": r.get("Expected Evidence / Assets"),
                "rationale": r.get("Rationale / Notes"),
                "routingTags": _l2_routing_tags(r),
            }
            fields_payload.append(response_field)
            field_order += 1

            # Comment field
            comment = {
                "fieldKey": field_key + "_comment",
                "label": "Your response and supporting notes",
                "placeholder": "Explain how this is implemented in your organization.",
                "fieldType": "TEXTAREA",
                "required": False,
                "fieldOrder": field_order,
                "module": r.get("Level 2 Module"),
                "category": cat,
                "linkedToFieldKey": field_key,
            }
            fields_payload.append(comment)
            field_order += 1

            # Evidence file field
            evidence = {
                "fieldKey": field_key + "_evidence",
                "label": "Evidence files",
                "placeholder": (r.get("Expected Evidence / Assets") or "Upload supporting documents."),
                "fieldType": "FILE",
                "required": False,
                "multipleFiles": True,
                "fieldOrder": field_order,
                "module": r.get("Level 2 Module"),
                "category": cat,
                "linkedToFieldKey": field_key,
            }
            fields_payload.append(evidence)
            field_order += 1

        steps_payload.append(
            {
                "category": cat,
                "stepOrder": step_order,
                "title": cat,
                "description": f"Evidence-based RICS questions for category: {cat}",
                "fields": fields_payload,
            }
        )
        step_order += 1

    # L2 response options (single shared list)
    response_opts = [
        "Yes — evidenced",
        "Yes — not yet evidenced",
        "Partially",
        "No",
        "Not applicable",
        "Not sure",
    ]

    return {
        "version": 1,
        "auditType": "RICS_RESPONSIBLE_AI",
        "level": "LEVEL_2",
        "title": "RICS Responsible AI – Level 2 Audit",
        "description": "Evidence-based RICS audit aligned to the Sept 2025 RICS Professional "
        "Standard. 55 questions across 11 categories with auditor scoring (0–5).",
        "steps": steps_payload,
        "responseOptions": [{"value": v, "label": v} for v in response_opts],
    }


def _l2_routing_tags(r: dict) -> list[str]:
    tags = []
    for k, prefix in [
        ("Level 2 Module", "module"),
        ("RICS Clause", "clause"),
        ("Question Priority", "priority"),
        ("Evidence Depth", "depth"),
        ("Applicability / Trigger", "applies"),
    ]:
        v = r.get(k)
        if v:
            tags.append(f"{prefix}:{v}")
    return tags


# ────────────────────────────────── main ────────────────────────────────

def main() -> int:
    l1_template, l1_options = convert_level1()
    l2_template = convert_level2()

    (OUT_DIR / "level1_questionnaire.v1.json").write_text(
        json.dumps(l1_template, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    (OUT_DIR / "level1_option_lists.v1.json").write_text(
        json.dumps(l1_options, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    (OUT_DIR / "level2_rics_questions.v1.json").write_text(
        json.dumps(l2_template, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print("Wrote:")
    for name in (
        "level1_questionnaire.v1.json",
        "level1_option_lists.v1.json",
        "level2_rics_questions.v1.json",
    ):
        p = OUT_DIR / name
        print(f"  {p}  ({p.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
