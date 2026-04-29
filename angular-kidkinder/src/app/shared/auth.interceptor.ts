import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
<<<<<<< HEAD
  const token = authService.getToken();
=======
  const token = localStorage.getItem('token') || authService.getToken();
>>>>>>> origin/gestion_boutique

  if (!token || request.url.includes('/api/auth/login')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
