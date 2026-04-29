import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, throwError } from 'rxjs';
<<<<<<< HEAD
import { AuthUser, LoginApiResponse, LoginRequest, UserRole } from './auth.models';
=======
import { AuthUser, LoginApiResponse, LoginRequest, LoginResponse, UserRole } from './auth.models';
>>>>>>> origin/gestion_boutique

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
<<<<<<< HEAD
  private readonly apiUrl = '/api/auth';
  private readonly storageKey = 'tinyspring.auth.user';
=======
  private readonly apiUrl = 'http://localhost:8081/api/auth';
  private readonly storageKey = 'tinyspring.auth.user';
  private readonly tokenKey = 'token';
  private readonly userIdKey = 'userId';
  private readonly nomKey = 'nom';
  private readonly emailKey = 'email';
  private readonly roleKey = 'role';
>>>>>>> origin/gestion_boutique

  private readonly currentUserSignal = signal<AuthUser | null>(this.getUserFromStorage());

  readonly currentUser = computed(() => this.currentUserSignal());
<<<<<<< HEAD
  readonly isLoggedIn = computed(() => this.currentUserSignal() !== null);

  login(payload: LoginRequest): Observable<AuthUser> {
=======
  readonly isLoggedIn = computed(() => Boolean(this.currentUserSignal()?.token));

  login(payload: LoginRequest): Observable<LoginResponse> {
>>>>>>> origin/gestion_boutique
    return this.http
      .post<LoginApiResponse>(`${this.apiUrl}/login`, {
        email: payload.email,
        password: payload.password
      })
      .pipe(
<<<<<<< HEAD
        map((response) => {
          const role = response.role as UserRole;

          if (!this.isAllowedRole(role)) {
            throw new Error('Seuls les parents et les animatrices peuvent acceder a cet espace.');
          }

          if (payload.selectedRole !== role) {
            throw new Error('Le role selectionne ne correspond pas a votre compte.');
          }

<<<<<<< HEAD
          if (!response.token) {
=======
          if (!response.accessToken) {
>>>>>>> origin/gestion-evenements
            throw new Error('Le serveur n a pas retourne de token JWT valide.');
          }

          const user = this.buildSessionUser(response, role);
          localStorage.setItem(this.storageKey, JSON.stringify(user));
          this.currentUserSignal.set(user);

          return user;
=======
        map((apiResponse) => {
          const response = this.normalizeLoginResponse(apiResponse);
          const normalizedRole = this.normalizeRole(response.role);

          if (!this.isAllowedRole(normalizedRole)) {
            throw new Error('Seuls les parents et les animatrices peuvent acceder a cet espace.');
          }

          if (this.normalizeRole(payload.selectedRole) !== normalizedRole) {
            throw new Error('Le role selectionne ne correspond pas a votre compte.');
          }

          if (!response.token) {
            throw new Error('Le serveur n a pas retourne de token JWT valide.');
          }

          if (!response.userId) {
            throw new Error('Le serveur n a pas retourne d identifiant utilisateur valide.');
          }

          const user = this.buildSessionUser(response);
          this.persistUser(user);
          this.currentUserSignal.set(user);

          return response;
>>>>>>> origin/gestion_boutique
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
<<<<<<< HEAD
    localStorage.removeItem(this.storageKey);
=======
    this.clearStorage();
>>>>>>> origin/gestion_boutique
    this.currentUserSignal.set(null);
    void this.router.navigate(['/connexion']);
  }

<<<<<<< HEAD
<<<<<<< HEAD
  clearSession(): void {
    localStorage.removeItem(this.storageKey);
    this.currentUserSignal.set(null);
  }

=======
>>>>>>> origin/gestion-evenements
  hasRole(role: UserRole): boolean {
    return this.currentUserSignal()?.role === role;
=======
  hasRole(role: string): boolean {
    return this.normalizeRole(this.currentUserSignal()?.role) === this.normalizeRole(role);
>>>>>>> origin/gestion_boutique
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSignal();
  }

  getToken(): string | null {
<<<<<<< HEAD
    return this.currentUserSignal()?.token ?? null;
  }

  redirectAfterLogin(user: AuthUser): Promise<boolean> {
    return this.router.navigate([this.getHomeRouteForRole(user.role)]);
  }

  getHomeRouteForRole(role: UserRole): string {
    return role === 'PARENT' ? '/parent/tableau-de-bord' : '/animateur/tableau-de-bord';
=======
    return localStorage.getItem(this.tokenKey) || this.currentUserSignal()?.token || null;
  }

  redirectAfterLogin(user: Pick<AuthUser, 'role'> | Pick<LoginResponse, 'role'>): Promise<boolean> {
    return this.router.navigate([this.getHomeRouteForRole(user.role)]);
  }

  getHomeRouteForRole(role: string | null | undefined): string {
    const normalizedRole = this.normalizeRole(role);

    if (normalizedRole === 'PARENT') {
      return '/parent/tableau-de-bord';
    }

    if (normalizedRole === 'ANIMATRICE') {
      return '/animateur/tableau-de-bord';
    }

    return '/connexion';
>>>>>>> origin/gestion_boutique
  }

  private getUserFromStorage(): AuthUser | null {
    const raw = localStorage.getItem(this.storageKey);
<<<<<<< HEAD

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
<<<<<<< HEAD
  const nom = response.nom?.trim() || (role === 'PARENT' ? 'Parent TinySpring' : 'Animatrice TinySpring');

  return {
    id: response.id ? String(response.id) : '',
    nom,
    email: response.email,
    role,
    initiales: this.getInitiales(nom),
    token: response.token,
    tokenType: response.type || 'Bearer',
    expiresIn: response.expiresIn ?? 0,
    isAuthenticated: true
  };
}
=======
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
>>>>>>> origin/gestion-evenements
=======
    const legacyToken = localStorage.getItem(this.tokenKey);
    const legacyUserId = this.parseUserId(localStorage.getItem(this.userIdKey));
    const legacyNom = localStorage.getItem(this.nomKey);
    const legacyEmail = localStorage.getItem(this.emailKey);
    const legacyRole = localStorage.getItem(this.roleKey);

    if (raw) {
      try {
        const user = this.sanitizeStoredUser(JSON.parse(raw) as Partial<AuthUser>);

        if (!user) {
          this.clearStorage();
          return null;
        }

        return user;
      } catch {
        this.clearStorage();
        return null;
      }
    }

    if (!legacyToken || legacyUserId === null || !legacyNom || !legacyEmail || !legacyRole) {
      return null;
    }

    return this.sanitizeStoredUser({
      userId: legacyUserId,
      nom: legacyNom,
      email: legacyEmail,
      role: legacyRole,
      token: legacyToken
    });
  }

  private isAllowedRole(role: string | null): role is Exclude<UserRole, 'ADMIN'> {
    return role === 'PARENT' || role === 'ANIMATRICE';
  }

  private normalizeLoginResponse(response: LoginApiResponse): LoginResponse {
    return {
      message: response.message || 'Connexion reussie',
      userId: this.parseUserId(response.userId ?? response.id) ?? 0,
      nom: response.nom?.trim() || 'Utilisateur TinySpring',
      email: response.email,
      role: response.role,
      token: response.token || response.accessToken || ''
    };
  }

  private buildSessionUser(response: LoginResponse): AuthUser {
    const normalizedRole = this.normalizeRole(response.role);
    const fallbackName = normalizedRole === 'ANIMATRICE' ? 'Animatrice TinySpring' : 'Parent TinySpring';
    const nom = response.nom?.trim() || fallbackName;

    return {
      userId: response.userId,
      nom,
      email: response.email,
      role: response.role,
      token: response.token,
      initiales: this.getInitiales(nom),
    };
  }

  private sanitizeStoredUser(user: Partial<AuthUser>): AuthUser | null {
    const userId = this.parseUserId(user.userId);
    const nom = user.nom?.trim();
    const email = user.email?.trim();
    const role = user.role?.trim();
    const token = user.token?.trim();

    if (userId === null || !nom || !email || !role || !token || !this.normalizeRole(role)) {
      return null;
    }

    return {
      userId,
      nom,
      email,
      role,
      token,
      initiales: user.initiales?.trim() || this.getInitiales(nom)
    };
  }

  private persistUser(user: AuthUser): void {
    localStorage.setItem(this.storageKey, JSON.stringify(user));
    localStorage.setItem(this.tokenKey, user.token);
    localStorage.setItem(this.userIdKey, String(user.userId));
    localStorage.setItem(this.nomKey, user.nom);
    localStorage.setItem(this.emailKey, user.email);
    localStorage.setItem(this.roleKey, user.role);
  }

  private clearStorage(): void {
    localStorage.removeItem(this.storageKey);
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userIdKey);
    localStorage.removeItem(this.nomKey);
    localStorage.removeItem(this.emailKey);
    localStorage.removeItem(this.roleKey);
  }

  private parseUserId(value: unknown): number | null {
    const parsedValue = Number(value);
    return Number.isFinite(parsedValue) && parsedValue > 0 ? parsedValue : null;
  }

  private normalizeRole(role: string | null | undefined): UserRole | null {
    if (!role) {
      return null;
    }

    const normalizedRole = role.toUpperCase().replace(/^ROLE_/, '');
    return normalizedRole === 'PARENT' || normalizedRole === 'ANIMATRICE' || normalizedRole === 'ADMIN'
      ? normalizedRole
      : null;
  }
>>>>>>> origin/gestion_boutique

  private getInitiales(value: string): string {
    return value
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }
}
