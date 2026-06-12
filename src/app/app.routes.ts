import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { guestGuard } from './guards/guest.guard';
import { roleGuard } from './guards/role.guard';
import { AuditWorkspaceComponent } from './admin/audit-workspace/audit-workspace.component';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'sign-up',
        loadComponent: () =>
          import('./auth/sign-up/sign-up.component').then(m => m.SignUpComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./auth/forgot-password/step-email/step-email.component')
            .then(m => m.StepEmailComponent)
      },
      {
        path: 'forgot-password/verify',
        loadComponent: () =>
          import('./auth/forgot-password/step-otp/step-otp.component')
            .then(m => m.StepOtpComponent)
      },
      {
        path: 'forgot-password/reset',
        loadComponent: () =>
          import('./auth/forgot-password/step-reset/step-reset.component')
            .then(m => m.StepResetComponent)
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ROLE_ADMIN'] },
    loadComponent: () =>
      import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./admin/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'auditors',
        loadComponent: () =>
          import('./admin/auditors/auditors.component')
            .then(m => m.AuditorsComponent)
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./admin/users/users.component')
            .then(m => m.UsersComponent)
      },
      {
        path: 'audits',
        loadComponent: () =>
          import('./admin/audits/audits.component')
            .then(m => m.AuditsComponent)
      },
      {
        path: 'audit-form-builder',
        loadComponent: () =>
          import('./admin/audit-form-builder/audit-form-builder.component')
            .then(m => m.AuditFormBuilderComponent)
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./admin/settings/settings.component')
            .then(m => m.SettingsComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./admin/profile/profile.component')
            .then(m => m.ProfileComponent)
      },

      {
  path: 'audits/workspace/:id',
  component: AuditWorkspaceComponent
},

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  {
    path: 'auditor',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ROLE_AUDITOR'] },
    loadComponent: () =>
      import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./auditor/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'audits',
        loadComponent: () =>
          import('./auditor/audits/audits.component')
            .then(m => m.AuditsComponent)
      },
      {
        path: 'audits/:id/scoring',
        loadComponent: () =>
          import('./auditor/audits/auditor-scoring.component')
            .then(m => m.AuditorScoringComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./auditor/profile/profile.component')
            .then(m => m.ProfileComponent)
      },
      {
  path: 'audits/workspace/:id',
  component: AuditWorkspaceComponent
},
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  {
    path: 'company',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ROLE_USER'] },
    loadComponent: () =>
      import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./company/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'audits',
        loadComponent: () =>
          import('./company/audits/audits.component')
            .then(m => m.AuditsComponent)
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./company/profile/profile.component')
            .then(m => m.ProfileComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: 'auth/login' }
];
