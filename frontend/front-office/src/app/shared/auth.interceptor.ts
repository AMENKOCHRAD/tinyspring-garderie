import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const user = authService.getCurrentUser();

  if (user && user.token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Basic ${user.token}`
      }
    });
    return next(clonedReq);
  }

  return next(req);
};
