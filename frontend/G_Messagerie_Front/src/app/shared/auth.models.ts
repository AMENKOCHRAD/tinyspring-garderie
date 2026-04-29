export type UserRole = 'ADMIN' | 'PARENT' | 'ANIMATRICE';

export interface LoginRequest {
  email: string;
  password: string;
  selectedRole?: UserRole;
}

export interface AuthUser {
  id: string;
  nom: string;
  email: string;
  role: UserRole;
  initiales: string;
  basicAuth: string;
  isAuthenticated: true;
}

export interface LoginApiResponse {
  message: string;
  id: number | string;
  nom: string;
  email: string;
  role: UserRole | string;
  accessToken?: string;
  tokenType?: string;
  expiresIn?: number;
}
