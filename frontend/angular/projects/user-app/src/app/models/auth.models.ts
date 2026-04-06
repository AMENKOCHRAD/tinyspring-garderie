export type UserRole = 'ADMIN' | 'PARENT' | 'ANIMATRICE';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  email: string;
  role: UserRole;
}

export interface AuthSession extends LoginResponse {
  authToken: string;
}
