import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, Renderer2
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminUsersService } from '../../services/admin-users.service';
import { User } from '../../models/user.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit, OnDestroy {

  users:       User[] = [];
  totalItems   = 0;
  totalPages   = 0;
  currentPage  = 0;
  pageSize     = 8;
  isLoading    = false;

  searchTerm    = '';
  filterActive: boolean | undefined = undefined;
  filterGender  = '';

  selectedUser:  User | null = null;
  showView       = false;

  activatingId:  number | null = null;
  resendingId:   number | null = null;
  resendSuccess  = '';
  resendError    = '';

  readonly serverBase = environment.serverBaseUrl;

  constructor(
    private svc:      AdminUsersService,
    private cdr:      ChangeDetectorRef,
    private renderer: Renderer2
  ) {}

  ngOnInit(): void { this.load(); }

  ngOnDestroy(): void {
    this.renderer.removeStyle(document.body, 'overflow');
  }

  load(): void {
    this.isLoading = true;
    this.svc.getUsers({
      page:   this.currentPage,
      size:   this.pageSize,
      search: this.searchTerm || undefined,
      active: this.filterActive,
      gender: this.filterGender || undefined
    }).subscribe({
      next: res => {
        this.users      = res.result;
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

  openView(user: User): void {
    this.selectedUser = user;
    this.showView     = true;
    this.renderer.setStyle(document.body, 'overflow', 'hidden');
  }

  closeView(): void {
    this.selectedUser = null;
    this.showView     = false;
    this.renderer.removeStyle(document.body, 'overflow');
  }

  toggleActivation(user: User): void {
    this.activatingId = user.id;
    this.svc.toggleActivation(user.id).subscribe({
      next: updated => {
        const idx = this.users.findIndex(u => u.id === user.id);
        if (idx !== -1) this.users[idx] = { ...updated };
        this.activatingId = null;
        this.cdr.detectChanges();
      },
      error: () => { this.activatingId = null; }
    });
  }

  resendPassword(user: User): void {
    this.resendingId  = user.id;
    this.resendSuccess = '';
    this.resendError   = '';
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
      error: () => {
        this.resendingId = null;
        this.resendError = 'Failed to send password.';
        setTimeout(() => { this.resendError = ''; }, 4000);
      }
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
}