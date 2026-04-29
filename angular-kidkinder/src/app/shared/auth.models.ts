<<<<<<< HEAD
export type UserRole = 'PARENT' | 'ANIMATRICE';
=======
export type UserRole = 'PARENT' | 'ANIMATRICE' | 'ADMIN';
>>>>>>> origin/gestion_boutique

export interface LoginRequest {
  email: string;
  password: string;
  selectedRole: UserRole;
}

export interface AuthUser {
<<<<<<< HEAD
  id: string;
  nom: string;
  email: string;
  role: UserRole;
  initiales: string;
  token: string;
  tokenType: string;
  expiresIn: number;
  isAuthenticated: true;
=======
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
>>>>>>> origin/gestion_boutique
}

export interface LoginApiResponse {
  message: string;
<<<<<<< HEAD
  id: number | string;
  nom: string;
  email: string;
  role: UserRole | string;
<<<<<<< HEAD
  token: string;
  type?: string;
  expiresIn?: number;
}
=======
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
>>>>>>> origin/gestion-evenements
=======
  userId?: number;
  id?: number | string;
  nom: string;
  email: string;
  role: string;
  token?: string;
  accessToken?: string;
}
>>>>>>> origin/gestion_boutique
