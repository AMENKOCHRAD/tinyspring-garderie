export interface Role {
  id?: number;
  name: string;
}

export interface User {
  id: number;
  nom: string;
  email: string;
  enabled: boolean;
  role: Role;
}