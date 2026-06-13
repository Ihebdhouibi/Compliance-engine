import {
  Component, Input, Output, EventEmitter, inject,
  ViewChild, ElementRef, AfterViewChecked, OnInit, NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { log } from '../../core/logging/log';
import { ChatMessageService } from '../../services/chat-message.service';

interface BotMessage {
  role: 'user' | 'bot';
  text: string;
  time: Date;
}

@Component({
  selector: 'app-audit-bot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-bot.component.html',
  styleUrl: './audit-bot.component.scss'
})
export class AuditBotComponent implements OnInit, AfterViewChecked {
  @Input() requestData: any = null;
  /** Provider returning a fresh text snapshot of the audit under review.
   *  Called at send-time so the assistant always sees the current step/answers. */
  @Input() contextProvider?: () => string;
  @Output() closed = new EventEmitter<void>();
  /** Emitted when the auditor pushes an assistant reply into the audit. */
  @Output() insert = new EventEmitter<{ target: 'finding' | 'recommendation' | 'note'; text: string }>();

  @ViewChild('messagesContainer')
  private messagesContainer!: ElementRef<HTMLDivElement>;

  private _needsScroll = false;

  messages: BotMessage[] = [];
  inputText = '';
  isThinking = false;

  /** One-tap prompts; the audit context supplies the specifics per step. */
  @Input() quickPrompts: string[] = [
    'Summarise the evidence for this step',
    'Which RICS clauses apply here?',
    'Is the firm compliant on this step?',
    'What evidence is still missing?',
    'Draft a finding for this step',
  ];

  private http = inject(HttpClient);
  private zone = inject(NgZone);
  private chatStore = inject(ChatMessageService);

  /** Numeric id of the audit this conversation belongs to, if any. */
  private get requestId(): number | null {
    const id = this.requestData?.id;
    if (id === null || id === undefined) return null;
    const n = Number(id);
    return Number.isFinite(n) ? n : null;
  }

  ngOnInit(): void {
    // The panel is recreated each time it is opened (it lives under *ngIf), so
    // rehydrate the transcript from the backend, keyed by the audit, instead of
    // starting empty. This is what makes the discussion persist across closes,
    // reloads, and sessions.
    const id = this.requestId;
    if (id === null) return;
    this.chatStore.getMessages(id).subscribe({
      next: (rows) => {
        this.messages = rows.map((r) => ({
          role: r.role === 'ASSISTANT' ? 'bot' : 'user',
          text: r.content,
          time: r.createdAt ? new Date(r.createdAt) : new Date(),
        }));
        this._needsScroll = true;
      },
      error: (err) => log.warn('ng.audit-bot', 'failed to load chat history', err),
    });
  }

  ngAfterViewChecked(): void {
    if (this._needsScroll) {
      this._doScroll();
      this._needsScroll = false;
    }
  }

  private _doScroll(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    } catch {}
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.isThinking) return;

    // Add user message
    this.messages.push({ role: 'user', text, time: new Date() });
    this.persist('USER', text);
    this.inputText = '';
    this.isThinking = true;
    this._needsScroll = true;

    // Call FastAPI chat endpoint, grounded in the current audit context.
    const url = `${environment.ragApiUrl}/chat/`;
    const context = this.contextProvider?.()?.slice(0, 24000) || undefined;
    const body = { message: text, limit: 5, context };
    log.info('ng.audit-bot', 'ask assistant', { chars: text.length, hasContext: !!context });

    this.http.post<{ answer: string; sources: any[] }>(url, body).subscribe({
      next: (res) => {
        const reply = res.answer || 'No response received.';
        log.info('ng.audit-bot', 'assistant replied', { chars: reply.length, sources: res.sources?.length ?? 0 });
        log.debug('ng.audit-bot', 'answer', reply);
        this.messages.push({ role: 'bot', text: reply, time: new Date() });
        this.persist('ASSISTANT', reply, res.sources);
        this.isThinking = false;
        this._needsScroll = true;
      },
      error: (err) => {
        console.error('Chat error:', err);
        this.messages.push({
          role: 'bot',
          text: 'Failed to reach AI assistant. Please try again.',
          time: new Date()
        });
        this.isThinking = false;
        this._needsScroll = true;
      }
    });
  }

  runPrompt(p: string): void {
    if (this.isThinking) return;
    this.inputText = p;
    this.send();
  }

  onKey(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.send();
    }
  }

  insertAs(target: 'finding' | 'recommendation' | 'note', text: string): void {
    if (!text?.trim()) return;
    this.insert.emit({ target, text: text.trim() });
  }

  /** Persist one turn to the audit-scoped transcript. Best-effort: a failed
   *  save is logged but never blocks the conversation. Transient errors (e.g.
   *  "failed to reach AI assistant") are intentionally not persisted. */
  private persist(role: 'USER' | 'ASSISTANT', content: string, sources?: any[]): void {
    const id = this.requestId;
    if (id === null) return;
    this.chatStore.saveMessage(id, {
      role,
      content,
      sources: sources && sources.length ? JSON.stringify(sources) : undefined,
    }).subscribe({
      error: (err) => log.warn('ng.audit-bot', 'failed to persist message', { role, err }),
    });
  }

  close(): void {
    this.closed.emit();
  }
}
