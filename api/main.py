from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes import search, chat

app = FastAPI(
    title="RICS Compliance Engine API",
    description="Retrieval and AI-assisted audit API grounded on the RICS AI Professional Standard.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(search.router)
app.include_router(chat.router)


@app.get("/health")
def health():
    return {"status": "ok"}
