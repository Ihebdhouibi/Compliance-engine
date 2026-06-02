topbar_ts = """import { Component, HostListener, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule } from "@angular/router";
import { UserStoreService } from "../../shared/user-store.service";
import { AuthService } from "../../services/auth.service";
import { environment } from "../../environments/environment";
import { DesignSettingsService } from "../../services/design-settings.service";
import { NotificationService } from "../../services/notification.service";

@Component({
  selector: "app-topbar",
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: "./topbar.component.html",
  styleUrl: "./topbar.component.scss"
})
export class TopbarComponent implements OnInit, OnDestroy {
  showNotif = false;

  constructor(
    public  userStore: UserStoreService,
    public  notifService: NotificationService,
    private authService: AuthService,
    private ds: DesignSettingsService
  ) {}

  ngOnInit(): void {
    if (this.userStore.getRole() === "ROLE_ADMIN") {
      this.notifService.startPolling();
    }
  }

  ngOnDestroy(): void { this.notifService.stopPolling(); }

  get isAdmin(): boolean { return this.userStore.getRole() === "ROLE_ADMIN"; }

  get userName(): string {
    const u = this.userStore.currentUser();
    if (!u) return "-";
    return ((u.firstName ?? "") + " " + (u.lastName ?? "")).trim() || u.email || "-";
  }

  get roleLabel(): string {
    const r = this.userStore.getRole();
    if (r === "ROLE_ADMIN")   return "Administrator";
    if (r === "ROLE_AUDITOR") return "Auditor";
    return "Company";
  }

  get initials(): string {
    const u = this.userStore.currentUser();
    if (!u) return "?";
    const first = u.firstName?.charAt(0)?.toUpperCase() ?? "";
    const last  = u.lastName?.charAt(0)?.toUpperCase()  ?? "";
    if (first || last) return first + last;
    return u.email?.charAt(0)?.toUpperCase() ?? "?";
  }

  get profileImageUrl(): string | null {
    const u = this.userStore.currentUser();
    if (u?.profileimage?.url) return environment.serverBaseUrl + u.profileimage.url;
    return null;
  }

  get platformTitle(): string {
    return this.ds.settings().projectTitle || "AI Audit System";
  }

  toggleNotif(): void { this.showNotif = !this.showNotif; }
  markRead(): void { this.notifService.markAllRead(); }
  logout(): void { this.authService.signOut(); }

  @HostListener("document:click", ["$event"])
  onDocumentClick(e: MouseEvent): void {
    if (!(e.target as Element).closest(".topbar__notif-wrap")) {
      this.showNotif = false;
    }
  }
}
"""

topbar_html = """<header class="topbar">
  <div class="topbar__left">
    <div class="topbar__tag">
      <span class="dot"></span>
      <span>{{ platformTitle }}</span>
    </div>
  </div>
  <div class="topbar__right">
    <div class="topbar__notif-wrap" *ngIf="isAdmin">
      <button class="topbar__icon-btn" (click)="toggleNotif()" aria-label="Notifications">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:17px;height:17px;display:block;">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        <span class="notif-badge" *ngIf="notifService.unreadCount() > 0">{{ notifService.unreadCount() }}</span>
      </button>
      <div class="notif-panel" *ngIf="showNotif">
        <div class="notif-panel__head">
          <span>Notifications</span>
          <button (click)="markRead()">Mark all read</button>
        </div>
        <div class="notif-panel__empty" *ngIf="notifService.notifications().length === 0">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:32px;height:32px;margin:0 auto 10px;display:block;">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          <p>No new audit requests</p>
        </div>
        <div class="notif-panel__item"
             *ngFor="let n of notifService.notifications()"
             [class.unread]="!n.read"
             (click)="notifService.markRead(n.id)">
          <div class="n-dot" [class.n-dot--read]="n.read"></div>
          <div class="n-body">
            <p>{{ n.message }}</p>
            <span class="n-type">{{ n.auditType }}</span>
            <span class="n-time">{{ notifService.timeAgo(n.submittedAt) }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="topbar__user">
      <div class="topbar__avatar" [title]="userName">
        <img *ngIf="profileImageUrl" [src]="profileImageUrl" [alt]="userName"
          style="width:100%;height:100%;object-fit:cover;display:block;border-radius:50%;" />
        <span class="topbar__initials" *ngIf="!profileImageUrl">{{ initials }}</span>
      </div>
      <div class="topbar__user-info">
        <span class="u-name">{{ userName }}</span>
        <span class="u-role">{{ roleLabel }}</span>
      </div>
    </div>
    <button class="topbar__logout" (click)="logout()">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:17px;height:17px;display:block;">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
        <polyline points="16 17 21 12 16 7" />
        <line x1="21" y1="12" x2="9" y2="12" />
      </svg>
      <span>Sign Out</span>
    </button>
  </div>
</header>
"""

with open("src/app/layout/topbar/topbar.component.ts", "w", encoding="utf-8") as f:
    f.write(topbar_ts)
print("topbar.component.ts updated!")

with open("src/app/layout/topbar/topbar.component.html", "w", encoding="utf-8") as f:
    f.write(topbar_html)
print("topbar.component.html updated!")
