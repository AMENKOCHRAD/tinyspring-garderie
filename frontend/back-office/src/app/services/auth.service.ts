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
<<<<<<< HEAD:frontend/back-office/src/app/services/auth.service.ts
  token?: string;
  id?: number;
  nom?: string;
=======
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface StoredUser extends LoginResponse {
  authToken: string;
>>>>>>> origin/gestion-transports:frontend/angular/src/app/services/auth.service.ts
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
<<<<<<< HEAD:frontend/back-office/src/app/services/auth.service.ts
  private apiUrl = '/api/auth';
=======
  private readonly storageKey = 'garderie_auth_user';
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;
>>>>>>> origin/gestion-transports:frontend/angular/src/app/services/auth.service.ts

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data);
  }

  saveUser(user: LoginResponse): void {
<<<<<<< HEAD:frontend/back-office/src/app/services/auth.service.ts
    localStorage.setItem('user', JSON.stringify(user));
    if (user?.token) {
      localStorage.setItem('token', user.token);
    }
  }

  getUser(): LoginResponse | null {
    const user = localStorage.getItem('user');
    return user ? (JSON.parse(user) as LoginResponse) : null;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  logout(): void {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('basicAuth');
  }
}

=======
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
>>>>>>> origin/gestion-transports:frontend/angular/src/app/services/auth.service.ts
