import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DesignSettingsService } from '../../services/design-settings.service';

interface Particle {
  left: number; bottom: number;
  duration: string; delay: string;
  size: number; opacity: number;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  form!: FormGroup;
  isLoading    = false;
  errorMessage = '';
  showPw       = false;
  particles: Particle[] = [];

  private ds = inject(DesignSettingsService);

  constructor(
    private fb:   FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.particles = Array.from({ length: 12 }, () => ({
      left:     Math.random() * 100,
      bottom:   Math.random() * 40,
      duration: `${4 + Math.random() * 6}s`,
      delay:    `-${Math.random() * 8}s`,
      size:     2 + Math.random() * 3,
      opacity:  0.2 + Math.random() * 0.5
    }));
  }

  get fc() { return this.form.controls; }

  // Dynamic branding from settings
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

    this.auth.signIn({
      username:   this.form.value.username,
      password:   this.form.value.password,
      deviceType: 'WEB',
      deviceId:   navigator.userAgent.slice(0, 50),
      ip:         ''
    }).subscribe({
      next: res => {
        this.auth.loadCurrentUser().subscribe({
          next: () => {
            this.isLoading = false;
            if (res.roles === 'ROLE_ADMIN')        this.router.navigate(['/admin/dashboard']);
            else if (res.roles === 'ROLE_AUDITOR') this.router.navigate(['/auditor/dashboard']);
            else                                   this.router.navigate(['/company/dashboard']);
          },
          error: () => { this.isLoading = false; }
        });
      },
      error: err => {
        this.isLoading    = false;
        this.errorMessage = err.error || 'Invalid credentials.';
      }
    });
  }
}