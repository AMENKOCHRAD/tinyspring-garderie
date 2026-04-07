import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { LoginRequest, LoginResponse, AuthUser, UserRole } from '../shared/auth.models';

/**
 * AuthService
 * Handles authentication with real Spring Boot backend
 * No JWT at this stage - stores minimal auth state in localStorage
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly BACKEND_URL = 'http://localhost:8081/api/auth';
  private readonly STORAGE_KEY = 'auth_user';

  private authSubject = new BehaviorSubject<AuthUser | null>(this.getStoredAuth());
  public auth$ = this.authSubject.asObservable();

  constructor(private http: HttpClient) {
    // Restore auth state from localStorage on service initialization
    const stored = this.getStoredAuth();
    if (stored) {
      this.authSubject.next(stored);
    }
  }

  /**
   * Authenticate user with backend
   * @param email User email
   * @param password User password
   * @returns Observable of login response
   */
  public login(email: string, password: string): Observable<LoginResponse> {
    const request: LoginRequest = { email, password };

    return this.http.post<LoginResponse>(`${this.BACKEND_URL}/login`, request).pipe(
      tap((response: LoginResponse) => {
        // Store auth state in memory and localStorage
        const authUser: AuthUser = {
          email: response.email,
          role: response.role,
          isAuthenticated: true
        };
        this.authSubject.next(authUser);
        this.storeAuth(authUser);
      }),
      catchError((error: HttpErrorResponse) => {
        // Handle specific errors from backend
        const errorMsg = this.getErrorMessage(error);
        return throwError(() => new Error(errorMsg));
      })
    );
  }

  /**
   * Logout user
   * Clears auth state and localStorage
   */
  public logout(): void {
    this.authSubject.next(null);
    this.clearAuth();
  }

  /**
   * Check if user is authenticated
   * @returns true if authenticated, false otherwise
   */
  public isAuthenticated(): boolean {
    return this.authSubject.value?.isAuthenticated ?? false;
  }

  /**
   * Get current authenticated user
   * @returns AuthUser or null
   */
  public getCurrentUser(): AuthUser | null {
    return this.authSubject.value;
  }

  /**
   * Get current user role
   * @returns UserRole or null
   */
  public getCurrentRole(): UserRole | null {
    return this.authSubject.value?.role ?? null;
  }

  /**
   * Check if user has specific role
   * @param role Role to check
   * @returns true if user has the role
   */
  public hasRole(role: UserRole): boolean {
    return this.authSubject.value?.role === role;
  }

  /**
   * Store auth data in localStorage
   * @param authUser Auth data to store
   */
  private storeAuth(authUser: AuthUser): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(authUser));
  }

  /**
   * Retrieve stored auth data from localStorage
   * @returns Stored AuthUser or null
   */
  private getStoredAuth(): AuthUser | null {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored) as AuthUser;
      } catch {
        // Invalid JSON in localStorage, clear it
        this.clearAuth();
        return null;
      }
    }
    return null;
  }

  /**
   * Clear auth data from localStorage
   */
  private clearAuth(): void {
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * Parse HTTP error responses and provide user-friendly messages
   * @param error HttpErrorResponse
   * @returns Error message string
   */
  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 404) {
      return 'Utilisateur introuvable';
    } else if (error.status === 401) {
      return 'Mot de passe incorrect';
    } else if (error.status === 0) {
      return 'Erreur de connexion au serveur. Veuillez vérifier que le backend est disponible sur http://localhost:8081';
    } else {
      return error.error?.message || 'Une erreur est survenue lors de la connexion';
    }
  }
}
