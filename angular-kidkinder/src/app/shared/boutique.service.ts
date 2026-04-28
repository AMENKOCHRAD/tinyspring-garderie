import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CategorieDto,
  CheckoutSessionResponse,
  CommandeDto,
  CommandeRequest,
  PageResponse,
  ProduitDto,
  ProduitQueryParams
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

  getProduits(params: ProduitQueryParams): Observable<PageResponse<ProduitDto>> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);

    const nom = params.nom?.trim();

    if (nom) {
      httpParams = httpParams.set('nom', nom);
    }

    if (params.categorieId !== null && params.categorieId !== undefined) {
      httpParams = httpParams.set('categorieId', params.categorieId);
    }

    return this.http.get<PageResponse<ProduitDto>>(`${this.apiUrl}/produits`, {
      params: httpParams
    });
  }

  getProduitById(produitId: number): Observable<ProduitDto> {
    return this.http.get<ProduitDto>(`${this.apiUrl}/produits/${produitId}`);
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
