import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Redirects /dashboard to the role-specific dashboard.
 * ADMIN   → /dashboard        (stays, loads DashboardComponent)
 * DEVOPS  → /dashboard-devops
 * DEV     → /dashboard-developpeur
 * AUDITOR → /dashboard        (fallback)
 */
export const dashboardGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  const role   = auth.role;

  if (role === 'DEVOPS')  { router.navigate(['/dashboard-devops']);       return false; }
  if (role === 'DEV')     { router.navigate(['/dashboard-developpeur']);  return false; }
  return true; // ADMIN / AUDITOR stay on /dashboard
};
