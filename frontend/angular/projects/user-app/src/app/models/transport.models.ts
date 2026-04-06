export type DemandeStatus = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface DemandeTransport {
  id: number;
  enfantId: number;
  enfantNomComplet: string;
  parentId: number;
  parentNom: string;
  trajetId: number;
  dateDemande: string;
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
  statut: DemandeStatus;
  pointRamassage: string;
}

export interface DemandeTransportPayload {
  enfantId: number;
  trajetId: number;
  pointRamassage: string;
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
