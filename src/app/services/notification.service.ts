import { Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { interval, Observable, Subscription, of } from "rxjs";
import { switchMap, catchError } from "rxjs/operators";
import { environment } from "../environments/environment";
import { UserStoreService } from "../shared/user-store.service";

export interface AppNotification {
  id: number;             // notification id (used for read/delete)
  message: string;
  auditType: string;
  auditRequestId: number; // target audit (used for "Open")
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: "root" })
export class NotificationService {
  notifications = signal<AppNotification[]>([]);
  unreadCount   = signal<number>(0);

  private pollSub?: Subscription;
  private readonly base = environment.apiUrl + "/admin/notifications";

  constructor(private http: HttpClient, private userStore: UserStoreService) {}

  startPolling(): void {
    this.refresh();
    this.pollSub = interval(30000).pipe(
      switchMap(() => this.fetch())
    ).subscribe(data => this.setData(data));
  }

  stopPolling(): void { this.pollSub?.unsubscribe(); }

  /** Re-fetch the feed and update the shared signals (badge + dropdown). */
  refresh(): void {
    this.fetch().subscribe(data => this.setData(data));
  }

  /** Raw fetch — also used directly by the dedicated notifications page. */
  list(): Observable<AppNotification[]> { return this.fetch(); }

  private fetch(): Observable<AppNotification[]> {
    if (this.userStore.getRole() !== "ROLE_ADMIN") return of([]);
    return this.http
      .get<AppNotification[]>(this.base)
      .pipe(catchError(() => of([])));
  }

  private setData(data: AppNotification[]): void {
    this.notifications.set(data);
    this.unreadCount.set(data.filter(n => !n.read).length);
  }

  markRead(ids: number[]): Observable<void> {
    return this.http.patch<void>(`${this.base}/read`, { ids });
  }

  markAllRead(): Observable<void> {
    return this.http.patch<void>(`${this.base}/read-all`, {});
  }

  remove(ids: number[]): Observable<void> {
    return this.http.request<void>("delete", this.base, { body: { ids } });
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
