import { Routes } from '@angular/router';
import { roleGuard }      from '../core/guards/role.guard';
import { dashboardGuard } from '../core/guards/dashboard.guard';

export const SHELL_ROUTES: Routes = [

  // ── /dashboard → role-based redirect ──────────────────────
  {
    path: 'dashboard',
    canActivate: [dashboardGuard],
    loadComponent: () =>
      import('../features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },

  // ── Dashboard DevOps (DEVOPS + ADMIN) ──────────────────────
  {
    path: 'dashboard-devops',
    canActivate: [roleGuard],
    data: { roles: ['DEVOPS', 'ADMIN'] },
    loadComponent: () =>
      import('../features/dashboard-devops/dashboard-devops.component').then(m => m.DashboardDevopsComponent),
  },

  // ── Dashboard Développeur (DEV + ADMIN) ───────────────────
  {
    path: 'dashboard-developpeur',
    canActivate: [roleGuard],
    data: { roles: ['DEV', 'ADMIN'] },
    loadComponent: () =>
      import('../features/dashboard-dev/dashboard-dev.component').then(m => m.DashboardDevComponent),
  },

  // ── Projects (all authenticated roles) ────────────────────
  {
    path: 'projects',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEV', 'DEVOPS'] },
    loadComponent: () =>
      import('../features/projects/projects.component').then(m => m.ProjectsComponent),
  },

  // ── Pipelines (ADMIN + DEVOPS) ────────────────────────────
  {
    path: 'pipelines',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
    loadComponent: () =>
      import('../features/pipelines/pipelines.component').then(m => m.PipelinesComponent),
  },

  // ── Security (ADMIN + DEVOPS) ─────────────────────────────
  {
    path: 'security',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
    loadComponent: () =>
      import('../features/security/security.component').then(m => m.SecurityComponent),
  },

  // ── Monitoring (ADMIN + DEVOPS) ───────────────────────────
  {
    path: 'monitoring',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
    loadComponent: () =>
      import('../features/monitoring/monitoring.component').then(m => m.MonitoringComponent),
  },

  // ── Notifications (all roles) ─────────────────────────────
  { path: 'notifications',
    loadComponent: () =>
      import('../features/notifications/notifications.component').then(m => m.NotificationsComponent),
  },

  // ── Audit Logs (ADMIN + DEVOPS) ───────────────────────────
  {
    path: 'audit-logs',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
    loadComponent: () =>
      import('../features/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent),
  },

  // ── Admin panel (ADMIN only) ──────────────────────────────
  {
    path: 'admin',
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('../features/admin/admin.component').then(m => m.AdminComponent),
  },

  // ── Default: redirect to /dashboard (guard handles role split) ─
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
