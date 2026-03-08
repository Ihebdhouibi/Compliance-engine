# Local Setup & Launch Guide

## Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Python | 3.11+ | Runtime |
| Docker Desktop | 29.x+ | Qdrant vector database |
| Git | 2.x+ | Version control |

---

## 1. Clone & Setup

```bash
git clone https://github.com/Ihebdhouibi/Compliance-engine.git
cd Compliance-engine
git checkout knowledge-base
```

### Create virtual environment

```bash
python -m venv venv
```

### Activate virtual environment

**Windows (PowerShell):**
```powershell
.\venv\Scripts\Activate.ps1
```

**macOS / Linux:**
```bash
source venv/bin/activate
```

### Install dependencies

```bash
pip install -r requirements.txt
```

---

## 2. Environment Variables

Create a `.env` file in the project root:

```env
OPENAI_API_KEY=sk-your-openai-api-key-here
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=rics_standards
EMBEDDING_MODEL=BAAI/bge-small-en-v1.5
```

---

## 3. Start Qdrant (Docker)

```bash
docker run -d --name qdrant -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

Verify it's running:
- Dashboard: http://localhost:6333/dashboard
- Health: http://localhost:6333/healthz

---

## 4. Ingest the Knowledge Base

```bash
python scripts/ingest_to_qdrant.py
```

Expected output:
```
Loaded knowledge base from ...\knowledge_base\rics_knowledge_base.json
  Rules:       47
  Definitions: 8
  Context:     2
...
Upserted 57 points into 'rics_standards'
Done! Browse at http://localhost:6333/dashboard
```

> **Note:** On first run, the embedding model (`bge-small-en-v1.5`) will be downloaded (~67MB). Subsequent runs use the cached model.

---

## 5. Start the FastAPI Server

```bash
python -m uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at:
- Swagger docs: http://localhost:8000/docs
- Health check: http://localhost:8000/health

### Available Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check |
| `POST` | `/rules/search` | Semantic search with optional filters |
| `GET` | `/rules/{rule_id}` | Fetch a single rule by ID |
| `GET` | `/rules/` | List all sections with rule counts |
| `POST` | `/rules/evidence-check` | Retrieve evidence requirements for a compliance area |
| `POST` | `/chat/` | RAG-powered chat — ask questions about RICS compliance |

---

## 6. Start the Streamlit UI

Open a **new terminal**, activate the venv, then:

```bash
streamlit run ui/app.py --server.port 8501
```

The chat assistant will open at: http://localhost:8501

---

## Quick Start Summary

Open **three terminals** and run:

```
Terminal 1 — Qdrant:
  docker start qdrant

Terminal 2 — FastAPI:
  .\venv\Scripts\Activate.ps1
  python -m uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload

Terminal 3 — Streamlit:
  .\venv\Scripts\Activate.ps1
  streamlit run ui/app.py --server.port 8501
```

---

## Project Structure

```
Compliance-engine/
├── .env                          # API keys (gitignored)
├── .gitignore
├── requirements.txt
│
├── knowledge_base/
│   └── rics_knowledge_base.json  # 47 rules + 8 definitions + 2 context entries
│
├── scripts/
│   ├── ingest_to_qdrant.py       # Embeds & upserts KB into Qdrant
│   └── search_example.py         # Example search queries
│
├── api/
│   ├── __init__.py
│   ├── config.py                 # Env vars & constants
│   ├── main.py                   # FastAPI app entry point
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py            # Pydantic request/response models
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── search.py             # Search, rules, evidence endpoints
│   │   └── chat.py               # RAG chat endpoint
│   └── services/
│       ├── __init__.py
│       ├── qdrant_service.py     # Qdrant client wrapper
│       └── llm_service.py        # GPT integration (RAG)
│
├── ui/
│   └── app.py                    # Streamlit chat assistant
│
└── technical_documentation/
    └── qdrant_collection_schema.md
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `docker` not found | Refresh PATH: `$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")` |
| Qdrant connection refused | Ensure Docker Desktop is running and container is up: `docker start qdrant` |
| OpenAI API error 401 | Check `OPENAI_API_KEY` in `.env` is valid |
| Streamlit can't reach API | Make sure FastAPI is running on port 8000 first |
| Embedding model download slow | First run downloads ~67MB model. Cached at `%LOCALAPPDATA%\Temp\fastembed_cache\` |
