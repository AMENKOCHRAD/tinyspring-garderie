import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';

/**
 * RoleGuard
 * Protects routes based on user role
 * Expected route data: { roles: ['PARENT', 'ANIMATRICE'] }
 * 
 * ADMIN is NOT allowed in this frontoffice - they have their own backoffice
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    // Check if user is authenticated
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return false;
    }

    // Get required roles from route data
    const requiredRoles: UserRole[] = route.data['roles'] || [];

    // If no roles specified, allow access
    if (requiredRoles.length === 0) {
      return true;
    }

    // Check if user has one of the required roles
    const userRole = this.authService.getCurrentRole();
    if (userRole && requiredRoles.includes(userRole)) {
      return true;
    }

    // User doesn't have required role, redirect based on role
    this.redirectByRole(userRole);
    return false;
  }

  /**
   * Redirect user to appropriate page based on their role
   * - PARENT → parent portal
   * - ANIMATRICE → animator portal
   * - ADMIN → not allowed in this frontoffice, logout and redirect to login
   * @param role User's role
   */
  private redirectByRole(role: UserRole | null): void {
    // ADMIN is not allowed in this frontoffice at all
    if (role === 'ADMIN') {
      this.authService.logout();
      this.router.navigate(['/login']);
      return;
    }

    // For other roles, redirect to their portal
    const redirectMap: Record<string, string> = {
      'PARENT': '/parent/portal',
      'ANIMATRICE': '/animateur/portal'
    };

    if (role && redirectMap[role]) {
      this.router.navigate([redirectMap[role]]);
    } else {
      this.router.navigate(['/']);
    }
  }
}
