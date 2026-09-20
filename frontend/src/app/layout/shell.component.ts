import { Component, signal, inject, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AuthService } from '../core/services/auth.service';

interface NavItem { path: string; label: string; icon: string; roles?: string[]; }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent {

  private router    = inject(Router);
  private sanitizer = inject(DomSanitizer);
  public  auth      = inject(AuthService);

  // ── Role-specific nav menus ─────────────────────────────────
  private readonly adminNav: NavItem[] = [
    { path: '/dashboard',     label: 'Dashboard',        icon: 'grid' },
    { path: '/projects',      label: 'Projets',           icon: 'folder' },
    { path: '/notifications', label: 'Notifications',     icon: 'bell' },
    { path: '/admin',         label: 'Utilisateurs',      icon: 'users' },
    { path: '/audit-logs',    label: 'Audit Logs',        icon: 'audit' },
    { path: '/monitoring',    label: 'Monitoring',        icon: 'monitor' },
  ];

  private readonly devopsNav: NavItem[] = [
    { path: '/dashboard-devops',        label: 'Dashboard',  icon: 'grid' },
    { path: '/projects',      label: 'Projets',           icon: 'folder' },
    { path: '/notifications', label: 'Notifications',     icon: 'bell' },
    { path: '/security',      label: 'Sécurité',          icon: 'shield' },
    { path: '/audit-logs',    label: 'Logs',              icon: 'audit' },
    { path: '/monitoring',    label: 'Monitoring',        icon: 'monitor' },
  ];

  private readonly devNav: NavItem[] = [
    { path: '/dashboard-developpeur',  label: 'Dashboard',  icon: 'grid' },
    { path: '/projects',      label: 'Projets',           icon: 'folder' },
    { path: '/notifications', label: 'Notifications',     icon: 'bell' },
  ];

  readonly navItems = computed(() => {
    switch (this.auth.role) {
      case 'ADMIN':   return this.adminNav;
      case 'DEVOPS':  return this.devopsNav;
      case 'DEV':     return this.devNav;
      default:        return this.devNav;
    }
  });

  sidebarOpen = signal(true);

  private routeTitle$ = this.router.events.pipe(
    filter(e => e instanceof NavigationEnd),
    map(() => {
      const seg = this.router.url.split('/').pop() ?? '';
      return ({
        dashboard: 'Tableau de bord', 'dashboard-devops': 'Dashboard DevOps', 'dashboard-developpeur': 'Dashboard Développeur', projects: 'Projets', pipelines: 'Pipelines',
        security: 'Sécurité', monitoring: 'Monitoring', notifications: 'Notifications',
        'audit-logs': 'Audit Logs', admin: 'Administration',
      } as Record<string, string>)[seg] ?? 'DevSecOps STB';
    })
  );
  readonly pageTitle = toSignal(this.routeTitle$, { initialValue: 'Tableau de bord' });

  toggleSidebar() { this.sidebarOpen.update(v => !v); }

  get initials(): string {
    return (this.auth.currentUser?.()?.username || '?').slice(0, 2).toUpperCase();
  }

  roleIcon(): string {
    return ({ ADMIN:'👑', DEV:'💻', DEVOPS:'🚀', AUDITOR:'🔍' })[this.auth.role] ?? '👤';
  }

  roleLabel(): string {
    return ({ ADMIN:'Administrateur', DEV:'Développeur', DEVOPS:'DevOps Engineer', AUDITOR:'Auditeur' })[this.auth.role] ?? this.auth.role;
  }

  getIcon(name: string): SafeHtml {
    const icons: Record<string, string> = {
      grid:     `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`,
      folder:   `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>`,
      activity: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
      shield:   `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
      bell:     `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>`,
      users:    `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
      monitor:  `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>`,
      audit:    `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>`,
      rocket:   `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09z"/><path d="m12 15-3-3a22 22 0 0 1 2-3.95A12.88 12.88 0 0 1 22 2c0 2.72-.78 7.5-6 11a22.35 22.35 0 0 1-4 2z"/></svg>`,
    };
    return this.sanitizer.bypassSecurityTrustHtml(icons[name] || '');
  }
}
