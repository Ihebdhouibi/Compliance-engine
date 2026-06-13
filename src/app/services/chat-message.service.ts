import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/** A persisted AI Audit Assistant turn, scoped to one audit request. */
export interface AuditChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sources?: string;       // JSON-encoded RAG sources (assistant turns)
  createdAt: string;
}

export interface SaveChatMessagePayload {
  role: 'USER' | 'ASSISTANT';
  content: string;
  sources?: string;
}

@Injectable({ providedIn: 'root' })
export class ChatMessageService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getMessages(requestId: number): Observable<AuditChatMessage[]> {
    return this.http.get<AuditChatMessage[]>(
      `${this.base}/audit-requests/${requestId}/chat-messages`
    );
  }

  saveMessage(requestId: number, payload: SaveChatMessagePayload): Observable<AuditChatMessage> {
    return this.http.post<AuditChatMessage>(
      `${this.base}/audit-requests/${requestId}/chat-messages`,
      payload
    );
  }

  clear(requestId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/audit-requests/${requestId}/chat-messages`
    );
  }
}
