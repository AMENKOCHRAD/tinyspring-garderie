import { Classe } from './classe.model';

export interface Groupe {
  id?: number;
  nom: string;
  capacite: number;
  animatriceId: number;
  classe?: Classe;
  horaireDebut: string;
  horaireFin: string;
  languePrincipale: string;
}
