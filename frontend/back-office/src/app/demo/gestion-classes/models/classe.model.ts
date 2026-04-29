import { Salle } from './salle.model';

export interface Classe {
  id?: number;
  nom: string;
  niveau: string;
  capaciteMax: number;
  salle?: Salle;
  anneeScolaire: string;
  ageMinimum: number;
  ageMaximum: number;
}
