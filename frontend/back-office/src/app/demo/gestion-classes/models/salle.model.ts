export interface Salle {
  id?: number;
  nom: string;
  surface: number;
  type: string;
  climatise?: boolean;
  equipements?: string;
  disponible?: boolean;
}
