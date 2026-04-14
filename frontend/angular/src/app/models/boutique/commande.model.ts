export type StatutCommande = 'PENDING' | 'CONFIRMEE' | 'EXPEDIEE' | 'LIVREE' | 'ANNULEE';

export type PaymentStatusCommande = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELED';

export interface CommandeItemDto {
  produitId: number;
  produitNom: string;
  produitImageUrl: string;
  quantite: number;
  prixUnitaire: number;
  sousTotal: number;
}

export interface CommandeDto {
  id: number;
  dateCommande: string;
  statut: StatutCommande;
  paymentStatus: PaymentStatusCommande;
  montantTotal: number;
  adresseLivraison: string;
  stripeSessionId?: string;
  stripePaymentIntentId?: string;
  userId: number;
  userNom: string;
  userEmail: string;
  items: CommandeItemDto[];
}

export type CommandeItem = CommandeItemDto;
export type Commande = CommandeDto;

export interface CommandeRequest {
  adresseLivraison: string;
  userId: number;
  produitIds: number[];
}
