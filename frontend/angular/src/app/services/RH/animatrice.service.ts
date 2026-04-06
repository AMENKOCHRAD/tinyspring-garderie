import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Animatrice, StatutAnimatrice } from '../../RH/animatrice/animatrice.model';

@Injectable({
  providedIn: 'root'
})
export class AnimatriceService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  private getAdminHeaders(): HttpHeaders {
    const credentials = btoa('admin@garderie.com:admin123');
    return new HttpHeaders({
      'Authorization': `Basic ${credentials}`,
      'Content-Type': 'application/json'
    });
  }

  private getAnimatriceHeaders(): HttpHeaders {
    const credentials = btoa('animatrice@garderie.com:anim123');
    return new HttpHeaders({
      'Authorization': `Basic ${credentials}`,
      'Content-Type': 'application/json'
    });
  }

  // ===== ADMIN =====
  getAllAnimatrices(): Observable<Animatrice[]> {
    return this.http.get<Animatrice[]>(`${this.apiUrl}/admin/animatrices`,
      { headers: this.getAdminHeaders() });
  }

  getAnimatriceById(id: number): Observable<Animatrice> {
    return this.http.get<Animatrice>(`${this.apiUrl}/admin/animatrices/${id}`,
      { headers: this.getAdminHeaders() });
  }

  getAnimatricesByStatut(statut: StatutAnimatrice): Observable<Animatrice[]> {
    return this.http.get<Animatrice[]>(`${this.apiUrl}/admin/animatrices/statut/${statut}`,
      { headers: this.getAdminHeaders() });
  }

  createAnimatrice(animatrice: Animatrice): Observable<Animatrice> {
    return this.http.post<Animatrice>(`${this.apiUrl}/admin/animatrices`,
      animatrice, { headers: this.getAdminHeaders() });
  }

  updateAnimatrice(id: number, animatrice: Animatrice): Observable<Animatrice> {
    return this.http.put<Animatrice>(`${this.apiUrl}/admin/animatrices/${id}`,
      animatrice, { headers: this.getAdminHeaders() });
  }

  deleteAnimatrice(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/animatrices/${id}`,
      { headers: this.getAdminHeaders() });
  }

  // ===== ANIMATRICE =====
  getMonProfil(id: number): Observable<Animatrice> {
    return this.http.get<Animatrice>(`${this.apiUrl}/animatrice/profil/${id}`,
      { headers: this.getAnimatriceHeaders() });
  }

  updateMonProfil(id: number, animatrice: Animatrice): Observable<Animatrice> {
    return this.http.put<Animatrice>(`${this.apiUrl}/animatrice/profil/${id}`,
      animatrice, { headers: this.getAnimatriceHeaders() });
  }
}