import { Groupe } from './groupe.model';

export interface Affectation {
  id?: number;
  enfantId: number;
  dateDebut: string;
  dateFin: string;
  groupe?: Groupe;
  statut: string;
}
