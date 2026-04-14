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
  zoneDesservie: string | null;
  latitudeDestination: number | null;
  longitudeDestination: number | null;
  dateTrajet: string;
  heureDepart: string;
  transportId: number | null;
  transportNom: string | null;
  transportMatricule: string | null;
}

export interface TrajetPayload {
  pointDepart: string;
  destination: string;
  zoneDesservie?: string | null;
  latitudeDestination?: number | null;
  longitudeDestination?: number | null;
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
  trajetId: number | null;
  dateDemande?: string;
  pointDepart: string;
  destination: string;
  dateTrajet: string | null;
  heureDepart: string | null;
  statut: StatutTransport;
  pointRamassage: string;
  destinationSouhaitee: string;
  sensTrajet?: 'MAISON_VERS_GARDERIE' | 'GARDERIE_VERS_MAISON';
  adresseMaison?: string;
  latitudeMaison?: number | null;
  longitudeMaison?: number | null;
  adresseGarderie?: string | null;
  dateSouhaitee?: string | null;
  heureSouhaitee?: string | null;
  suspicious?: boolean;
  anomalyScore?: number | null;
  anomalyLevel?: string | null;
  anomalyReasons?: string[];
  duplicateDetected?: boolean;
  aiAnalysisAvailable?: boolean;
  aiModelVersion?: string | null;
  aiAnalysisError?: string | null;
}

export interface AdminDemandPredictionResponse {
  targetDate: string;
  hour: number;
  activeChildrenCount: number;
  availableTransportCount: number;
  avgRouteDistanceKm: number;
  predictedDemandCount: number | null;
  demandLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'UNKNOWN';
  aiAvailable: boolean;
  modelVersion: string | null;
  message: string;
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

export interface DemandeAffectationRecommendation {
  demandeId: number;
  enfantId: number;
  enfantNomComplet: string;
  statutDemande: StatutTransport;
  sensTrajet: 'MAISON_VERS_GARDERIE' | 'GARDERIE_VERS_MAISON';
  zoneRecherchee: string;
  destinationSouhaitee: string;
  affectationAutomatiquePossible: boolean;
  scorePertinence: number;
  distanceEstimeeKm: number | null;
  modeEvaluation: string;
  motifRefus: string | null;
  trajetRecommandeId: number | null;
  trajetRecommandePointDepart: string | null;
  trajetRecommandeDestination: string | null;
  trajetRecommandeDate: string | null;
  trajetRecommandeHeure: string | null;
  transportRecommandeId: number | null;
  transportRecommandeNom: string | null;
  zoneDesservieTrajet: string | null;
}

export interface NouveauTrajetSuggestionDemande {
  demandeId: number;
  enfantId: number;
  enfantNomComplet: string;
  sensTrajet: 'MAISON_VERS_GARDERIE' | 'GARDERIE_VERS_MAISON';
  zoneRecherchee: string;
  destinationSouhaitee: string;
}

export interface NouveauTrajetRecommendation {
  zoneCentrale: string;
  latitudeCentre: number | null;
  longitudeCentre: number | null;
  nombreDemandes: number;
  distanceMoyenneAuTrajetLePlusProcheKm: number | null;
  recommandation: string;
  demandes: NouveauTrajetSuggestionDemande[];
}
