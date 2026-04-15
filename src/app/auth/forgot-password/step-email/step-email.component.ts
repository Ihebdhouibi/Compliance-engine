import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ForgotPasswordStateService } from '../forgot-password-state.service';
import { DesignSettingsService } from '../../../services/design-settings.service';


@Component({
  selector: 'app-step-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './step-email.component.html',
  styleUrl: './step-email.component.scss'
})
export class StepEmailComponent implements OnInit {
  form!: FormGroup;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private state: ForgotPasswordStateService,
    private router: Router,
    private ds: DesignSettingsService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isLoading = true;
    this.errorMessage = '';
    const email = this.form.value.email;

    this.authService.forgotStep1(email).subscribe({
      next: () => {
        this.state.setEmail(email);
        this.isLoading = false;
        this.router.navigate(['/auth/forgot-password/verify']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Email not found.';
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