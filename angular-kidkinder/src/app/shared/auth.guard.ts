import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const currentUser = authService.getCurrentUser();
  const token = authService.getToken();

  if (currentUser && token) {
    return true;
  }

  return router.createUrlTree(['/connexion']);
};
