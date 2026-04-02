export type StatutTransport = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface TransportItem {
  id: number;
  nom: string;
  matricule: string;
  capacite: number;
  tauxRemplissage: number;
}

export interface TransportPayload {
  nom: string;
  matricule: string;
  capacite: number;
}

export interface TrajetItem {
  id: number;
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
  transportId: number;
  transportNom: string;
  transportMatricule: string;
}

export interface TrajetPayload {
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
  transportId: number;
}

export interface DemandeTransport {
  id: number;
  enfantId: number;
  enfantNomComplet: string;
  parentId: number;
  parentNom: string;
  trajetId: number;
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
  statut: StatutTransport;
  pointRamassage: string;
}

export interface TraitementDemandeTransportResponse {
  demandeId: number;
  statut: StatutTransport;
  affectationId: number | null;
  tauxRemplissageTransport: number | null;
}

export interface AffectationTransport {
  id: number;
  enfantId: number;
  enfantNomComplet: string;
  transportId: number;
  transportNom: string;
  matriculeTransport: string;
  trajetId: number;
  pointDepart: string;
  destination: string;
  dateTrajet: string;
  heureDepart: string;
  pointRamassage: string;
}
