export interface DashboardRecentCommande {
  id: number;
  dateCommande: string;
  statut: string;
  montantTotal: number;
  userNom: string;
  userEmail: string;
}

export interface DashboardTopProduit {
  id: number;
  nom: string;
  imageUrl: string | null;
  totalCommandes: number;
}

export interface DashboardMonthlySales {
  mois: string;
  montant: number;
}

export interface DashboardStats {
  totalProduits: number;
  produitsEnStock: number;
  produitsRupture: number;
  produitsStockFaible: number;
  totalCategories: number;
  totalCommandes: number;
  commandesPendingPayment: number;
  commandesConfirmees: number;
  commandesExpediees: number;
  commandesLivrees: number;
  commandesAnnulees: number;
  montantTotalVentes: number;
  dernieresCommandes: DashboardRecentCommande[];
  topProduits: DashboardTopProduit[];
  ventesParMois: DashboardMonthlySales[];
}
