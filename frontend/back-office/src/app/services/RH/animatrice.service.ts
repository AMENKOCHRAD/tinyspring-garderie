import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Animatrice, StatutAnimatrice } from '../../RH/animatrice/animatrice.model';

@Injectable({
  providedIn: 'root'
})
export class AnimatriceService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  // ✅ Supprimé getAdminHeaders() et getAnimatriceHeaders()
  // L'intercepteur JWT ajoute le token automatiquement

  // ===== ADMIN =====
  getAllAnimatrices(): Observable<Animatrice[]> {
    return this.http.get<Animatrice[]>(`${this.apiUrl}/admin/animatrices`);
  }

  getAnimatriceById(id: number): Observable<Animatrice> {
    return this.http.get<Animatrice>(`${this.apiUrl}/admin/animatrices/${id}`);
  }

  getAnimatricesByStatut(statut: StatutAnimatrice): Observable<Animatrice[]> {
    return this.http.get<Animatrice[]>(`${this.apiUrl}/admin/animatrices/statut/${statut}`);
  }

  createAnimatrice(animatrice: Animatrice): Observable<Animatrice> {
    return this.http.post<Animatrice>(`${this.apiUrl}/admin/animatrices`, animatrice);
  }

  updateAnimatrice(id: number, animatrice: Animatrice): Observable<Animatrice> {
    return this.http.put<Animatrice>(`${this.apiUrl}/admin/animatrices/${id}`, animatrice);
  }

  deleteAnimatrice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/animatrices/${id}`);
  }

  // ✅ Upload photo — FormData sans Content-Type
  // (le navigateur le définit automatiquement avec boundary)
  uploadPhoto(id: number, file: File): Observable<Animatrice> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Animatrice>(
      `${this.apiUrl}/admin/animatrices/${id}/upload-photo`,
      formData
      // ✅ Pas de Content-Type manuel — le navigateur gère le multipart boundary
    );
  }

  // ===== ANIMATRICE =====
  getMonProfil(id: number): Observable<Animatrice> {
    return this.http.get<Animatrice>(`${this.apiUrl}/animatrice/profil/${id}`);
  }

  updateMonProfil(id: number, animatrice: Animatrice): Observable<Animatrice> {
    return this.http.put<Animatrice>(`${this.apiUrl}/animatrice/profil/${id}`, animatrice);
  }
}