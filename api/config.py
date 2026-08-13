import os

from dotenv import load_dotenv

load_dotenv()

QDRANT_URL: str = os.getenv("QDRANT_URL", "http://localhost:6333")
QDRANT_COLLECTION: str = os.getenv("QDRANT_COLLECTION", "rics_standards")
EMBEDDING_MODEL: str = os.getenv("EMBEDDING_MODEL", "BAAI/bge-small-en-v1.5")
EMBEDDING_DIM: int = 384

QWEN_BASE_URL: str = os.getenv("QWEN_BASE_URL", "http://localhost:11434/v1")
QWEN_MODEL: str = os.getenv("QWEN_MODEL", "qwen3:8b")
QWEN_API_KEY: str = "not-used"


QWEN_TIMEOUT: float = float(os.getenv("QWEN_TIMEOUT", "120.0"))
