import { Component, HostListener, OnInit, OnDestroy } from "@angular/core";
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
