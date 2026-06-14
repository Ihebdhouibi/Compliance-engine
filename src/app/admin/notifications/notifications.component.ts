import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService, AppNotification } from '../../services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  private notifSvc = inject(NotificationService);
  private router   = inject(Router);
  private cdr      = inject(ChangeDetectorRef);

  notifications: AppNotification[] = [];
  selected = new Set<number>();
  loading = false;
  busy = false;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.notifSvc.list().subscribe(data => {
      this.notifications = data;
      // Drop selections for rows that no longer exist.
      this.selected = new Set([...this.selected].filter(id => data.some(n => n.id === id)));
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  get unreadCount(): number { return this.notifications.filter(n => !n.read).length; }
  get selectedCount(): number { return this.selected.size; }
  get allSelected(): boolean {
    return this.notifications.length > 0 && this.selected.size === this.notifications.length;
  }

  isSelected(id: number): boolean { return this.selected.has(id); }

  toggle(id: number): void {
    if (this.selected.has(id)) this.selected.delete(id);
    else this.selected.add(id);
  }

  toggleAll(): void {
    if (this.allSelected) this.selected.clear();
    else this.notifications.forEach(n => this.selected.add(n.id));
  }

  /** Reload the page list and refresh the shared badge after a mutation. */
  private after(): void {
    this.busy = false;
    this.load();
    this.notifSvc.refresh();
  }

  markSelectedRead(): void {
    const ids = [...this.selected];
    if (!ids.length || this.busy) return;
    this.busy = true;
    this.notifSvc.markRead(ids).subscribe({ next: () => this.after(), error: () => this.after() });
  }

  deleteSelected(): void {
    const ids = [...this.selected];
    if (!ids.length || this.busy) return;
    this.busy = true;
    this.notifSvc.remove(ids).subscribe({ next: () => this.after(), error: () => this.after() });
  }

  markAllRead(): void {
    if (this.busy || this.unreadCount === 0) return;
    this.busy = true;
    this.notifSvc.markAllRead().subscribe({ next: () => this.after(), error: () => this.after() });
  }

  open(n: AppNotification): void {
    if (!n.read) this.notifSvc.markRead([n.id]).subscribe({ next: () => this.notifSvc.refresh(), error: () => {} });
    this.router.navigate(['/admin/audits/workspace', n.auditRequestId]);
  }

  markOneRead(n: AppNotification): void {
    if (n.read || this.busy) return;
    this.busy = true;
    this.notifSvc.markRead([n.id]).subscribe({ next: () => this.after(), error: () => this.after() });
  }

  removeOne(n: AppNotification): void {
    if (this.busy) return;
    this.busy = true;
    this.notifSvc.remove([n.id]).subscribe({ next: () => this.after(), error: () => this.after() });
  }

  timeAgo(s: string): string { return this.notifSvc.timeAgo(s); }
  trackById(_: number, n: AppNotification): number { return n.id; }
}
