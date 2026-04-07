import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.includes('/api/admin/')) {
    const email = localStorage.getItem('userEmail');
    const password = localStorage.getItem('userPassword');
    if (email && password) {
      const token = btoa(`${email}:${password}`);
      const cloned = req.clone({
        setHeaders: { Authorization: `Basic ${token}` }
      });
      return next(cloned);
    }
  }
  return next(req);
};
