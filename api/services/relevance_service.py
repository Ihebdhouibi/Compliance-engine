"""Semantic relevance scoring for evidence text.

Given a block of OCR-extracted evidence text and an audit question, split the
text into sentences and score how semantically close each sentence is to the
question (cosine similarity over shared FastEmbed embeddings). The auditor's
UI shades the most relevant sentences so the compliance proof is easy to find.

Embeddings are local (no external API cost). Character offsets into the *original*
text are returned so the frontend can highlight without re-joining/reformatting.
"""
from __future__ import annotations

import re

import numpy as np

from api.logging_config import get_logger, timed
from api.services.embedder import get_embedder

_log = get_logger("svc.embedder")

# A "sentence" = a run of text up to a terminator (.!?) or a line break.
_SENTENCE_RE = re.compile(r"[^.!?\n\r]+[.!?]*", re.UNICODE)

# Keep latency bounded on large documents.
_MAX_SEGMENTS = 160
# Minimum length for a standalone segment; shorter fragments merge into the
# previous one so headings/labels don't fragment the scoring.
_MIN_SEGMENT_LEN = 18

# Relevance thresholds (cosine). Tuned for BAAI/bge-small-en-v1.5, whose scores
# sit in a compressed band, so levels are relative to the document's best match.
_DOC_FLOOR = 0.30      # below this, nothing in the doc is treated as relevant
_STRONG_ABS = 0.42
_SOFT_ABS = 0.34
_STRONG_REL = 0.88     # >= 88% of the best score in the doc → strong
_SOFT_REL = 0.72       # >= 72% of the best score in the doc → soft


def _split_with_spans(text: str) -> list[tuple[int, int, str]]:
    """Split text into (start, end, sentence) spans over the ORIGINAL string.

    Tiny fragments (labels, single words) are merged into the previous span so
    they share its offsets, keeping the highlight aligned to the source text.
    """
    spans: list[list] = []  # mutable [start, end, text]
    for m in _SENTENCE_RE.finditer(text):
        seg = m.group().strip()
        if not seg:
            continue
        if spans and len(seg) < _MIN_SEGMENT_LEN:
            spans[-1][1] = m.end()  # extend previous span to swallow the fragment
        else:
            spans.append([m.start(), m.end(), seg])
        if len(spans) >= _MAX_SEGMENTS:
            break
    return [(s, e, t) for s, e, t in spans]


def _assign_level(score: float, best: float) -> int:
    """Bucket a similarity score into 0 (none), 1 (soft), 2 (strong)."""
    if best < _DOC_FLOOR:
        return 0
    if score >= max(_STRONG_ABS, _STRONG_REL * best):
        return 2
    if score >= max(_SOFT_ABS, _SOFT_REL * best):
        return 1
    return 0


def score_relevance(text: str, query: str) -> tuple[list[dict], bool]:
    """Return (segments, truncated).

    Each segment: {start, end, score, level}. ``truncated`` is True when the
    document exceeded the segment cap and was scored only up to that point.
    """
    if not text or not query.strip():
        return [], False

    spans = _split_with_spans(text)
    if not spans:
        return [], False

    truncated = len(spans) >= _MAX_SEGMENTS

    embedder = get_embedder()
    seg_texts = [t for _, _, t in spans]
    with timed(_log, "embed", texts=len(seg_texts) + 1):  # +1 for the query
        query_vec = np.asarray(list(embedder.embed([query]))[0], dtype=np.float32)
        query_vec /= np.linalg.norm(query_vec) + 1e-9
        seg_vecs = list(embedder.embed(seg_texts))

    scores: list[float] = []
    for vec in seg_vecs:
        v = np.asarray(vec, dtype=np.float32)
        v /= np.linalg.norm(v) + 1e-9
        scores.append(float(np.dot(query_vec, v)))

    best = max(scores) if scores else 0.0
    segments = [
        {"start": start, "end": end, "score": round(score, 4), "level": _assign_level(score, best)}
        for (start, end, _), score in zip(spans, scores)
    ]
    return segments, truncated
