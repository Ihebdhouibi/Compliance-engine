import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserStoreService } from '../../shared/user-store.service';

@Component({
  selector: 'app-company-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  constructor(private store: UserStoreService) {}

  get userName(): string {
    const u = this.store.currentUser();
    return u ? (u.firstName ?? '') : 'there';
  }

  get companyName(): string {
    return this.store.currentUser()?.companyInfo?.companyName ?? 'Company';
  }

  stats = [
    { label: 'Total Audits',    value: '6',  change: '+2 this year',    up: true,  color: '#00d4ed', glow: '#00d4ed', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/></svg>` },
    { label: 'In Progress',     value: '2',  change: 'Assigned auditor', up: true,  color: '#f5b800', glow: '#f5b800', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>` },
    { label: 'Completed',       value: '3',  change: 'Reports available', up: true, color: '#00e5a0', glow: '#00e5a0', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>` },
    { label: 'Pending Review',  value: '1',  change: 'Awaiting admin',  up: false, color: '#ff4d6a', glow: '#ff4d6a', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>` },
  ];

  myAudits = [
    { type: 'AI Bias Audit',       auditor: 'Mohamed A.',  status: 'active',    statusLabel: 'In Progress', date: 'Started 5 days ago' },
    { type: 'Data Privacy Review', auditor: 'Sara K.',     status: 'active',    statusLabel: 'In Progress', date: 'Started 12 days ago' },
    { type: 'Compliance Audit',    auditor: 'Ahmed B.',    status: 'completed', statusLabel: 'Completed',   date: '2 months ago' },
    { type: 'Risk Assessment',     auditor: 'Unassigned',  status: 'pending',   statusLabel: 'Pending',     date: 'Requested 2 days ago' },
    { type: 'AI Model Review',     auditor: 'Fatima R.',   status: 'completed', statusLabel: 'Completed',   date: '4 months ago' },
  ];

  compliance = [
    { name: 'GDPR Compliance',      pct: 88, color: '#00e5a0', status: 'Compliant' },
    { name: 'AI Act (EU)',           pct: 64, color: '#f5b800', status: 'In Progress' },
    { name: 'ISO 27001',             pct: 92, color: '#00e5a0', status: 'Compliant' },
    { name: 'Data Bias Standards',   pct: 45, color: '#ff4d6a', status: 'Needs Attention' },
    { name: 'Model Transparency',    pct: 71, color: '#00d4ed', status: 'Partial' },
  ];
}
