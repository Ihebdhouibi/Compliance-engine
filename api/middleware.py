"""Correlation + request-logging ASGI middleware.

Implemented as pure ASGI (not Starlette's BaseHTTPMiddleware) so the
correlation-id contextvar set here is visible to the route handler and every
service it calls — BaseHTTPMiddleware runs the app in a separate task and would
lose the contextvar.

Reads an inbound ``X-Correlation-Id`` (e.g. forwarded by the Spring backend or
the Angular client) or mints a new one, logs ``-> METHOD path`` /
``<- STATUS METHOD path (Nms)``, and echoes the id back on the response.
"""
from __future__ import annotations

import time

from api.logging_config import get_logger, new_correlation_id, set_correlation_id

log = get_logger("fastapi.http")
_HEADER = b"x-correlation-id"


class CorrelationLoggingMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") != "http":
            await self.app(scope, receive, send)
            return

        headers = dict(scope.get("headers") or [])
        raw = headers.get(_HEADER)
        cid = raw.decode() if raw else new_correlation_id()
        set_correlation_id(cid)

        method = scope.get("method", "?")
        path = scope.get("path", "?")

        # The browser ships logs here constantly; don't log the log-ingest call.
        if path == "/logs":
            await self.app(scope, receive, send)
            return

        t0 = time.perf_counter()
        log.info(f"-> {method} {path}")

        status = {"code": 0}

        async def send_wrapper(message):
            if message["type"] == "http.response.start":
                status["code"] = message["status"]
                message.setdefault("headers", []).append((_HEADER, cid.encode()))
            await send(message)

        try:
            await self.app(scope, receive, send_wrapper)
        except Exception as exc:  # noqa: BLE001 - logged then re-raised
            ms = int((time.perf_counter() - t0) * 1000)
            log.error(f"<- 500 {method} {path} ({ms}ms) {type(exc).__name__}: {exc}")
            raise

        ms = int((time.perf_counter() - t0) * 1000)
        log.info(f"<- {status['code']} {method} {path} ({ms}ms)")
