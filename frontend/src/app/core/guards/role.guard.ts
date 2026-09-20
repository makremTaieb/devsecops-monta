import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  const allowed: string[] = route.data['roles'] ?? [];
  const role = auth.role;
  if (!role) { router.navigate(['/auth/login']); return false; }
  if (allowed.length === 0 || allowed.includes(role)) return true;
  router.navigate(['/dashboard']);
  return false;
};
