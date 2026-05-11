"""
Example searches against the RICS knowledge base in Qdrant.
Run: python scripts/search_example.py
"""

from qdrant_client import QdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue
from fastembed import TextEmbedding

client = QdrantClient(url="http://localhost:6333")
model = TextEmbedding(model_name="BAAI/bge-small-en-v1.5")

SEP = "=" * 80


def search(query: str, limit: int = 5, filt: Filter | None = None):
    vector = list(model.embed([query]))[0].tolist()
    return client.search(
        collection_name="rics_standards",
        query_vector=vector,
        query_filter=filt,
        limit=limit,
    )


def print_results(results, label=""):
    if label:
        print(f"\n{label}")
    print(SEP)
    for i, hit in enumerate(results, 1):
        p = hit.payload
        rid = p.get("rule_id", "?")
        text = p.get("requirement_text") or f"{p.get('term', '')}: {p.get('text', '')}"
        text = (text[:140] + "...") if len(text) > 140 else text
        print(f"  #{i}  [{rid}]  score={hit.score:.4f}")
        print(f"      {text}")
        print(f"      tags={p.get('topic_tags', [])}")
    print()


# ── Example 1: Plain semantic search ──────────────────────────────────────────
query1 = "What are the requirements for data privacy and confidentiality?"
print_results(search(query1), f'SEARCH 1 — "{query1}"')

# ── Example 2: Filtered by section (only Section 4) ──────────────────────────
query2 = "How should output reliability be assessed?"
section_filter = Filter(
    must=[FieldCondition(key="section", match=MatchValue(value="4.2"))]
)
print_results(
    search(query2, filt=section_filter),
    f'SEARCH 2 — "{query2}" (filtered: section=4.2)',
)

# ── Example 3: Filtered by entry type (only glossary/definitions) ─────────────
query3 = "What is an AI system?"
type_filter = Filter(
    must=[FieldCondition(key="entry_type", match=MatchValue(value="glossary"))]
)
print_results(
    search(query3, filt=type_filter),
    f'SEARCH 3 — "{query3}" (filtered: glossary only)',
)

# ── Example 4: Filtered by applies_to (only rules for firms) ─────────────────
query4 = "risk register requirements"
firm_filter = Filter(
    must=[FieldCondition(key="applies_to", match=MatchValue(value="regulated_firms"))]
)
print_results(
    search(query4, filt=firm_filter),
    f'SEARCH 4 — "{query4}" (filtered: regulated_firms only)',
)

# ── Example 5: Combined filter — procurement & due diligence in Section 4.1 ──
query5 = "supplier due diligence before buying an AI tool"
combined = Filter(
    must=[
        FieldCondition(key="section", match=MatchValue(value="4.1")),
        FieldCondition(key="entry_type", match=MatchValue(value="rule")),
    ]
)
print_results(
    search(query5, filt=combined),
    f'SEARCH 5 — "{query5}" (filtered: section=4.1, rules only)',
)
