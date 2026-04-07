export interface Categorie {
  id: number;
  nom: string;
  description: string;
  imageUrl: string;
  nombreProduits: number;
}

export interface CategorieRequest {
  nom: string;
  description: string;
  imageUrl: string;
}
