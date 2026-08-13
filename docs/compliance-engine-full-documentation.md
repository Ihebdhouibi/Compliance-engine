# Compliance Engine – AI Audit System

## 1. Project Overview

**Compliance Engine** is an AI‑powered audit platform designed to help firms assess compliance with the **RICS AI Professional Standard**. It provides:

- AI‑assisted chat (RAG) based on RICS rules
- Semantic search over the RICS knowledge base
- Audit request submission and management
- Real‑time notifications for administrators
- Role‑based access (Admin, Auditor, Company)

The system consists of three main branches:

| Branch | Technology | Purpose |
|--------|------------|---------|
| `knowledge-base` | Python FastAPI + Qdrant | AI engine, chat, RICS search, vector storage |
| `AiAuditBackend` | Spring Boot (Java) | User authentication, audit request management, notifications |
| `AiAuditFront` | Angular | Web frontend for all users |
| `ui-optimization` | (merged frontend + backend) | Integration branch with latest UI and notification system |

---

## 2. Architecture
┌─────────────────────────────────────────────────────────────┐
│ Angular Frontend │
│ http://localhost:4200 │
└─────────────┬───────────────────────────────┬───────────────┘
│ │
│ (HTTP REST) │ (WebSocket STOMP)
▼ ▼
┌─────────────────────────┐ ┌─────────────────────────┐
│ Spring Boot Backend │ │ FastAPI AI Engine │
│ http://localhost:8080 │ │ http://localhost:8000 │
│ - Authentication │ │ - Chat (RAG) │
│ - Audit requests │ │ - RICS search │
│ - Notifications (WS) │ │ - Embeddings │
└─────────────┬───────────┘ └───────────┬─────────────┘
│ │
▼ ▼
┌─────────────────────────┐ ┌─────────────────────────┐
│ H2 Database │ │ Qdrant │
│ (in‑memory for dev) │ │ Vector Database │
│ │ │ port 6333 │
└─────────────────────────┘ └─────────────────────────┘

---

## 3. Technology Stack

| Component | Technologies |
|-----------|--------------|
| **Frontend** | Angular 19, TypeScript, SCSS, RxJS, Angular Signals, STOMP WebSocket, SockJS |
| **Backend (main)** | Spring Boot 2.7.18, Java 17, Spring Security, JPA/Hibernate, H2, WebSocket |
| **AI Engine** | FastAPI, Python 3.12, Qdrant, FastEmbed, Ollama (Qwen), PaddleOCR (optional) |
| **Database** | H2 (dev), PostgreSQL (production ready) |
| **Vector DB** | Qdrant (local or cloud) |
| **Build Tools** | Maven (Spring Boot), npm (Angular), pip (Python) |

---

## 4. Branch Structure and Purpose

| Branch | Description |
|--------|-------------|
| `knowledge-base` | FastAPI + Qdrant + RICS knowledge base. Provides `/chat` and `/rules/search` endpoints. |
| `AiAuditBackend` | Spring Boot with authentication, audit CRUD, user roles. |
| `AiAuditFront` | Angular application (dashboard, audit forms, profile). |
| `develop` | Integration branch for combining backend and frontend. |
| `ui-optimization` | **Current working branch** – contains merged frontend + backend + real‑time notification system. |

---

## 5. Real‑Time Notification System (Added in `ui-optimization`)

### 5.1 Overview
Admins receive instant notifications when a client submits a new audit request. Uses **WebSocket (STOMP)** with a fallback polling mechanism.

### 5.2 Backend Implementation

#### Key Java Files
- `models/Notification.java` – JPA entity
- `repositories/NotificationRepository.java` – data access
- `config/WebSocketConfig.java` – STOMP endpoint `/ws`
- `services/NotificationService.java` – saves notification and broadcasts via `SimpMessagingTemplate`
- `controllers/admin/NotificationController.java` – REST endpoints:
  - `GET /api/v1/admin/notifications` – list notifications (submitted audits)
  - `GET /api/v1/admin/notifications/unread-count` – count unread
- Modified `AuditRequestService.submitRequest()` – calls `notificationService.createAndSend(...)`
- `UserService.findFirstAdmin()` – helper to find an admin recipient

#### WebSocket Configuration
- Endpoint: `/ws`
- Allowed origins: `*` (for development)
- Simple broker: `/topic`, `/queue`
- User destination prefix: `/user`

### 5.3 Frontend Implementation

#### Angular Service (`notification.service.ts`)
- Uses `@stomp/stompjs` and `sockjs-client`
- Connects to `/user/{adminId}/queue/notifications`
- Stores notifications and unread count in signals
- `markAllRead()` and `markRead(id)` update local state

#### Polyfill for `global` object
In `src/main.ts`:
```typescript
(window as any).global = window;
