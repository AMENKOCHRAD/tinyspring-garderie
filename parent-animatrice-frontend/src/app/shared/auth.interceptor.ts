import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const isLoginRequest = request.url.includes('/api/auth/login');

  const authorizedRequest =
    !token || isLoginRequest
      ? request
      : request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });

  return next(authorizedRequest).pipe(
    catchError((error) => {
      const status = error?.status;

      if ((status === 401 || status === 403) && token && !isLoginRequest) {
        authService.logout();
      }

      return throwError(() => error);
    })
  );
};
