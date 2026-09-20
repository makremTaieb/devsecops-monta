import { Routes } from '@angular/router';
import { authGuard } from './core/interceptors/auth.guard';

export const routes: Routes = [
  // ── Public routes ──────────────────────────────
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES),
  },

  // ✅ Default route → LOGIN
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },

  // ── Protected routes ──────────────────────────
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/shell.component').then(m => m.ShellComponent),
    loadChildren: () =>
      import('./layout/shell.routes').then(m => m.SHELL_ROUTES),
  },

  // ── Fallback ──────────────────────────────────
  { path: '**', redirectTo: 'auth/login' },
];