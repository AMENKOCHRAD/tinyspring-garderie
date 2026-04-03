import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data);
  }

  saveSession(response: LoginResponse, credentials: LoginRequest): void {
    localStorage.setItem('user', JSON.stringify(response));
    localStorage.setItem('auth_email', credentials.email);
    localStorage.setItem('auth_password', credentials.password);
  }

  getUser(): LoginResponse | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('user');
  }

  getRole(): string | null {
    const user = this.getUser();
    return user ? user.role : null;
  }

  getEmail(): string | null {
    return localStorage.getItem('auth_email');
  }

  getPassword(): string | null {
    return localStorage.getItem('auth_password');
  }

  getBasicAuthHeaders(): HttpHeaders {
    const email = this.getEmail();
    const password = this.getPassword();

    if (!email || !password) {
      return new HttpHeaders();
    }

    const basicAuth = 'Basic ' + btoa(`${email}:${password}`);

    return new HttpHeaders({
      Authorization: basicAuth
    });
  }

  logout(): void {
    localStorage.removeItem('user');
    localStorage.removeItem('auth_email');
    localStorage.removeItem('auth_password');
  }
}