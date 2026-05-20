"""Routing explainer.

Reads the *same* routing rules JSON as the Java RoutingEngine so both
runtimes stay in lock-step. The file lives at::

    src/main/resources/routing/level1_routing_rules.v1.json

This module is intended for read-only explanation use cases (e.g. the
chat assistant answering "why did I get pathway 2?"). For authoritative
routing decisions, always trust the Java engine — this is a mirror.
"""
from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, List, Optional

# Repo root is two levels up from this file: api/services/<this> → repo root
_REPO_ROOT = Path(__file__).resolve().parents[2]
_RULES_PATH = (
    _REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "routing"
    / "level1_routing_rules.v1.json"
)


@lru_cache(maxsize=1)
def _rules() -> Dict[str, Any]:
    with _RULES_PATH.open("r", encoding="utf-8") as fp:
        return json.load(fp)


# ── public API ────────────────────────────────────────────────────────


def list_pathways() -> List[Dict[str, Any]]:
    """All pathway summaries (id, name, package, modules, indicative price)."""
    out: List[Dict[str, Any]] = []
    for p in _rules().get("pathways", []):
        out.append(
            {
                "id": p.get("id"),
                "name": p.get("name"),
                "recommendedPackage": p.get("recommendedPackage"),
                "activeModules": p.get("activeModules", []),
                "evidenceDepth": p.get("evidenceDepth"),
                "indicativePrice": p.get("indicativePrice"),
            }
        )
    return out


def explain_pathway(pathway_id: str) -> Optional[Dict[str, Any]]:
    """Human-readable explanation for a single pathway."""
    for p in _rules().get("pathways", []):
        if p.get("id") == pathway_id:
            return {
                "id": p.get("id"),
                "name": p.get("name"),
                "rationale": p.get("rationale"),
                "explanation": p.get("explanation"),
                "recommendedPackage": p.get("recommendedPackage"),
                "activeModules": p.get("activeModules", []),
                "evidenceDepth": p.get("evidenceDepth"),
                "indicativePrice": p.get("indicativePrice"),
            }
    return None


def compute(answers: Dict[str, Any]) -> Dict[str, Any]:
    """Mirror of the Java engine.

    Picks the first pathway whose ``match`` rule evaluates to true, then
    applies size adjustments. Returns ``None`` when nothing matches.
    """
    rules = _rules()
    for p in rules.get("pathways", []):
        if _matches(p.get("match", {}), answers):
            return _apply_size(p, answers, rules)
    return {}


# ── matcher (mirrors RoutingEngine.evalCondition) ─────────────────────


def _matches(rule: Dict[str, Any], answers: Dict[str, Any]) -> bool:
    if not rule:
        return True
    if "all" in rule:
        return all(_eval_cond(c, answers) for c in rule["all"])
    if "any" in rule:
        return any(_eval_cond(c, answers) for c in rule["any"])
    return _eval_cond(rule, answers)


def _eval_cond(cond: Dict[str, Any], answers: Dict[str, Any]) -> bool:
    field = cond.get("field")
    val = answers.get(field)
    if "equals" in cond:
        return str(val) == str(cond["equals"])
    if "in" in cond:
        return str(val) in {str(x) for x in cond["in"]}
    if "present" in cond:
        present = val is not None and val != ""
        return present == bool(cond["present"])
    return False


def _apply_size(pathway: Dict[str, Any], answers: Dict[str, Any], rules: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(pathway)
    size = answers.get("selector_firm_size")
    adjustments = pathway.get("sizeAdjustments") or {}
    if size and size in adjustments:
        price = dict(pathway.get("indicativePrice") or {})
        adj = adjustments[size]
        if "minMultiplier" in adj and "min" in price:
            price["min"] = int(price["min"] * adj["minMultiplier"])
        if "maxMultiplier" in adj and "max" in price:
            price["max"] = int(price["max"] * adj["maxMultiplier"])
        out["indicativePrice"] = price
    return out
