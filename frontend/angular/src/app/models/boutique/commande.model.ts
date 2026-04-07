export type StatutCommande = 'EN_ATTENTE' | 'CONFIRMEE' | 'EXPEDIEE' | 'LIVREE' | 'ANNULEE';

export interface CommandeProduit {
  id: number;
  nom: string;
  prix: number;
}

export interface Commande {
  id: number;
  dateCommande: string;
  statut: StatutCommande;
  montantTotal: number;
  adresseLivraison: string;
  userId: number;
  userNom: string;
  userEmail: string;
  produits: CommandeProduit[];
}

export interface CommandeRequest {
  adresseLivraison: string;
  userId: number;
  produitIds: number[];
}
