"""
Qdrant Ingestion Script for RICS Knowledge Base
-------------------------------------------------
Reads the structured JSON knowledge base, generates embeddings
via FastEmbed (BAAI/bge-small-en-v1.5, 384d), and upserts all
entries into a Qdrant collection.

Prerequisites:
  - Qdrant running on localhost:6333 (docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant)
  - pip install qdrant-client fastembed
"""

import json
import uuid
from pathlib import Path

from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    PointStruct,
    VectorParams,
    PayloadSchemaType,
)

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
QDRANT_URL = "http://localhost:6333"
COLLECTION_NAME = "rics_standards"
EMBEDDING_MODEL = "BAAI/bge-small-en-v1.5"
EMBEDDING_DIM = 384

KB_PATH = Path(__file__).resolve().parent.parent / "knowledge_base" / "rics_knowledge_base.json"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def build_embedding_text(entry: dict, entry_type: str) -> str:
    """Compose the text that will be embedded for a given entry."""
    if entry_type == "rule":
        parts = [
            f"Section {entry['section']} - {entry['section_title']}",
            entry["requirement_text"],
        ]
        if entry.get("trigger"):
            parts.append(f"Trigger: {entry['trigger']}")
        if entry.get("evidence_implied"):
            parts.append(f"Evidence: {entry['evidence_implied']}")
        return " | ".join(parts)

    if entry_type in ("document_definition", "glossary"):
        return f"{entry['term']}: {entry['text']}"

    if entry_type == "scope_context":
        return f"{entry['title']}: {entry['text']}"

    return entry.get("text", "")


def deterministic_uuid(name: str) -> str:
    """Create a deterministic UUID-5 from a name string (for idempotent upserts)."""
    return str(uuid.uuid5(uuid.NAMESPACE_URL, name))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    # Load knowledge base
    with open(KB_PATH, "r", encoding="utf-8") as f:
        kb = json.load(f)

    print(f"Loaded knowledge base from {KB_PATH}")
    print(f"  Rules:       {len(kb['rules'])}")
    print(f"  Definitions: {len(kb['definitions'])}")
    print(f"  Context:     {len(kb['context_entries'])}")

    # Connect to Qdrant
    client = QdrantClient(url=QDRANT_URL)
    print(f"\nConnected to Qdrant at {QDRANT_URL}")

    # Recreate collection (idempotent — safe for re-runs)
    if client.collection_exists(COLLECTION_NAME):
        client.delete_collection(COLLECTION_NAME)
        print(f"Deleted existing collection '{COLLECTION_NAME}'")

    client.create_collection(
        collection_name=COLLECTION_NAME,
        vectors_config=VectorParams(
            size=EMBEDDING_DIM,
            distance=Distance.COSINE,
        ),
    )
    print(f"Created collection '{COLLECTION_NAME}' (dim={EMBEDDING_DIM}, cosine)")

    # Create payload indexes for filterable fields
    for field, schema in [
        ("entry_type", PayloadSchemaType.KEYWORD),
        ("section", PayloadSchemaType.KEYWORD),
        ("requirement_type", PayloadSchemaType.KEYWORD),
        ("applies_to", PayloadSchemaType.KEYWORD),
        ("obligation_keyword", PayloadSchemaType.KEYWORD),
        ("rule_id", PayloadSchemaType.KEYWORD),
    ]:
        client.create_payload_index(
            collection_name=COLLECTION_NAME,
            field_name=field,
            field_schema=schema,
        )
    print("Created payload indexes")

    # Build entries list: (id_str, embedding_text, payload)
    entries = []

    for rule in kb["rules"]:
        payload = {
            "entry_type": "rule",
            "rule_id": rule["id"],
            "section": rule["section"],
            "section_title": rule["section_title"],
            "parent_section": rule["parent_section"],
            "parent_section_title": rule["parent_section_title"],
            "requirement_type": rule["requirement_type"],
            "obligation_keyword": rule["obligation_keyword"],
            "applies_to": rule["applies_to"],
            "requirement_text": rule["requirement_text"],
            "trigger": rule.get("trigger", ""),
            "evidence_implied": rule.get("evidence_implied", ""),
            "source_text_verbatim": rule.get("source_text_verbatim", ""),
            "related_rules": rule.get("related_rules", []),
            "related_definitions": rule.get("related_definitions", []),
            "topic_tags": rule.get("topic_tags", []),
        }
        if rule.get("parent_rule"):
            payload["parent_rule"] = rule["parent_rule"]

        entries.append((
            rule["id"],
            build_embedding_text(rule, "rule"),
            payload,
        ))

    for defn in kb["definitions"]:
        payload = {
            "entry_type": defn["type"],
            "rule_id": defn["id"],
            "term": defn["term"],
            "text": defn["text"],
            "topic_tags": defn.get("tags", []),
        }
        if defn.get("source"):
            payload["source"] = defn["source"]
        entries.append((
            defn["id"],
            build_embedding_text(defn, defn["type"]),
            payload,
        ))

    for ctx in kb["context_entries"]:
        payload = {
            "entry_type": ctx["type"],
            "rule_id": ctx["id"],
            "section": ctx["section"],
            "title": ctx["title"],
            "text": ctx["text"],
            "topic_tags": ctx.get("tags", []),
        }
        entries.append((
            ctx["id"],
            build_embedding_text(ctx, "scope_context"),
            payload,
        ))

    print(f"\nPrepared {len(entries)} entries for embedding")

    # Generate embeddings via FastEmbed
    texts = [e[1] for e in entries]

    print(f"Generating embeddings with {EMBEDDING_MODEL} ...")
    from fastembed import TextEmbedding

    embed_model = TextEmbedding(model_name=EMBEDDING_MODEL)
    embeddings = list(embed_model.embed(texts))
    print(f"Generated {len(embeddings)} embeddings (dim={len(embeddings[0])})")

    # Build points
    points = []
    for i, (id_str, _, payload) in enumerate(entries):
        points.append(
            PointStruct(
                id=deterministic_uuid(id_str),
                vector=embeddings[i].tolist(),
                payload=payload,
            )
        )

    # Upsert in one batch (57 points is small enough)
    client.upsert(collection_name=COLLECTION_NAME, points=points)
    print(f"\nUpserted {len(points)} points into '{COLLECTION_NAME}'")

    # Verify
    info = client.get_collection(COLLECTION_NAME)
    print(f"\nCollection info:")
    print(f"  Points count:  {info.points_count}")
    print(f"  Vector size:   {info.config.params.vectors.size}")
    print(f"  Distance:      {info.config.params.vectors.distance}")
    print(f"\nDone! Browse at {QDRANT_URL}/dashboard")


if __name__ == "__main__":
    main()
