<<<<<<< HEAD:frontend/back-office/src/app/services/auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
=======
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';

>>>>>>> origin/gestion-transports:frontend/angular/src/app/services/auth.interceptor.ts
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
<<<<<<< HEAD:frontend/back-office/src/app/services/auth.interceptor.ts
  const user = authService.getUser();

  if (user && user.token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Basic ${user.token}`
      }
    });
    return next(clonedReq);
  }

  return next(req);
=======
  const router = inject(Router);
  const authToken = authService.getAuthToken();

  if (!authToken || req.url.includes('/api/auth/login')) {
    if (!req.url.includes('/api/transport')) {
      return next(req);
    }

    authService.logout();
    router.navigate(['/sign-in']);

    return throwError(
      () =>
        new HttpErrorResponse({
          status: 401,
          statusText: 'Unauthorized',
          error: {
            details: ['Session admin invalide ou expiree. Merci de vous reconnecter.']
          }
        })
    );
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${authToken}`
    }
  });

  return next(authReq);
>>>>>>> origin/gestion-transports:frontend/angular/src/app/services/auth.interceptor.ts
};
