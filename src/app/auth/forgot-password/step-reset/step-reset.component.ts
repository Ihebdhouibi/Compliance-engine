import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignSettingsService } from '../../../services/design-settings.service';

import {
  ReactiveFormsModule, FormBuilder, FormGroup,
  Validators, AbstractControl, ValidationErrors
} from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ForgotPasswordStateService } from '../forgot-password-state.service';

function matchPasswords(g: AbstractControl): ValidationErrors | null {
  const p = g.get('password');
  const c = g.get('confirmPassword');
  if (p && c && p.value !== c.value) {
    c.setErrors({ mismatch: true });
    return { mismatch: true };
  }
  return null;
}

@Component({
  selector: 'app-step-reset',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './step-reset.component.html',
  styleUrl: './step-reset.component.scss'
})
export class StepResetComponent implements OnInit {
  form!: FormGroup;
  isLoading   = false;
  errorMessage = '';
  isSuccess   = false;
  showPw      = false;
  showConfirm = false;

  constructor(
    private fb:     FormBuilder,
    private auth:   AuthService,
    private state:  ForgotPasswordStateService,
    private router: Router,
    private ds: DesignSettingsService
  ) {}

  ngOnInit(): void {
    if (!this.state.email || !this.state.verifiedCode) {
      this.router.navigate(['/auth/forgot-password']);
      return;
    }
    this.form = this.fb.group({
      password:        ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: matchPasswords });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isLoading    = true;
    this.errorMessage = '';

    this.auth.forgotStep3(
      this.state.email,
      this.state.verifiedCode,
      this.form.value.password
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSuccess = true;
        this.state.clear();
      },
      error: err => {
        this.isLoading    = false;
        this.errorMessage = err.error || 'Failed to reset password. Please try again.';
      }
    });
  }
  

get platformTitle(): string {
  return this.ds.settings().projectTitle || 'AuditAI';
}

get logoUrl(): string | null {
  return this.ds.getLogoUrl();
}
}