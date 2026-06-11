from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes import search, chat, ocr, routing, relevance
from api.services.ocr_queue import get_queue


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Startup: warm the OCR engine + spawn worker tasks
    queue = get_queue()
    queue.start_workers()
    yield
    # Shutdown: cancel workers cleanly
    await queue.stop_workers()


app = FastAPI(
    title="RICS Compliance Engine API",
    description="Retrieval and AI-assisted audit API grounded on the RICS AI Professional Standard.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(search.router)
app.include_router(chat.router)
app.include_router(ocr.router)
app.include_router(routing.router)
app.include_router(relevance.router)


@app.get("/health")
def health():
    return {"status": "ok"}

