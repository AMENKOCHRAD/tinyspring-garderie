export type StatutAnimatrice = 'ACTIVE' | 'INACTIVE';

export interface Animatrice {
  id?: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  dateEmbauche?: string;
  statut?: StatutAnimatrice;
  specialite?: string;
  photoUrl?: string;
}