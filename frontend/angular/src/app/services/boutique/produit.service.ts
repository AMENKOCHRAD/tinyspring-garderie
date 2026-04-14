import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Produit } from 'src/app/models/boutique/produit.model';

@Injectable({ providedIn: 'root' })
export class ProduitService {
  private publicUrl = 'http://localhost:8081/api/boutique/produits';
  private adminUrl = 'http://localhost:8081/api/admin/boutique/produits';
  private http = inject(HttpClient);

  getAll(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.publicUrl);
  }

  getAllAdmin(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.adminUrl);
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
