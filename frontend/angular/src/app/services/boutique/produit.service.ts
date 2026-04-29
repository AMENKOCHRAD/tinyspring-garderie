import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Produit } from 'src/app/models/boutique/produit.model';
import { SpringPage } from 'src/app/models/boutique/spring-page.model';

export interface AdminProduitPageParams {
  page: number;
  size: number;
  nom?: string;
  categorieId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class ProduitService {
  private publicUrl = 'http://localhost:8081/api/boutique/produits';
  private adminUrl = 'http://localhost:8081/api/admin/boutique/produits';
  private adminFetchAllSize = 1000;
  private http = inject(HttpClient);

  getAll(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.publicUrl);
  }

  getAllAdmin(): Observable<Produit[]> {
    return this.getAdminPage({ page: 0, size: this.adminFetchAllSize }).pipe(
      map((response) => response.content)
    );
  }

  getAdminPage(params: AdminProduitPageParams): Observable<SpringPage<Produit>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size));

    const nom = params.nom?.trim();
    if (nom) {
      httpParams = httpParams.set('nom', nom);
    }

    if (params.categorieId != null) {
      httpParams = httpParams.set('categorieId', String(params.categorieId));
    }

    return this.http.get<SpringPage<Produit>>(this.adminUrl, { params: httpParams });
  }

  getLowStock(): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.adminUrl}/low-stock`);
  }

  getById(id: number): Observable<Produit> {
    return this.http.get<Produit>(`${this.publicUrl}/${id}`);
  }

  getByCategorie(categorieId: number): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.publicUrl}/categorie/${categorieId}`);
  }

  search(nom: string): Observable<Produit[]> {
    return this.http.get<Produit[]>(`${this.publicUrl}/search?nom=${encodeURIComponent(nom)}`);
  }

  create(formData: FormData): Observable<Produit> {
    return this.http.post<Produit>(this.adminUrl, formData);
  }

  update(id: number, formData: FormData): Observable<Produit> {
    return this.http.put<Produit>(`${this.adminUrl}/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${+id}`);
  }
}
