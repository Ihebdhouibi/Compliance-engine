import { Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { interval, Subscription } from "rxjs";
import { switchMap, catchError } from "rxjs/operators";
import { of } from "rxjs";
import { environment } from "../environments/environment";
import { UserStoreService } from "../shared/user-store.service";

export interface AppNotification {
  id: number;
  message: string;
  auditType: string;
  submittedAt: string;
  status: string;
  read: boolean;
}

@Injectable({ providedIn: "root" })
export class NotificationService {
  notifications = signal<AppNotification[]>([]);
  unreadCount   = signal<number>(0);
  private pollSub?: Subscription;
  private readIds  = new Set<number>();

  constructor(private http: HttpClient, private userStore: UserStoreService) {}

  startPolling(): void {
    this.fetch();
    this.pollSub = interval(30000).pipe(
      switchMap(() => this.fetchNotifications())
    ).subscribe(data => this.handleData(data));
  }

  stopPolling(): void { this.pollSub?.unsubscribe(); }

  private fetch(): void {
    this.fetchNotifications().subscribe(data => this.handleData(data));
  }

  private fetchNotifications() {
    if (this.userStore.getRole() !== "ROLE_ADMIN") return of([]);
    return this.http.get<AppNotification[]>(
      environment.apiUrl + "/admin/notifications"
    ).pipe(catchError(() => of([])));
  }

  private handleData(data: AppNotification[]): void {
    const mapped = data.map(n => ({ ...n, read: this.readIds.has(n.id) }));
    this.notifications.set(mapped);
    this.unreadCount.set(mapped.filter(n => !n.read).length);
  }

  markAllRead(): void {
    const current = this.notifications();
    current.forEach(n => this.readIds.add(n.id));
    this.notifications.set(current.map(n => ({ ...n, read: true })));
    this.unreadCount.set(0);
  }

  markRead(id: number): void {
    this.readIds.add(id);
    const current = this.notifications();
    this.notifications.set(current.map(n => n.id === id ? { ...n, read: true } : n));
    this.unreadCount.set(current.filter(n => !n.read && n.id !== id).length);
  }

  timeAgo(dateStr: string): string {
    if (!dateStr) return "";
    const diff = Date.now() - new Date(dateStr).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1)  return "Just now";
    if (m < 60) return m + " min ago";
    const h = Math.floor(m / 60);
    if (h < 24) return h + " hr ago";
    return Math.floor(h / 24) + " day(s) ago";
  }
}

