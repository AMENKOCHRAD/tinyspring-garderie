import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  userId: number;
  nom: string;
  email: string;
  role: string;
  token: string;
}

export interface AuthUser {
  token: string;
  userId: number;
  nom: string;
  email: string;
  role: string;
}

const AUTH_STORAGE_KEY = 'authUser';
const LEGACY_AUTH_STORAGE_KEY = 'user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data);
  }

  saveUser(user: LoginResponse): void {
    const authUser: AuthUser = {
      token: user.token,
      userId: user.userId,
      nom: user.nom,
      email: user.email,
      role: user.role
    };

    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser));
    localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
    this.clearLegacyCredentials();
  }

  getUser(): AuthUser | null {
    const storedUser = this.readStoredUser(localStorage.getItem(AUTH_STORAGE_KEY));
    if (storedUser) {
      return storedUser;
    }

    const legacyUser = this.readStoredUser(localStorage.getItem(LEGACY_AUTH_STORAGE_KEY));
    if (legacyUser) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(legacyUser));
      localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
      return legacyUser;
    }

    return null;
  }

  getToken(): string | null {
    const token = this.getUser()?.token?.trim();
    return token ? token : null;
  }

  isAdmin(): boolean {
    return this.isAdminRole(this.getUser()?.role);
  }

  isAdminRole(role: string | null | undefined): boolean {
    return this.normalizeRole(role) === 'ADMIN';
  }

  normalizeRole(role: string | null | undefined): string {
    return (role ?? '').replace(/^ROLE_/, '').trim().toUpperCase();
  }

  clearLegacyCredentials(): void {
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userPassword');
  }

  logout(): void {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem(LEGACY_AUTH_STORAGE_KEY);
    this.clearLegacyCredentials();
  }

  private readStoredUser(storedUser: string | null): AuthUser | null {
    if (!storedUser) {
      return null;
    }

    try {
      const parsed = JSON.parse(storedUser) as Partial<AuthUser>;
      if (!parsed || typeof parsed !== 'object') {
        return null;
      }

      return {
        token: typeof parsed.token === 'string' ? parsed.token : '',
        userId: typeof parsed.userId === 'number' ? parsed.userId : Number(parsed.userId ?? 0),
        nom: typeof parsed.nom === 'string' ? parsed.nom : '',
        email: typeof parsed.email === 'string' ? parsed.email : '',
        role: typeof parsed.role === 'string' ? parsed.role : ''
      };
    } catch (error) {
      console.warn('[AuthService] Impossible de lire la session stockee.', error);
      return null;
    }
  }
}
