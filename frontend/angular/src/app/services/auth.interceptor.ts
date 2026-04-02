import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
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
      Authorization: `Basic ${authToken}`
    }
  });

  return next(authReq);
};
