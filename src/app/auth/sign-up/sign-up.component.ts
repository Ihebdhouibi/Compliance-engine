import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DesignSettingsService } from '../../services/design-settings.service';

@Component({
  selector: 'app-sign-up',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './sign-up.component.html',
  styleUrl: './sign-up.component.scss'
})
export class SignUpComponent implements OnInit {
  form!: FormGroup;
  isLoading      = false;
  errorMessage   = '';
  successMessage = '';

  private ds = inject(DesignSettingsService);

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName:       ['', Validators.required],
      lastName:        ['', Validators.required],
      email:           ['', [Validators.required, Validators.email]],
      phoneNumber:     ['', Validators.required],
      companyName:     ['', Validators.required],
      companyActivity: ['', Validators.required],
      companyAddress:  ['', Validators.required]
    });
  }

  get fc() { return this.form.controls; }

  get platformTitle(): string {
    return this.ds.settings().projectTitle || 'AuditAI';
  }

  get logoUrl(): string | null {
    return this.ds.getLogoUrl();
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.isLoading    = true;
    this.errorMessage = '';

    this.auth.signUp(this.form.value).subscribe({
      next: () => {
        this.isLoading      = false;
        this.successMessage = 'Registration successful! Check your email for your login credentials.';
        setTimeout(() => this.router.navigate(['/auth/login']), 3500);
      },
      error: err => {
        this.isLoading    = false;
        this.errorMessage = err.error || 'Registration failed.';
      }
    });
  }
}
