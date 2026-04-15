import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule, FormBuilder,
  FormGroup, Validators, AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { UserStoreService } from '../../shared/user-store.service';
import {
  ProfileService,
  UpdateCoachProfileRequest
} from '../../services/profile.service';
import { environment } from '../../environments/environment';

function passwordMatch(g: AbstractControl): ValidationErrors | null {
  const p = g.get('newPassword');
  const c = g.get('confirmPassword');
  if (p && c && p.value && p.value !== c.value) {
    c.setErrors({ mismatch: true });
    return { mismatch: true };
  }
  return null;
}

type Tab = 'profile' | 'password';

@Component({
  selector: 'app-auditor-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  private ps        = inject(ProfileService);
  private userStore = inject(UserStoreService);

  activeTab: Tab = 'profile';

  profileForm!:  FormGroup;
  passwordForm!: FormGroup;

  isSavingProfile  = false;
  isSavingPassword = false;
  isUploadingImage = false;

  profileSuccess  = '';
  profileError    = '';
  passwordSuccess = '';
  passwordError   = '';
  imageError      = '';

  showOldPw  = false;
  showNewPw  = false;
  showConfPw = false;

  imagePreview: string | null = null;

  readonly serverBase = environment.serverBaseUrl;

  constructor(private fb: FormBuilder) {}

  get user() { return this.userStore.currentUser(); }

  get avatarUrl(): string | null {
    const u = this.user;
    return u?.profileimage?.url
      ? `${this.serverBase}${u.profileimage.url}`
      : null;
  }

  get initials(): string {
    const u = this.user;
    if (!u) return '?';
    return `${u.firstName?.charAt(0) ?? ''}${u.lastName?.charAt(0) ?? ''}`.toUpperCase()
      || u.email?.charAt(0)?.toUpperCase() || '?';
  }

  ngOnInit(): void {
    this.buildForms();
  }

  private buildForms(): void {
    const u = this.user;

    this.profileForm = this.fb.group({
      firstName:   [u?.firstName   ?? '', Validators.required],
      lastName:    [u?.lastName    ?? '', Validators.required],
      email:       [u?.email       ?? '', [Validators.required, Validators.email]],
      phoneNumber: [u?.phoneNumber ?? ''],
      gender:      [u?.gender      ?? '']
    });

    this.passwordForm = this.fb.group({
      oldPassword:     ['', Validators.required],
      newPassword:     ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatch });
  }

  setTab(t: Tab): void { this.activeTab = t; }

  onImageChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file || !this.user) return;
    this.isUploadingImage = true;
    this.imageError       = '';

    const reader = new FileReader();
    reader.onload = ev => { this.imagePreview = ev.target?.result as string; };
    reader.readAsDataURL(file);

    this.ps.updateProfileImage(this.user.id, file).subscribe({
      next: updated => {
        this.userStore.setUser(updated);
        this.isUploadingImage = false;
      },
      error: () => {
        this.isUploadingImage = false;
        this.imageError = 'Failed to upload image.';
      }
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || !this.user) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.isSavingProfile = true;
    this.profileError    = '';

    const req: UpdateCoachProfileRequest = {
      firstName:   this.profileForm.value.firstName   || undefined,
      lastName:    this.profileForm.value.lastName    || undefined,
      email:       this.profileForm.value.email       || undefined,
      phoneNumber: this.profileForm.value.phoneNumber || undefined,
      gender:      this.profileForm.value.gender      || undefined
    };

    this.ps.updateAdminAuditorProfile(this.user.id, req).subscribe({
      next: updated => {
        this.userStore.setUser(updated);
        this.isSavingProfile = false;
        this.profileSuccess  = 'Profile updated successfully.';
        setTimeout(() => this.profileSuccess = '', 3500);
      },
      error: err => {
        this.isSavingProfile = false;
        this.profileError    = typeof err.error === 'string'
          ? err.error : 'Failed to update profile.';
      }
    });
  }

  savePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.isSavingPassword = true;
    this.passwordError    = '';

    const { oldPassword, newPassword } = this.passwordForm.value;

    this.ps.updatePassword(newPassword, oldPassword).subscribe({
      next: () => {
        this.isSavingPassword = false;
        this.passwordSuccess  = 'Password updated successfully.';
        this.passwordForm.reset();
        setTimeout(() => this.passwordSuccess = '', 3500);
      },
      error: err => {
        this.isSavingPassword = false;
        this.passwordError    = typeof err.error === 'string'
          ? err.error : 'Wrong current password.';
      }
    });
  }

  get pf()  { return this.profileForm.controls; }
  get pwf() { return this.passwordForm.controls; }
}