/**
 * LOGIN REQUEST
 * Payload sent to backend for authentication
 */
export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * LOGIN RESPONSE
 * Response from backend after successful login
 */
export interface LoginResponse {
  message: string;
  email: string;
  role: UserRole;
  token?: string;
}

/**
 * AUTH USER
 * Internal representation of authenticated user
 */
export interface AuthUser {
  email: string;
  role: UserRole;
  isAuthenticated: boolean;
  token?: string;
}

/**
 * USER ROLE
 * Available user roles in the system
 */
export type UserRole = 'PARENT' | 'ANIMATRICE' | 'ADMIN';

/**
 * AUTH ERROR
 * Error response from backend
 */
export interface AuthError {
  status: number;
  message: string;
}
