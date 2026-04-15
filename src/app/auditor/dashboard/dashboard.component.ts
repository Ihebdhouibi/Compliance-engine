import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserStoreService } from '../../shared/user-store.service';

@Component({
  selector: 'app-auditor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  constructor(private store: UserStoreService) {}

  get userName(): string {
    const u = this.store.currentUser();
    return u ? (u.firstName ?? '') : 'Auditor';
  }

  get activeAudits(): number {
    return this.assignedAudits.filter(a => a.status === 'active').length;
  }

  stats = [
    { label: 'Assigned Audits', value: '8',  change: '+2 this month', up: true,  color: '#00d4ed', glow: '#00d4ed', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/></svg>` },
    { label: 'In Progress',     value: '3',  change: 'Active now',    up: true,  color: '#f5b800', glow: '#f5b800', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>` },
    { label: 'Completed',       value: '47', change: '+5 this month', up: true,  color: '#00e5a0', glow: '#00e5a0', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>` },
    { label: 'Avg. Days/Audit', value: '12', change: '-2 vs last mo', up: true,  color: '#18e8ff', glow: '#18e8ff', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>` },
  ];

  assignedAudits = [
    { initials: 'AC', company: 'Acme Corp',    type: 'AI Bias Audit',       status: 'active',    statusLabel: 'In Progress', deadline: 'Due in 3 days' },
    { initials: 'TF', company: 'TechFlow Ltd', type: 'Data Privacy Review', status: 'active',    statusLabel: 'In Progress', deadline: 'Due in 7 days' },
    { initials: 'NS', company: 'NovaSec',      type: 'Risk Assessment',     status: 'active',    statusLabel: 'In Progress', deadline: 'Due in 14 days' },
    { initials: 'FI', company: 'FinServe Grp', type: 'Compliance Audit',    status: 'pending',   statusLabel: 'Pending',     deadline: 'Due in 21 days' },
    { initials: 'GH', company: 'GlobalHR Inc', type: 'AI Model Review',     status: 'completed', statusLabel: 'Completed',   deadline: 'Submitted' },
  ];
}
