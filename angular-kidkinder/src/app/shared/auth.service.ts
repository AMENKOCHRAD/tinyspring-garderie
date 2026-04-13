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
      })
      .pipe(
        map((response) => {
          const role = response.role as UserRole;

          if (!this.isAllowedRole(role)) {
            throw new Error('Seuls les parents et les animatrices peuvent acceder a cet espace.');
          }

          if (payload.selectedRole !== role) {
            throw new Error('Le role selectionne ne correspond pas a votre compte.');
          }

          if (!response.accessToken) {
            throw new Error('Le serveur n a pas retourne de token JWT valide.');
          }

          const user = this.buildSessionUser(response, role);
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

  getCurrentUser(): AuthUser | null {
    return this.currentUserSignal();
  }

  getToken(): string | null {
    return this.currentUserSignal()?.token ?? null;
  }

  redirectAfterLogin(user: AuthUser): Promise<boolean> {
    return this.router.navigate([this.getHomeRouteForRole(user.role)]);
  }

  getHomeRouteForRole(role: UserRole): string {
    return role === 'PARENT' ? '/parent/tableau-de-bord' : '/animateur/tableau-de-bord';
  }

  private getUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(this.storageKey);

    if (!raw) {
      return null;
    }

    try {
      const user = JSON.parse(raw) as AuthUser;

      if (!user?.token || !this.isAllowedRole(user.role)) {
        localStorage.removeItem(this.storageKey);
        return null;
      }

      return user;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private isAllowedRole(role: string): role is UserRole {
    return role === 'PARENT' || role === 'ANIMATRICE';
  }

  private buildSessionUser(response: LoginApiResponse, role: UserRole): AuthUser {
    const nom = response.nom?.trim() || (role === 'PARENT' ? 'Parent TinySpring' : 'Animatrice TinySpring');

    return {
      id: String(response.id ?? response.email).toLowerCase(),
      nom,
      email: response.email,
      role,
      initiales: this.getInitiales(nom),
      token: response.accessToken,
      tokenType: response.tokenType || 'Bearer',
      expiresIn: response.expiresIn ?? 0,
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
