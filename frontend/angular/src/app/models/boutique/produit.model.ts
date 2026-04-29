export interface Produit {
  id: number;
  nom: string;
  description: string;
  prix: number;
  stock: number;
  imageUrl: string;
  categorieId: number;
  categorieNom: string;
  seuilAlerte: number;
}

export interface ProduitRequest {
  nom: string;
  description: string;
  prix: number;
  stock: number;
  imageUrl: string;
  categorieId: number;
}
