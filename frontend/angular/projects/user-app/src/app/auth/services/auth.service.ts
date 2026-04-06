import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthSession, LoginRequest, LoginResponse, UserRole } from '../../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storageKey = 'garderie_auth_user';
  private readonly authUrl = `${environment.apiBaseUrl}/auth`;

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, credentials);
  }

  saveSession(response: LoginResponse, credentials: LoginRequest): void {
    const authToken = btoa(`${credentials.email}:${credentials.password}`);
    const session: AuthSession = { ...response, authToken };
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }

  getSession(): AuthSession | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      const session = JSON.parse(raw) as Partial<AuthSession>;
      if (!session.email || !session.role || !session.authToken) {
        return null;
      }

      return session as AuthSession;
    } catch {
      return null;
    }
  }

  getRole(): UserRole | null {
    return this.getSession()?.role ?? null;
  }

  getAuthToken(): string | null {
    return this.getSession()?.authToken ?? null;
  }

  logout(redirectToLogin = true): void {
    localStorage.removeItem(this.storageKey);
    if (redirectToLogin) {
      this.router.navigateByUrl('/login');
    }
  }

  redirectToRoleHome(role = this.getRole()): void {
    switch (role) {
      case 'ADMIN':
        window.location.href = `${environment.adminAppUrl}/analytics`;
        break;
      case 'PARENT':
        this.router.navigateByUrl('/parent');
        break;
      case 'ANIMATRICE':
        this.router.navigateByUrl('/animatrice');
        break;
      default:
        this.router.navigateByUrl('/login');
    }
  }
}
