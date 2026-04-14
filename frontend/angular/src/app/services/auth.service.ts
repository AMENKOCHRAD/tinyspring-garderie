import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  nom: string;
  message: string;
  email: string;
  role: string;
  accessToken: string;   // ✅ Token JWT
  tokenType: string;     // ✅ "Bearer"
  expiresIn: number;     // ✅ Durée en ms
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8081/api/auth';
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'user';

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap((response) => {
        // ✅ Sauvegarder le token JWT
        localStorage.setItem(this.TOKEN_KEY, response.accessToken);
        // ✅ Sauvegarder les infos utilisateur
        localStorage.setItem(this.USER_KEY, JSON.stringify({
          id: response.id,
          nom: response.nom,
          email: response.email,
          role: response.role
        }));
      })
    );
  }

  // ✅ Récupérer le token pour l'intercepteur
  getToken(): string {
    return localStorage.getItem(this.TOKEN_KEY) || '';
  }

  getUser(): Partial<LoginResponse> | null {
    const user = localStorage.getItem(this.USER_KEY);
    return user ? JSON.parse(user) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getRole(): string {
    return this.getUser()?.role || '';
  }

  logout(): void {
    // ✅ Nettoyer le token ET les infos user
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }
}