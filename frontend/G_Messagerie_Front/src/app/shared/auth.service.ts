import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, throwError } from 'rxjs';
import { AuthUser, LoginApiResponse, LoginRequest, UserRole } from './auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiUrl = '/api/auth';
  private readonly storageKey = 'tinyspring.auth.user';

  private readonly currentUserSignal = signal<AuthUser | null>(this.getUserFromStorage());

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isLoggedIn = computed(() => this.currentUserSignal() !== null);

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<LoginApiResponse>(`${this.apiUrl}/login`, {
        email: payload.email,
        password: payload.password
      }, {
        withCredentials: true
      })
      .pipe(
        map((response) => {
          const role = this.normalizeRole(response.role);

          if (!role) {
            throw new Error('Role non reconnu.');
          }

          if (role === 'ADMIN') {
            throw new Error('Ce template est reserve uniquement aux parents et aux animatrices.');
          }

          if (payload.selectedRole && payload.selectedRole !== role) {
            throw new Error('Le role selectionne ne correspond pas a votre compte.');
          }

          const user = this.buildSessionUser(response, payload, role);
          localStorage.setItem(this.storageKey, JSON.stringify(user));
          this.currentUserSignal.set(user);

          return user;
        }),
        catchError((error) => {
          const message =
            typeof error?.error === 'string'
              ? error.error
              : error?.error?.message || error?.message || 'Connexion impossible pour le moment.';

          return throwError(() => new Error(message));
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.currentUserSignal.set(null);
    void this.router.navigate(['/connexion']);
  }

  hasRole(role: UserRole): boolean {
    return this.currentUserSignal()?.role === role;
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const currentRole = this.currentUserSignal()?.role;
    return !!currentRole && roles.includes(currentRole);
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSignal();
  }

  getAuthorizationHeader(): string | null {
    const basicAuth = this.currentUserSignal()?.basicAuth;
    return basicAuth ? `Basic ${basicAuth}` : null;
  }

  redirectAfterLogin(user: AuthUser): Promise<boolean> {
    return this.router.navigate([this.getHomeRouteForRole(user.role)]);
  }

  getHomeRouteForRole(role: UserRole): string {
    return role === 'PARENT' ? '/parent/reclamations' : '/animatrice/reclamations';
  }

  private getUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(this.storageKey);

    if (!raw) {
      return null;
    }

    try {
      const user = JSON.parse(raw) as AuthUser;

      if (!user?.basicAuth || !this.normalizeRole(user.role) || user.role === 'ADMIN') {
        localStorage.removeItem(this.storageKey);
        return null;
      }

      return user;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private normalizeRole(role: string): UserRole | null {
    const normalized = role?.replace(/^ROLE_/, '').toUpperCase();

    if (normalized === 'ADMIN' || normalized === 'PARENT' || normalized === 'ANIMATRICE') {
      return normalized;
    }

    return null;
  }

  private buildSessionUser(response: LoginApiResponse, payload: LoginRequest, role: UserRole): AuthUser {
    const fallbackName = role === 'PARENT' ? 'Parent TinySpring' : 'Animatrice TinySpring';
    const nom = response.nom?.trim() || fallbackName;

    return {
      id: String(response.id ?? response.email).toLowerCase(),
      nom,
      email: response.email,
      role,
      initiales: this.getInitiales(nom),
      basicAuth: btoa(`${payload.email}:${payload.password}`),
      isAuthenticated: true
    };
  }

  private getInitiales(value: string): string {
    return value
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }
}
