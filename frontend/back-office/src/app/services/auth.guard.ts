import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(): boolean {
    const user = this.authService.getUser();
    if (user && user.role === 'ADMIN') {
      return true;
    }

    // Not authenticated or not admin → redirect to login
    this.authService.logout();
    this.router.navigate(['/login']);
    return false;
  }
}
