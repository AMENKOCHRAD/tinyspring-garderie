import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

function ensureAdminAccess(): boolean {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  authService.logout();
  void router.navigate(['/sign-in']);
  return false;
}

export const adminAuthGuard: CanActivateFn = () => ensureAdminAccess();

export const adminChildAuthGuard: CanActivateChildFn = () => ensureAdminAccess();
