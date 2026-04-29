export interface PredictionSemaine {
  semaine: number;
  periode_debut: string;
  periode_fin: string;
  quantite_prevue: number;
  confiance: number;
}

export interface Prediction {
  id: number;
  produitId: number;
  produitNom: string;
  stockActuel: number;
  totalPrevu4Semaines: number;
  alerteRupture: boolean;
  dateCalcul: string;
  detailsJson: string;
}
