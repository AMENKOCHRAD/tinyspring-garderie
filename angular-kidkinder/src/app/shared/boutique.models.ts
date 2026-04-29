export interface CategorieDto {
  id: number;
  nom: string;
  description: string;
  imageUrl: string;
  nombreProduits: number;
}

export interface ProduitDto {
  id: number;
  nom: string;
  description: string;
  prix: number;
  stock: number;
  imageUrl: string;
  categorieId: number;
  categorieNom: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ProduitQueryParams {
  page: number;
  size: number;
  nom?: string;
  categorieId?: number | null;
}

export interface CommandeItemRequest {
  produitId: number;
  quantite: number;
}

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
  statut: string;
  paymentStatus: string;
  montantTotal: number;
  adresseLivraison: string;
  stripeSessionId: string;
  stripePaymentIntentId: string;
  userId: number;
  userNom: string;
  userEmail: string;
  items: CommandeItemDto[];
}

export interface CommandeRequest {
  adresseLivraison: string;
  userId: number;
  items: CommandeItemRequest[];
}

export interface CheckoutSessionResponse {
  checkoutUrl: string;
}

export interface CartItem {
  produit: ProduitDto;
  quantite: number;
}
