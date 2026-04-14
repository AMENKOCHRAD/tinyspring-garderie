export type DemandeStatus = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';
export type SensTrajet = 'MAISON_VERS_GARDERIE' | 'GARDERIE_VERS_MAISON';

export interface DemandeTransport {
  id: number;
  enfantId: number;
  enfantNomComplet: string;
  parentId: number;
  parentNom: string;
  trajetId: number | null;
  dateDemande: string;
  pointDepart: string;
  destination: string;
  dateTrajet: string | null;
  heureDepart: string | null;
  statut: DemandeStatus;
  pointRamassage: string;
  destinationSouhaitee: string;
  sensTrajet: SensTrajet;
  adresseMaison: string;
  latitudeMaison: number;
  longitudeMaison: number;
  adresseGarderie: string;
}

export interface DemandeTransportPayload {
  enfantId: number;
  sensTrajet: SensTrajet;
  adresseMaison: string;
  latitudeMaison: number;
  longitudeMaison: number;
}

export interface Trajet {
  id: number;
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
}

export interface ParentChild {
  id: number;
  nomComplet: string;
}
