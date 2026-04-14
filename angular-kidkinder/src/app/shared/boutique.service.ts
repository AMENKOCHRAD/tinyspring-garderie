import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CategorieDto,
  CheckoutSessionResponse,
  CommandeDto,
  CommandeRequest,
  ProduitDto
} from './boutique.models';

@Injectable({
  providedIn: 'root'
})
export class BoutiqueService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/api/boutique';
  private readonly imageBaseUrl = 'http://localhost:8081';

  getCategories(): Observable<CategorieDto[]> {
    return this.http.get<CategorieDto[]>(`${this.apiUrl}/categories`);
  }

  getProduits(): Observable<ProduitDto[]> {
    return this.http.get<ProduitDto[]>(`${this.apiUrl}/produits`);
  }

  getProduitsByCategorie(categorieId: number): Observable<ProduitDto[]> {
    return this.http.get<ProduitDto[]>(`${this.apiUrl}/produits/categorie/${categorieId}`);
  }

  searchProduits(nom: string): Observable<ProduitDto[]> {
    return this.http.get<ProduitDto[]>(`${this.apiUrl}/produits/search`, {
      params: { nom }
    });
  }

  createCommande(payload: CommandeRequest): Observable<CommandeDto> {
    return this.http.post<CommandeDto>(`${this.apiUrl}/commandes`, payload);
  }

  createCheckoutSession(commandeId: number): Observable<CheckoutSessionResponse> {
    return this.http.post<CheckoutSessionResponse>(`${this.apiUrl}/commandes/${commandeId}/checkout-session`, {});
  }

  getMesCommandes(userId: number): Observable<CommandeDto[]> {
    return this.http.get<CommandeDto[]>(`${this.apiUrl}/commandes/user/${userId}`);
  }

  getCommandeById(id: number): Observable<CommandeDto> {
    return this.http.get<CommandeDto>(`${this.apiUrl}/commandes/${id}`);
  }

  getImageUrl(imageUrl: string | null | undefined): string {
    if (!imageUrl) {
      return '';
    }

    return imageUrl.startsWith('http') ? imageUrl : `${this.imageBaseUrl}${imageUrl}`;
  }
}
