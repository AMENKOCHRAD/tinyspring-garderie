import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminBoutiqueGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const hasToken = !!authService.getToken();
  const role = authService.getUser()?.role ?? null;

  if (hasToken && authService.isAdmin()) {
    return true;
  }

  console.warn('[AdminBoutiqueGuard] Acces refuse sur une route admin boutique.', {
    url: state.url,
    hasToken,
    role
  });

  return router.createUrlTree(['/login']);
};
