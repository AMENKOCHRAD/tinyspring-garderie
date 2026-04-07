import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const credentials = localStorage.getItem('basicAuth');

  if (credentials) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Basic ${credentials}`)
    });
    return next(cloned);
  }
  return next(req);
};