import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  email: string;
  role: string;
}

export interface StoredUser extends LoginResponse {
  authToken: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data);
  }

  saveUser(user: LoginResponse, credentials: LoginRequest): void {
    const authToken = btoa(`${credentials.email}:${credentials.password}`);
    const storedUser: StoredUser = {
      ...user,
      authToken
    };
    localStorage.setItem('user', JSON.stringify(storedUser));
  }

  getUser(): StoredUser | null {
    const user = localStorage.getItem('user');
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

  logout(): void {
    localStorage.removeItem('user');
  }
}
