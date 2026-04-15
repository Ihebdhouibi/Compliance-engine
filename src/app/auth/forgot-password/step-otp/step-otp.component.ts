import {
  Component, OnInit, OnDestroy, AfterViewInit,
  ViewChildren, QueryList, ElementRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ForgotPasswordStateService } from '../forgot-password-state.service';
import { DesignSettingsService } from '../../../services/design-settings.service';

@Component({
  selector: 'app-step-otp',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './step-otp.component.html',
  styleUrl: './step-otp.component.scss'
})
export class StepOtpComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChildren('otpInput') inputs!: QueryList<ElementRef<HTMLInputElement>>;

  // Source of truth — never bound to [value], only read for submit
  private values: string[] = ['', '', '', ''];

  isLoading      = false;
  errorMessage   = '';
  isExpired      = false;
  timeLeft       = 180;
  timerDisplay   = '03:00';
  timerClass     = '';
  resendCooldown = 0;

  private timerRef?:  ReturnType<typeof setInterval>;
  private resendRef?: ReturnType<typeof setInterval>;
  private ds = inject(DesignSettingsService);

  constructor(
    private auth:   AuthService,
    public  state:  ForgotPasswordStateService,
    private router: Router
  ) {}

  get maskedEmail(): string {
    const e = this.state.email;
    if (!e) return '';
    const [local, domain] = e.split('@');
    if (!local || !domain) return e;
    return `${local.slice(0, 2)}${'*'.repeat(Math.max(local.length - 2, 3))}@${domain}`;
  }

  get platformTitle(): string { return this.ds.settings().projectTitle || 'AuditAI'; }
  get logoUrl(): string | null { return this.ds.getLogoUrl(); }

  // Computed from private values array — used for submit button disabled state
  get isComplete(): boolean { return this.values.every(v => v !== ''); }

  // Expose for template *ngIf on submit button
  get digits(): string[] { return this.values; }

  ngOnInit(): void {
    if (!this.state.email) {
      this.router.navigate(['/auth/forgot-password']);
      return;
    }
    this.startTimer();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.focusAt(0), 200);
  }

  ngOnDestroy(): void {
    clearInterval(this.timerRef);
    clearInterval(this.resendRef);
  }

  startTimer(): void {
    this.timeLeft  = 180;
    this.isExpired = false;
    clearInterval(this.timerRef);
    this.timerRef = setInterval(() => {
      this.timeLeft--;
      const m = Math.floor(this.timeLeft / 60);
      const s = this.timeLeft % 60;
      this.timerDisplay = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
      this.timerClass   = this.timeLeft <= 30 ? 'danger'
                        : this.timeLeft <= 60 ? 'warning' : '';
      if (this.timeLeft <= 0) {
        clearInterval(this.timerRef);
        this.isExpired = true;
      }
    }, 1000);
  }

  // ── Focus: select existing content so typing replaces it
  onFocus(e: Event): void {
    const el = e.target as HTMLInputElement;
    setTimeout(() => el.select(), 0);
  }

  // ── Keydown: handle backspace + arrows before input event fires
  onKeyDown(e: KeyboardEvent, i: number): void {
    const el = e.target as HTMLInputElement;

    if (e.key === 'Backspace') {
      e.preventDefault();
      // If box has content, clear it
      if (this.values[i]) {
        this.values[i] = '';
        el.value = '';
        this.updateBoxClass(i);
      } else if (i > 0) {
        // Box already empty — move back and clear previous
        this.values[i - 1] = '';
        this.getInput(i - 1).value = '';
        this.updateBoxClass(i - 1);
        this.focusAt(i - 1);
      }
      this.errorMessage = '';
      return;
    }

    if (e.key === 'ArrowLeft')  { e.preventDefault(); if (i > 0) this.focusAt(i - 1); return; }
    if (e.key === 'ArrowRight') { e.preventDefault(); if (i < 3) this.focusAt(i + 1); return; }

    // Block non-numeric printable characters
    if (e.key.length === 1 && !/[0-9]/.test(e.key)) {
      e.preventDefault();
    }
  }

  // ── Input: fires after character appears in the DOM
  onInput(e: Event, i: number): void {
    const el  = e.target as HTMLInputElement;

    // Get only the last digit typed (handles browser autofill edge cases)
    const raw    = el.value.replace(/\D/g, '');
    const single = raw.slice(-1); // take only last char to avoid duplication

    if (!single) {
      this.values[i] = '';
      el.value = '';
      this.updateBoxClass(i);
      return;
    }

    // Set this box only
    this.values[i] = single;
    el.value = single;        // force exact single char
    this.updateBoxClass(i);
    this.errorMessage = '';

    // Advance to next
    if (i < 3) {
      this.focusAt(i + 1);
    }
  }

  // ── Paste on any input
  onPaste(e: ClipboardEvent): void {
    e.preventDefault();
    const text   = e.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, 4);
    if (!digits) return;

    for (let j = 0; j < 4; j++) {
      const val = digits[j] ?? '';
      this.values[j] = val;
      this.getInput(j).value = val;
      this.updateBoxClass(j);
    }

    const next = this.values.findIndex(v => v === '');
    this.focusAt(next === -1 ? 3 : next);
    this.errorMessage = '';
  }

  // ── Update CSS classes directly on the DOM element
  private updateBoxClass(i: number): void {
    const el = this.getInput(i);
    if (!el) return;
    el.classList.toggle('filled', !!this.values[i]);
    el.classList.remove('err');
  }

  private markAllError(): void {
    for (let i = 0; i < 4; i++) {
      this.getInput(i)?.classList.add('err');
    }
  }

  private clearAll(): void {
    this.values = ['', '', '', ''];
    for (let i = 0; i < 4; i++) {
      const el = this.getInput(i);
      if (el) { el.value = ''; el.classList.remove('filled', 'err'); }
    }
  }

  private getInput(i: number): HTMLInputElement {
    return this.inputs?.toArray()[i]?.nativeElement;
  }

  focusAt(i: number): void {
    this.getInput(i)?.focus();
  }

  onSubmit(): void {
    if (!this.isComplete || this.isExpired) return;
    this.isLoading    = true;
    this.errorMessage = '';

    this.auth.forgotStep2(this.state.email, this.values.join('')).subscribe({
      next: () => {
        this.isLoading = false;
        this.state.setCode(this.values.join(''));
        this.router.navigate(['/auth/forgot-password/reset']);
      },
      error: err => {
        this.isLoading    = false;
        this.errorMessage = err.error || 'Invalid code. Please try again.';
        this.clearAll();
        this.markAllError();
        setTimeout(() => this.focusAt(0), 100);
      }
    });
  }

  resendCode(): void {
    this.auth.forgotStep1(this.state.email).subscribe({
      next: () => {
        this.clearAll();
        this.errorMessage   = '';
        this.startTimer();
        this.resendCooldown = 60;
        clearInterval(this.resendRef);
        this.resendRef = setInterval(() => {
          this.resendCooldown--;
          if (this.resendCooldown <= 0) clearInterval(this.resendRef);
        }, 1000);
        setTimeout(() => this.focusAt(0), 100);
      },
      error: () => { this.errorMessage = 'Failed to resend code.'; }
    });
  }
}