import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserStoreService } from '../../shared/user-store.service';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../environments/environment';
import { DesignSettingsService } from '../../services/design-settings.service';


interface Notification {
  message: string;
  time: string;
  read: boolean;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {

  showNotif = false;

  notifications: Notification[] = [
    { message: 'New audit request submitted by Acme Corp.', time: '2 min ago', read: false },
    { message: 'Auditor assigned to Report #012.',          time: '1 hr ago',  read: false },
  ];

  constructor(
    public  userStore: UserStoreService,
    private authService: AuthService,
    private ds: DesignSettingsService
  ) {}

  get unread(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  get userName(): string {
    const u = this.userStore.currentUser();
    if (!u) return '—';
    return `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || u.email || '—';
  }

  get roleLabel(): string {
    const r = this.userStore.getRole();
    if (r === 'ROLE_ADMIN')   return 'Administrator';
    if (r === 'ROLE_AUDITOR') return 'Auditor';
    return 'Company';
  }

  get initials(): string {
    const u = this.userStore.currentUser();
    if (!u) return '?';
    const first = u.firstName?.charAt(0)?.toUpperCase() ?? '';
    const last  = u.lastName?.charAt(0)?.toUpperCase()  ?? '';
    if (first || last) return first + last;
    return u.email?.charAt(0)?.toUpperCase() ?? '?';
  }

  // Only returns a URL if the user actually has a server-side profile image
  // Returns null otherwise — no fallback file path at all
  get profileImageUrl(): string | null {
    const u = this.userStore.currentUser();
    if (u?.profileimage?.url) {
      return `${environment.serverBaseUrl}${u.profileimage.url}`;
    }
    return null;
  }

  toggleNotif(): void { this.showNotif = !this.showNotif; }
  markRead():    void { this.notifications = this.notifications.map(n => ({ ...n, read: true })); }
  logout():      void { this.authService.signOut(); }

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent): void {
    if (!(e.target as Element).closest('.topbar__notif-wrap')) {
      this.showNotif = false;
    }
  }

  get platformTitle(): string {
  return this.ds.settings().projectTitle || 'AI Audit System';
}
}