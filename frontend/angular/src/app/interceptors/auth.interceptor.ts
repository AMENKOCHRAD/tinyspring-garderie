<<<<<<< HEAD
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('accessToken');

    if (token) {
      const cloned = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next.handle(cloned);
    }

    return next.handle(req);
  }
}
=======
import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (req.url.includes('/api/admin/')) {
    const authUser = authService.getUser();
    const token = authUser?.token?.trim() || null;

    console.log('[AuthInterceptor] token trouve:', !!token, '| URL:', req.url);

    if (!token) {
      console.warn('[AuthInterceptor] Aucun JWT disponible pour la requete admin.', req.url);
      return next(req);
    }

    if (!authService.isAdminRole(authUser?.role)) {
      console.warn('[AuthInterceptor] Session non ADMIN sur une requete admin.', {
        url: req.url,
        role: authUser?.role ?? null
      });
    }

    if (!req.headers.has('Authorization')) {
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      return next(cloned);
    }
  }
  return next(req);
};
>>>>>>> origin/gestion_boutique
