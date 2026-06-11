"""Shared FastEmbed text-embedding model.

Loading the embedding model is expensive (weights + ONNX session), so every
feature that needs embeddings — the Qdrant knowledge-base search and the
evidence relevance scorer — shares this single lazily-initialised instance.
"""
from functools import lru_cache

from fastembed import TextEmbedding

from api.config import EMBEDDING_MODEL


@lru_cache(maxsize=1)
def get_embedder() -> TextEmbedding:
    """Return the process-wide embedding model, loading it on first use."""
    return TextEmbedding(model_name=EMBEDDING_MODEL)
