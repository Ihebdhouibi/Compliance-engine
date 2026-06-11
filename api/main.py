from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.logging_config import setup_logging, get_logger
from api.middleware import CorrelationLoggingMiddleware
from api.routes import search, chat, ocr, routing, relevance
from api.services.ocr_queue import get_queue

setup_logging()
log = get_logger("fastapi.app")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Startup: warm the OCR engine + spawn worker tasks
    log.info("startup > spawning OCR workers")
    queue = get_queue()
    queue.start_workers()
    log.info("startup < API ready")
    yield
    # Shutdown: cancel workers cleanly
    log.info("shutdown > stopping OCR workers")
    await queue.stop_workers()
    log.info("shutdown < done")


app = FastAPI(
    title="RICS Compliance Engine API",
    description="Retrieval and AI-assisted audit API grounded on the RICS AI Professional Standard.",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(CorrelationLoggingMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["X-Correlation-Id"],
)

app.include_router(search.router)
app.include_router(chat.router)
app.include_router(ocr.router)
app.include_router(routing.router)
app.include_router(relevance.router)


@app.get("/health")
def health():
    return {"status": "ok"}

