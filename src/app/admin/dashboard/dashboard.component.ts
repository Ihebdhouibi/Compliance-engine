import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserStoreService } from '../../shared/user-store.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  constructor(private store: UserStoreService) {}

  get userName(): string {
    const u = this.store.currentUser();
    return u ? (u.firstName ?? '') : 'Admin';
  }

  stats = [
    { label: 'Total Users',     value: '248', change: '+12 this month',  up: true,  color: '#00d4ed', glow: '#00d4ed', iconKey: 'users'     },
    { label: 'Active Auditors', value: '34',  change: '+3 this month',   up: true,  color: '#00e5a0', glow: '#00e5a0', iconKey: 'clipboard' },
    { label: 'Pending Audits',  value: '17',  change: '-4 vs last week', up: false, color: '#f5b800', glow: '#f5b800', iconKey: 'clock'     },
    { label: 'Completed',       value: '193', change: '+28 this month',  up: true,  color: '#18e8ff', glow: '#18e8ff', iconKey: 'check'     },
  ];

  recentAudits = [
    { initials: 'AC', company: 'Acme Corp',      type: 'AI Bias Audit',       status: 'pending',   statusLabel: 'Pending',      date: 'Today'       },
    { initials: 'TF', company: 'TechFlow Ltd',   type: 'Data Privacy Review', status: 'active',    statusLabel: 'In Progress',  date: 'Yesterday'   },
    { initials: 'GH', company: 'GlobalHR Inc',   type: 'Compliance Audit',    status: 'completed', statusLabel: 'Completed',    date: '3 days ago'  },
    { initials: 'NS', company: 'NovaSec',        type: 'Risk Assessment',     status: 'rejected',  statusLabel: 'Rejected',     date: '4 days ago'  },
    { initials: 'FI', company: 'FinServe Group', type: 'AI Model Audit',      status: 'active',    statusLabel: 'In Progress',  date: '5 days ago'  },
  ];

  barData = [
    { label: 'Pending',     value: 17, pct: 28, color: '#f5b800' },
    { label: 'In Progress', value: 24, pct: 40, color: '#00d4ed' },
    { label: 'Completed',   value: 58, pct: 96, color: '#00e5a0' },
    { label: 'Rejected',    value: 8,  pct: 13, color: '#ff4d6a' },
  ];
}