import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  id: number | string;
  nom: string;
  email: string;
  role: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface StoredUser extends LoginResponse {
  authToken: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly storageKey = 'garderie_auth_user';
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data);
  }

  saveUser(user: LoginResponse): void {
    const authToken = user.accessToken;
    const storedUser: StoredUser = {
      ...user,
      authToken
    };
    localStorage.setItem(this.storageKey, JSON.stringify(storedUser));
  }

  getUser(): StoredUser | null {
    const user = localStorage.getItem(this.storageKey);
    if (!user) {
      return null;
    }

    const parsedUser = JSON.parse(user) as Partial<StoredUser>;
    if (!parsedUser.email || !parsedUser.role || !parsedUser.authToken) {
      return null;
    }

    return parsedUser as StoredUser;
  }

  getAuthToken(): string | null {
    return this.getUser()?.authToken ?? null;
  }

  getRedirectUrlForRole(role: string): string {
    switch (role) {
      case 'ADMIN':
        return `${environment.adminAppUrl}/analytics`;
      case 'PARENT':
        return `${environment.userAppUrl}/connexion`;
      case 'ANIMATRICE':
        return `${environment.userAppUrl}/connexion`;
      default:
        return `${environment.adminAppUrl}/login`;
    }
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
  }
}
