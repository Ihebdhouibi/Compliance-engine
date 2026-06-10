import {
  Component, Input, Output, EventEmitter, inject,
  ViewChild, ElementRef, AfterViewChecked, NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

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
export class AuditBotComponent implements AfterViewChecked {
  @Input() requestData: any = null;
  /** Provider returning a fresh text snapshot of the audit under review.
   *  Called at send-time so the assistant always sees the current step/answers. */
  @Input() contextProvider?: () => string;
  @Output() closed = new EventEmitter<void>();

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
    this.inputText = '';
    this.isThinking = true;
    this._needsScroll = true;

    // Call FastAPI chat endpoint, grounded in the current audit context.
    const url = `${environment.ragApiUrl}/chat/`;
    const context = this.contextProvider?.()?.slice(0, 24000) || undefined;
    const body = { message: text, limit: 5, context };

    this.http.post<{ answer: string; sources: any[] }>(url, body).subscribe({
      next: (res) => {
        const reply = res.answer || 'No response received.';
        this.messages.push({ role: 'bot', text: reply, time: new Date() });
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

  close(): void {
    this.closed.emit();
  }
}