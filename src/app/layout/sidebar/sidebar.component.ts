import { Component, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserStoreService } from '../../shared/user-store.service';
import { DesignSettingsService } from '../../services/design-settings.service';

interface NavItem {
  label: string;
  route: string;
  iconKey: string;
  roles: string[];
  exact?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {

  collapsed  = signal(false);
  mobileOpen = signal(false);

  private ds = inject(DesignSettingsService);

  constructor(private store: UserStoreService) {}

  // Logo URL — reactive, updates when settings change
  get logoUrl(): string | null {
    return this.ds.getLogoUrl();
  }

  get projectTitle(): string {
    return this.ds.settings().projectTitle || 'AuditAI';
  }

  private all: NavItem[] = [
    {
      label:   'Dashboard',
      route:   'dashboard',
      iconKey: 'grid',
      roles:   ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_USER'],
      exact:   true
    },
    {
      label:   'Auditors',
      route:   'auditors',
      iconKey: 'users',
      roles:   ['ROLE_ADMIN']
    },
    {
      label:   'Users',
      route:   'users',
      iconKey: 'user',
      roles:   ['ROLE_ADMIN']
    },
    {
      label:   'Audits',
      route:   'audits',
      iconKey: 'clipboard',
      roles:   ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_USER']
    },
    {
      label:   'Assistant IA',
      route:   'chat',
      iconKey: 'robot',
      roles:   ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_USER']
    },
    {
      label:   'Recherche RICS',
      route:   'search',
      iconKey: 'search',
      roles:   ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_USER']
    },
    {
      label:   'Audit Builder',
      route:   'audit-form-builder',
      iconKey: 'builder',
      roles:   ['ROLE_ADMIN']
    },
    {
      label:   'Notifications',
      route:   'notifications',
      iconKey: 'bell',
      roles:   ['ROLE_ADMIN']
    },
    {
      label:   'Settings',
      route:   'settings',
      iconKey: 'settings',
      roles:   ['ROLE_ADMIN']
    },
    {
      label:   'Profile',
      route:   'profile',
      iconKey: 'profile',
      roles:   ['ROLE_ADMIN', 'ROLE_AUDITOR', 'ROLE_USER']
    },
  ];

  navItems = computed(() => {
    const role = this.store.getRole();
    return role ? this.all.filter(i => i.roles.includes(role)) : [];
  });

  get roleBadge(): string {
    const r = this.store.getRole();
    if (r === 'ROLE_ADMIN')   return 'Administrator';
    if (r === 'ROLE_AUDITOR') return 'Auditor';
    return 'Company';
  }

  toggleCollapse(): void { this.collapsed.update(v => !v); }
  toggleMobile(): void {
    if (window.innerWidth > 1024) {
      this.collapsed.update(v => !v);
    } else {
      this.mobileOpen.update(v => !v);
    }
  }
  closeAll():       void { this.mobileOpen.set(false); }
}
