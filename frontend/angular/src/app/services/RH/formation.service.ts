import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Formation, StatutFormation } from '../../RH/formation/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {

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
  getAllFormations(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/admin/formations`,
      { headers: this.getAdminHeaders() });
  }

  getFormationById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/admin/formations/${id}`,
      { headers: this.getAdminHeaders() });
  }

  createFormation(formation: Formation): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/admin/formations`,
      formation, { headers: this.getAdminHeaders() });
  }

  updateFormation(id: number, formation: Formation): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/admin/formations/${id}`,
      formation, { headers: this.getAdminHeaders() });
  }

  updateStatutFormation(id: number, statut: StatutFormation): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/admin/formations/${id}/statut/${statut}`,
      {}, { headers: this.getAdminHeaders() });
  }

  deleteFormation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/formations/${id}`,
      { headers: this.getAdminHeaders() });
  }

  // ===== ANIMATRICE =====
  getFormationsDisponibles(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/animatrice/formations/disponibles`,
      { headers: this.getAnimatriceHeaders() });
  }

  getMesFormations(animatriceId: number): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/animatrice/formations/mes-formations/${animatriceId}`,
      { headers: this.getAnimatriceHeaders() });
  }

  sInscrireFormation(formationId: number, animatriceId: number): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/animatrice/formations/${formationId}/inscrire/${animatriceId}`,
      {}, { headers: this.getAnimatriceHeaders() });
  }
}