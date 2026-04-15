import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, Renderer2,
  ViewChild, TemplateRef, EmbeddedViewRef,
  ApplicationRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule, ReactiveFormsModule,
  FormBuilder, FormGroup, Validators
} from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  AdminUsersService,
  AddAuditorRequest,
  UpdateAuditorRequest
} from '../../services/admin-users.service';
import { User } from '../../models/user.model';
import { environment } from '../../environments/environment';

type ModalMode = 'add' | 'edit' | 'view' | null;

@Component({
  selector: 'app-auditors',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './auditors.component.html',
  styleUrl: './auditors.component.scss'
})
export class AuditorsComponent implements OnInit, OnDestroy {

  auditors:    User[] = [];
  totalItems   = 0;
  totalPages   = 0;
  currentPage  = 0;
  pageSize     = 8;
  isLoading    = false;

  searchTerm    = '';
  filterActive: boolean | undefined = undefined;
  filterGender  = '';

  modalMode:    ModalMode = null;
  selectedUser: User | null = null;
  modalLoading  = false;
  modalError    = '';
  modalSuccess  = '';

  activatingId:  number | null = null;
  resendingId:   number | null = null;
  resendSuccess  = '';

  addForm!:  FormGroup;
  editForm!: FormGroup;

  readonly serverBase = environment.serverBaseUrl;

  constructor(
    private svc:      AdminUsersService,
    private fb:       FormBuilder,
    private cdr:      ChangeDetectorRef,
    private renderer: Renderer2
  ) {}

  ngOnInit(): void {
    this.buildForms();
    this.load();
  }

  ngOnDestroy(): void {
    this.renderer.removeStyle(document.body, 'overflow');
  }

  private buildForms(): void {
    this.addForm = this.fb.group({
      firstName:   ['', Validators.required],
      lastName:    ['', Validators.required],
      email:       ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      gender:      ['']
    });
    this.editForm = this.fb.group({
      firstName:   ['', Validators.required],
      lastName:    ['', Validators.required],
      email:       ['', [Validators.required, Validators.email]],
      phoneNumber: [''],
      gender:      ['']
    });
  }

  load(): void {
    this.isLoading = true;
    this.svc.getAuditors({
      page:   this.currentPage,
      size:   this.pageSize,
      search: this.searchTerm || undefined,
      active: this.filterActive,
      gender: this.filterGender || undefined
    }).subscribe({
      next: res => {
        this.auditors   = res.result;
        this.totalItems = res.totalItems;
        this.totalPages = res.totalPages;
        this.isLoading  = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  onSearch():       void { this.currentPage = 0; this.load(); }
  onFilterChange(): void { this.currentPage = 0; this.load(); }

  clearFilters(): void {
    this.searchTerm   = '';
    this.filterActive = undefined;
    this.filterGender = '';
    this.currentPage  = 0;
    this.load();
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.load();
  }

  get pages(): number[] {
    const total = Math.min(this.totalPages, 7);
    const start = Math.max(0, Math.min(this.currentPage - 3, this.totalPages - total));
    return Array.from({ length: total }, (_, i) => start + i);
  }

  openAdd(): void {
    this.addForm.reset();
    this.modalError = this.modalSuccess = '';
    this.modalMode  = 'add';
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  openView(user: User): void {
    this.selectedUser = user;
    this.modalMode    = 'view';
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  openEdit(user: User): void {
    this.selectedUser = user;
    this.editForm.patchValue({
      firstName:   user.firstName   ?? '',
      lastName:    user.lastName    ?? '',
      email:       user.email       ?? '',
      phoneNumber: user.phoneNumber ?? '',
      gender:      user.gender      ?? ''
    });
    this.modalError = this.modalSuccess = '';
    this.modalMode  = 'edit';
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeModal(): void {
    this.modalMode    = null;
    this.selectedUser = null;
    this.modalError   = this.modalSuccess = '';
    this.renderer.removeStyle(document.body, 'overflow');
  }

  submitAdd(): void {
    if (this.addForm.invalid) { this.addForm.markAllAsTouched(); return; }
    this.modalLoading = true;
    this.modalError   = '';

    const req: AddAuditorRequest = {
      firstName:   this.addForm.value.firstName,
      lastName:    this.addForm.value.lastName,
      email:       this.addForm.value.email,
      phoneNumber: this.addForm.value.phoneNumber || undefined,
      gender:      this.addForm.value.gender      || undefined
    };

    this.svc.addAuditor(req).subscribe({
      next: () => {
        this.modalLoading = false;
        this.modalSuccess = 'Auditor created. Login credentials sent by email.';
        setTimeout(() => { this.closeModal(); this.load(); }, 2000);
      },
      error: err => {
        this.modalLoading = false;
        this.modalError   = typeof err.error === 'string'
          ? err.error : 'Failed to create auditor.';
      }
    });
  }

  submitEdit(): void {
    if (this.editForm.invalid || !this.selectedUser) {
      this.editForm.markAllAsTouched(); return;
    }
    this.modalLoading = true;
    this.modalError   = '';

    const req: UpdateAuditorRequest = {
      firstName:   this.editForm.value.firstName,
      lastName:    this.editForm.value.lastName,
      email:       this.editForm.value.email,
      phoneNumber: this.editForm.value.phoneNumber || undefined,
      gender:      this.editForm.value.gender      || undefined
    };

    this.svc.updateAuditor(this.selectedUser.id, req).subscribe({
      next: () => {
        this.modalLoading = false;
        this.modalSuccess = 'Profile updated successfully.';
        setTimeout(() => { this.closeModal(); this.load(); }, 1500);
      },
      error: err => {
        this.modalLoading = false;
        this.modalError   = typeof err.error === 'string'
          ? err.error : 'Failed to update profile.';
      }
    });
  }

  toggleActivation(user: User): void {
    this.activatingId = user.id;
    this.svc.toggleActivation(user.id).subscribe({
      next: updated => {
        const idx = this.auditors.findIndex(a => a.id === user.id);
        if (idx !== -1) this.auditors[idx] = { ...updated };
        this.activatingId = null;
        this.cdr.detectChanges();
      },
      error: () => { this.activatingId = null; }
    });
  }

  resendPassword(user: User): void {
    this.resendingId   = user.id;
    this.resendSuccess = '';
    this.svc.resendPassword(user.email).subscribe({
      next: () => {
        this.resendingId   = null;
        this.resendSuccess = `New password sent to ${user.email}`;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.resendSuccess = '';
          this.cdr.detectChanges();
        }, 4000);
      },
      error: () => { this.resendingId = null; }
    });
  }

  avatarUrl(user: User): string | null {
    return user.profileimage?.url
      ? `${this.serverBase}${user.profileimage.url}`
      : null;
  }

  initials(user: User): string {
    const f = user.firstName?.charAt(0)?.toUpperCase() ?? '';
    const l = user.lastName?.charAt(0)?.toUpperCase()  ?? '';
    return f + l || user.email?.charAt(0)?.toUpperCase() || '?';
  }

  get fc() { return this.addForm.controls; }
  get ef() { return this.editForm.controls; }
}