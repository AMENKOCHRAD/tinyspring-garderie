import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { ProduitDto } from '../shared/boutique.models';
import { AuthService } from '../shared/auth.service';

export type TypeInteractionAffinite =
  | 'VUE_3S'
  | 'VUE_10S'
  | 'VUE_30S'
  | 'CLIC_DETAIL'
  | 'RECHERCHE'
  | 'AJOUT_PANIER'
  | 'COMMANDE'
  | 'ANNULATION';

@Injectable({
  providedIn: 'root'
})
export class AffiniteService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = 'http://localhost:8081/api/boutique';

  getProduitsRecommandes(): Observable<ProduitDto[]> {
    const endpoint = this.authService.getToken() ? 'produits/recommandes' : 'produits';
    return this.http.get<ProduitDto[]>(`${this.apiUrl}/${endpoint}`);
  }

  envoyerInteraction(produitId: number, typeInteraction: TypeInteractionAffinite): Observable<void> {
    if (!this.authService.getToken()) {
      return EMPTY;
    }

    return this.http.post<void>(`${this.apiUrl}/interactions`, {
      produitId,
      typeInteraction
    });
  }
}
