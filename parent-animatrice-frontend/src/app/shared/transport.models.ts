export type DemandeStatus = 'EN_ATTENTE' | 'REVISION_PARENT_DEMANDEE' | 'ACCEPTEE' | 'REFUSEE';
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
  dateSouhaitee: string;
  heureSouhaitee: string;
  suspicious: boolean;
  anomalyScore: number | null;
  anomalyLevel: string | null;
  anomalyReasons: string[];
  duplicateDetected: boolean;
  aiAnalysisAvailable: boolean;
  aiModelVersion: string | null;
  aiAnalysisError: string | null;
  revisionRequestMessage: string | null;
  revisionRequestedAt: string | null;
}

export interface DemandeTransportPayload {
  enfantId: number;
  sensTrajet: SensTrajet;
  adresseMaison: string;
  latitudeMaison: number;
  longitudeMaison: number;
  dateSouhaitee: string;
  heureSouhaitee: string;
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
