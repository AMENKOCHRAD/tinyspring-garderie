export type TypeFormation = 'INTERNE' | 'EXTERNE';
export type StatutFormation = 'INSCRITE' | 'EN_COURS' | 'TERMINEE' | 'ABANDONNEE';

export interface Formation {
  id?: number;
  titre: string;
  description?: string;
  type: TypeFormation;
  dateDebut?: string;
  dateFin?: string;
  formateur?: string;
  placesMax?: number;
  statutInscription?: StatutFormation;
  animatrices?: any[];
}