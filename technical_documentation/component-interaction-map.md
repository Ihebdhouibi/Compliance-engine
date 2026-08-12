# Component Interaction Map

How the Compliance Engine's components currently interact and exchange
information. This is the reference for the component-based logging system:
every **call boundary** marked below is a log seam.

> Status: reflects the codebase as of the `semantic-relevance-highlighting`
> branch (RAG chat, OCR pipeline, grounded suggestions, clause picker, and
> semantic evidence relevance all wired).

---

## 1. Deployment / component graph

```
┌──────────────────────── BROWSER (Angular SPA, :4200 dev) ────────────────────────┐
│  audit-workspace.component   audit-bot.component   chat.component                 │
│  rics-search.service         chat.service          notification.service          │
│  token.service (JWT in storage)                                                   │
└───┬──────────────────────────────┬───────────────────────────────────┬───────────┘
    │ REST + JWT (Bearer)          │ REST (no auth, CORS)                │ WebSocket/STOMP
    │ http://localhost:8080/api/v1 │ http://localhost:8000               │ /ws
    ▼                              ▼                                     ▼
┌───────────────────────────┐  ┌──────────────────────────────────┐   (push: audit alerts)
│ SPRING BOOT  :8080        │  │ FASTAPI AI ENGINE  :8000         │        ▲
│ (AiAuditServer, Java 17)  │  │ routers: /chat /rules /routing   │        │
│  • Auth / JWT             │  │          /ocr /relevance /health │        │
│  • Audit CRUD + results   │  │  ┌────────────┐ ┌──────────────┐ │        │
│  • OcrOrchestratorService ┼─▶│  │ llm_service│ │ qdrant_service│ │        │
│    POST /ocr/upload       │  │  └─────┬──────┘ └──────┬───────┘ │        │
│  • Notifications (STOMP) ──┼──┼────────┼───────────────┼─────────┼────────┘
│  • File storage WebContent│  │  ┌─────▼─────┐   ┌──────▼──────┐  │
└───┬──────────────┬────────┘  │                │   │ embedder    │  │
    │ JPA/JDBC     │ FS I/O     │  │ (external)│   │ (FastEmbed) │  │
    ▼              ▼            │  └───────────┘   └──────┬──────┘  │
┌────────┐  ┌──────────────┐   │  ┌────────────┐         │         │
│ DB     │  │ WebContent/  │   │  │ RapidOCR   │  relevance_service│
│ H2/PG  │  │ *.pdf *.png  │   │  │ (ONNX)     │  (sentence scoring)│
└────────┘  └──────────────┘   │  └────────────┘         ▼         │
                               │                  ┌──────────────┐ │
                               │                  │ QDRANT :6333 │◀┘
                               │                  │ rics_standards│
                               └──────────────────┴──────────────┘
```

### Processes / runtimes
| Component | Runtime | Port | Responsibility |
|-----------|---------|------|----------------|
| Angular SPA | Browser | 4200 (dev) | UI: audit workspace, AI assistant, chat, notifications |
| Spring Boot (`AiAuditServer`) | JVM 17 | 8080 | Auth, audit CRUD, results, OCR orchestration, notifications, file storage |
| FastAPI AI engine | Python 3.11 | 8000 | RAG chat, rule search, evidence-check, routing, OCR, relevance |
| Qdrant | Docker | 6333 | Vector store `rics_standards` (57 points, 384-dim) |
|  | External | — | `gpt-4o-mini` chat completion |
| FastEmbed | in FastAPI | — | `BAAI/bge-small-en-v1.5` embeddings (shared) |
| RapidOCR | in FastAPI | — | ONNX OCR text extraction |
| Database | — | — | H2 (dev) / PostgreSQL (prod) |
| Filesystem | — | — | `WebContent/` uploaded evidence |

---

## 2. Call-boundary inventory (log seams)

