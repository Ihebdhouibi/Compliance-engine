"""Routing explainer endpoints.

These are read-only mirrors of the Java RoutingEngine, used by the chat
assistant and any UI that needs to summarise the pathways.
"""
from typing import Any, Dict

from fastapi import APIRouter, HTTPException

from api.services import routing_explainer

router = APIRouter(prefix="/routing", tags=["Routing"])


@router.get("/pathways")
def list_pathways():
    return routing_explainer.list_pathways()


@router.get("/pathways/{pathway_id}")
def get_pathway(pathway_id: str):
    data = routing_explainer.explain_pathway(pathway_id)
    if data is None:
        raise HTTPException(status_code=404, detail="Pathway not found")
    return data


@router.post("/compute")
def compute(answers: Dict[str, Any]):
    """Cross-validate routing against the Java engine."""
    return routing_explainer.compute(answers)
