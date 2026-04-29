export type UserRole = 'PARENT' | 'ANIMATRICE';

export interface LoginRequest {
  email: string;
  password: string;
  selectedRole: UserRole;
}

export interface AuthUser {
  id: string;
  nom: string;
  email: string;
  role: UserRole;
  initiales: string;
  token: string;
  tokenType: string;
  expiresIn: number;
  isAuthenticated: true;
}

export interface LoginApiResponse {
  message: string;
  id: number | string;
  nom: string;
  email: string;
  role: UserRole | string;
  token: string;
  type?: string;
  expiresIn?: number;
}