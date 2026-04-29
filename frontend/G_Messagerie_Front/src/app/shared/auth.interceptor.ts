import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const authorization = authService.getAuthorizationHeader();

  if (!request.url.startsWith('/api')) {
    return next(request);
  }

  const secureRequest = request.clone({
    withCredentials: true,
    ...(authorization && !request.url.includes('/api/auth/login')
      ? {
          setHeaders: {
            Authorization: authorization
          }
        }
      : {})
  });

  return next(secureRequest);
};