| # | Caller → Callee | Transport | Endpoint / channel | Payload in → out | Sync? |
|---|---|---|---|---|---|
| S1 | Angular → Spring | REST+JWT | `GET /api/v1/audits/{id}` (admin/auditor) | id → request+answers | sync |
| S2 | Angular → Spring | REST+JWT | form template / process steps | auditType → steps | sync |
| S3 | Angular → Spring | REST+JWT | `GET/POST/PUT` step results | stepResult ↔ saved | sync |
| S4 | Angular → Spring | REST+JWT | `POST` upload answer file (multipart) | file → media | sync → triggers O1 |
| S5 | Angular → Spring | REST+JWT | `GET /audits/{id}/ocr`, `/ocr/media/{mid}`, `POST .../retry` | id → EvidenceOcrResult[] | sync (4s poll) |
| S6 | Angular → Spring | REST+JWT | `GET` evidence blob | media.url → file bytes | sync |
| W1 | Spring → Angular | STOMP | `/ws` topic | notification push | async |
| O1 | Spring → FastAPI | REST | `POST /ocr/upload` (multipart) | file → `{filename,text,page_count,elapsed_ms}` | sync (`@Async` thread) |
| A1 | Angular → FastAPI | REST | `POST /chat/` | `{message,limit,section?,context}` → `{answer,sources}` | sync |
| A2 | Angular → FastAPI | REST | `POST /rules/search` | `{query,limit,...}` → `{results}` | sync |
| A3 | Angular → FastAPI | REST | `POST /rules/evidence-check` | `{query,limit}` → `{evidence}` | sync |
| A4 | Angular → FastAPI | REST | `POST /relevance/highlight` | `{text,query}` → `{segments,truncated}` | sync |
| E1 | FastAPI → Qdrant | REST | vector `search` | embedding → hits | sync |
| E2 | FastAPI →  | HTTPS | chat completion `gpt-4o-mini` | messages → answer | sync |
| E3 | FastAPI in-proc | call | FastEmbed `embed()` | text → vector (used by E1, A4) | sync |
| E4 | FastAPI in-proc | call | RapidOCR `extract()` | file → text (used by O1) | sync |
| D1 | Spring → DB | JPA | persist/read | entities | sync |
| F1 | Spring → FS | I/O | `WebContent/...` | file bytes | sync |

---

## 3. Sequence — OCR pipeline (S4 → O1 → S5)

```
Angular        Spring(:8080)            FastAPI(:8000)        RapidOCR     DB
  │  S4 upload    │                           │                 │          │
  ├──────────────▶│ store file (F1)           │                 │          │
  │               ├─ EvidenceOcrResult=PENDING ──────────────────────────▶ │ (D1)
  │               │  @Async O1 POST /ocr/upload│                 │          │
  │               ├───────────────────────────▶│ extract (E4)    │          │
  │               │                            ├────────────────▶│          │
  │               │   {text,page_count,ms}     │◀────────────────┤          │
  │               │◀───────────────────────────┤                 │          │
  │               ├─ EvidenceOcrResult=DONE,rawText ─────────────────────▶  │ (D1)
  │  S5 poll /ocr (every 4s until DONE/FAILED) │                 │          │
  ├──────────────▶│ read (D1) → results        │                 │          │
  │◀──────────────┤ badge → "Extracted text·Np"│                 │          │
```

## 4. Sequence — RAG assistant & semantic relevance (A1 / A4)

```
audit-bot ─ A1 POST /chat/ {message, context=buildAiContext()} ─▶ FastAPI
                                                                   ├─ E3 embed(message)
                                                                   ├─ E1 Qdrant search ─▶ rules
                                                                   ├─ E2 chat model (gpt-4o-mini, rules+context)
                                            {answer, sources} ◀────┤
audit-workspace (drawer, Extracted-text tab):
  A4 POST /relevance/highlight {text=rawText, query=drawerQuery(=fieldLabel)} ─▶ FastAPI
                                                                   ├─ split text → sentences
                                                                   ├─ E3 embed(query + each sentence)
                                                                   ├─ cosine + bucket level 0/1/2
                          {segments:[{start,end,score,level}], truncated} ◀──── (offsets → <mark>)
```

---

## 5. Correlation keys that cross boundaries

IDs a logging system threads to stitch a request across components:

- **`auditRequestId`** — browser → Spring → (OCR rows, results, notifications)
- **`mediaId`** — evidence file → OCR result → drawer / relevance
- **`stepId` / `fieldId`** — question identity (drives suggestions, verdicts, relevance `query`)
- **Missing today: a cross-service trace/correlation id.** Spring→FastAPI (O1) and
  browser→FastAPI (A1–A4) carry no shared request id, so an OCR or chat call
  cannot currently be tied back to the originating audit action in logs.

---

## 6. Implications for the logging system

- **Component IDs (log channels):** `ng.audit-workspace`, `ng.audit-bot`,
  `ng.rics-search`, `spring.audit`, `spring.ocr-orchestrator`, `spring.ws-notify`,
  `fastapi.chat`, `fastapi.rules`, `fastapi.relevance`, `fastapi.ocr`,
  `svc.qdrant`, `svc.`, `svc.embedder`, `svc.rapidocr`.
- **Propagate one `X-Correlation-Id`** browser → Spring → FastAPI (and stamp it on
  the `@Async` OCR thread) so seams O1 / A1–A4 / E1–E4 share it.
- **Log each seam** with: `{correlationId, component, op, auditId?, mediaId?, ms,
  status, sizes}` — never payload bodies (OCR text and chat content are sensitive).
