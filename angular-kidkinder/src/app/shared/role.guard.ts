import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
<<<<<<< HEAD
import { UserRole } from './auth.models';
=======
>>>>>>> origin/gestion_boutique

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const expectedRoles =
<<<<<<< HEAD
    (route.data['roles'] as UserRole[] | undefined) ||
    (route.data['role'] ? [route.data['role'] as UserRole] : []);
=======
    (route.data['roles'] as string[] | undefined) ||
    (route.data['role'] ? [route.data['role'] as string] : []);
>>>>>>> origin/gestion_boutique

  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/connexion']);
  }

  if (expectedRoles.length === 0) {
    return true;
  }

  if (expectedRoles.some((role) => authService.hasRole(role))) {
    return true;
  }

  return router.createUrlTree(['/connexion']);
};
