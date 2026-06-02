import { Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { environment } from "../environments/environment";

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
  sources?: RuleSource[];
  loading?: boolean;
}

export interface RuleSource {
  rule_id: string;
  score: number;
  entry_type: string;
  payload: any;
}

@Injectable({ providedIn: "root" })
export class ChatService {
  messages = signal<ChatMessage[]>([]);
  loading  = signal<boolean>(false);

  constructor(private http: HttpClient) {}

  sendMessage(message: string, section?: string): void {
    if (!message.trim()) return;
    this.messages.update(m => [...m, { role: "user", content: message }]);
    this.messages.update(m => [...m, { role: "assistant", content: "", loading: true }]);
    this.loading.set(true);
    this.http.post<any>(environment.ragApiUrl + "/chat/", { message, limit: 5, section }).subscribe({
      next: (res) => {
        this.messages.update(msgs => {
          const updated = [...msgs];
          updated[updated.length - 1] = { role: "assistant", content: res.answer, sources: res.sources, loading: false };
          return updated;
        });
        this.loading.set(false);
      },
      error: () => {
        this.messages.update(msgs => {
          const updated = [...msgs];
          updated[updated.length - 1] = { role: "assistant", content: "Sorry, could not connect to AI assistant. Ensure the Python API is running on port 8000.", loading: false };
          return updated;
        });
        this.loading.set(false);
      }
    });
  }

  clearMessages(): void { this.messages.set([]); }
}
