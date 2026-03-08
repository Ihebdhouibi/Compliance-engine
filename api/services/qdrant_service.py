from fastembed import TextEmbedding
from qdrant_client import QdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue

from api.config import QDRANT_URL, QDRANT_COLLECTION, EMBEDDING_MODEL


class QdrantService:
    """Encapsulates all Qdrant operations for the RICS knowledge base."""

    def __init__(self) -> None:
        self._client = QdrantClient(url=QDRANT_URL)
        self._embed = TextEmbedding(model_name=EMBEDDING_MODEL)

    # ── embedding helper ─────────────────────────────────────────────────

    def _embed_query(self, text: str) -> list[float]:
        return list(self._embed.embed([text]))[0].tolist()

    # ── filter builder ───────────────────────────────────────────────────

    @staticmethod
    def _build_filter(
        section: str | None = None,
        applies_to: str | None = None,
        entry_type: str | None = None,
    ) -> Filter | None:
        conditions = []
        if section:
            conditions.append(FieldCondition(key="section", match=MatchValue(value=section)))
        if applies_to:
            conditions.append(FieldCondition(key="applies_to", match=MatchValue(value=applies_to)))
        if entry_type:
            conditions.append(FieldCondition(key="entry_type", match=MatchValue(value=entry_type)))
        return Filter(must=conditions) if conditions else None

    # ── public API ───────────────────────────────────────────────────────

    def search(
        self,
        query: str,
        limit: int = 5,
        section: str | None = None,
        applies_to: str | None = None,
        entry_type: str | None = None,
    ) -> list[dict]:
        vector = self._embed_query(query)
        hits = self._client.search(
            collection_name=QDRANT_COLLECTION,
            query_vector=vector,
            query_filter=self._build_filter(section, applies_to, entry_type),
            limit=limit,
        )
        return [
            {"rule_id": h.payload.get("rule_id", ""), "score": h.score, "entry_type": h.payload.get("entry_type", ""), "payload": h.payload}
            for h in hits
        ]

    def get_rule(self, rule_id: str) -> dict | None:
        results = self._client.scroll(
            collection_name=QDRANT_COLLECTION,
            scroll_filter=Filter(must=[FieldCondition(key="rule_id", match=MatchValue(value=rule_id))]),
            limit=1,
            with_payload=True,
            with_vectors=False,
        )
        points = results[0]
        if not points:
            return None
        return points[0].payload

    def get_related(self, rule_id: str) -> list[dict]:
        rule = self.get_rule(rule_id)
        if not rule:
            return []

        related_ids: list[str] = []
        related_ids.extend(rule.get("related_rules", []))
        related_ids.extend(rule.get("related_definitions", []))
        if rule.get("parent_rule"):
            related_ids.append(rule["parent_rule"])

        related = []
        for rid in dict.fromkeys(related_ids):  # deduplicate, preserve order
            entry = self.get_rule(rid)
            if entry:
                related.append(entry)
        return related

    def list_sections(self) -> list[dict]:
        all_points, _ = self._client.scroll(
            collection_name=QDRANT_COLLECTION,
            scroll_filter=Filter(must=[FieldCondition(key="entry_type", match=MatchValue(value="rule"))]),
            limit=100,
            with_payload=True,
            with_vectors=False,
        )
        section_map: dict[str, dict] = {}
        for pt in all_points:
            sec = pt.payload.get("section", "")
            if sec not in section_map:
                section_map[sec] = {"section": sec, "section_title": pt.payload.get("section_title", ""), "rule_count": 0}
            section_map[sec]["rule_count"] += 1

        return sorted(section_map.values(), key=lambda s: s["section"])
