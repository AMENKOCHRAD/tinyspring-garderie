import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categorie } from 'src/app/models/boutique/categorie.model';

@Injectable({ providedIn: 'root' })
export class CategorieService {
  private publicUrl = 'http://localhost:8081/api/boutique/categories';
  private adminUrl = 'http://localhost:8081/api/admin/boutique/categories';
  private http = inject(HttpClient);

  getAll(): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(this.publicUrl);
  }

  getAllAdmin(): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(this.adminUrl);
  }

  getById(id: number): Observable<Categorie> {
    return this.http.get<Categorie>(`${this.publicUrl}/${id}`);
  }

  create(formData: FormData): Observable<Categorie> {
    return this.http.post<Categorie>(this.adminUrl, formData);
  }

  update(id: number, formData: FormData): Observable<Categorie> {
    return this.http.put<Categorie>(`${this.adminUrl}/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }
}
