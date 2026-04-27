import {
  Component, Input, Output, EventEmitter, inject,
  ViewChild, ElementRef, AfterViewChecked, NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

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
  @Output() closed = new EventEmitter<void>();

  // ★ Reference to the scrollable messages container
  @ViewChild('messagesContainer')
  private messagesContainer!: ElementRef<HTMLDivElement>;

  // ★ Flag: scroll after next view check
  private _needsScroll = false;

  messages:   BotMessage[] = [];
  inputText   = '';
  isThinking  = false;

  private http  = inject(HttpClient);
  private zone  = inject(NgZone);

  // ★ Called after every change detection cycle
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

  get contextSummary(): string {
    if (!this.requestData) return '';
    const answers = (this.requestData.answers || [])
      .map((a: any) => `${a.fieldLabel}: ${a.answerValue || '[file]'}`)
      .join('\n');
    return `Audit Type: ${this.requestData.auditType}\nCompany: ${
      this.requestData.submittedBy?.companyInfo?.companyName || 'N/A'
    }\nAnswers:\n${answers}`;
  }

  send(): void {
    const text = this.inputText.trim();
    if (!text || this.isThinking) return;

    // Push user message and trigger scroll
    this.messages.push({ role: 'user', text, time: new Date() });
    this.inputText      = '';
    this.isThinking     = true;
    this._needsScroll   = true;  // ★ scroll after user message renders

    const systemPrompt = `You are an expert AI audit assistant. 
Help the auditor analyze audit requests, check compliance, identify risks, 
and make informed decisions. Be concise and professional.
${this.contextSummary ? '\nAudit context:\n' + this.contextSummary : ''}`;

    const apiMessages = this.messages
      .filter(m => m.role === 'user')
      .map(m => ({ role: 'user' as const, content: m.text }));

    this.http.post<any>('https://api.anthropic.com/v1/messages', {
      model:      'claude-sonnet-4-20250514',
      max_tokens: 1000,
      system:     systemPrompt,
      messages:   apiMessages
    }).subscribe({
      next: res => {
        const reply = res.content?.[0]?.text ?? 'No response received.';
        this.messages.push({ role: 'bot', text: reply, time: new Date() });
        this.isThinking   = false;
        this._needsScroll = true;  // ★ scroll after bot response renders
      },
      error: () => {
        this.messages.push({
          role: 'bot',
          text: 'Failed to reach AI assistant. Please try again.',
          time: new Date()
        });
        this.isThinking   = false;
        this._needsScroll = true;
      }
    });
  }

  onKey(e: KeyboardEvent): void {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.send();
    }
  }

  close(): void { this.closed.emit(); }
}