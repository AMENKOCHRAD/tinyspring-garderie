export type UserRole = 'PARENT' | 'ANIMATRICE' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
  selectedRole: UserRole;
}

export interface AuthUser {
  userId: number;
  nom: string;
  email: string;
  role: string;
  token: string;
  initiales: string;
}

export interface LoginResponse {
  message: string;
  userId: number;
  nom: string;
  email: string;
  role: string;
  token: string;
}

export interface LoginApiResponse {
  message: string;
  userId?: number;
  id?: number | string;
  nom: string;
  email: string;
  role: string;
  token?: string;
  accessToken?: string;
}
