import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { UserRole } from '../../models/auth.models';
import { AuthService } from '../services/auth.service';

export const sessionGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.getSession() ? true : router.parseUrl('/login');
};

export const guestOnlyGuard: CanActivateFn = () => {
  const authService = inject(AuthService);

  if (authService.getSession()) {
    authService.redirectToRoleHome();
    return false;
  }

  return true;
};

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const currentRole = authService.getRole();
  const roles = (route.data?.['roles'] as UserRole[] | undefined) ?? [];

  if (currentRole && roles.includes(currentRole)) {
    return true;
  }

  if (currentRole) {
    authService.redirectToRoleHome(currentRole);
    return false;
  }

  return router.parseUrl('/login');
};
