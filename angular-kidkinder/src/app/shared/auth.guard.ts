import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
<<<<<<< HEAD

  if (authService.isLoggedIn()) {
=======
  const currentUser = authService.getCurrentUser();
  const token = authService.getToken();

  if (currentUser && token) {
>>>>>>> origin/gestion_boutique
    return true;
  }

  return router.createUrlTree(['/connexion']);
};
