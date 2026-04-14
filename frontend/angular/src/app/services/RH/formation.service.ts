import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Formation, StatutFormation } from '../../RH/formation/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  // ✅ Supprimé getAdminHeaders() et getAnimatriceHeaders()
  // L'intercepteur JWT ajoute le token automatiquement

  // ===== ADMIN =====
  getAllFormations(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/admin/formations`);
  }

  getFormationById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/admin/formations/${id}`);
  }

  createFormation(formation: Formation): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/admin/formations`, formation);
  }

  updateFormation(id: number, formation: Formation): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/admin/formations/${id}`, formation);
  }

  updateStatutFormation(id: number, statut: StatutFormation): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/admin/formations/${id}/statut/${statut}`, {});
  }

  deleteFormation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/formations/${id}`);
  }

  // ===== ANIMATRICE =====
  getFormationsDisponibles(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/animatrice/formations/disponibles`);
  }

  getMesFormations(animatriceId: number): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.apiUrl}/animatrice/formations/mes-formations/${animatriceId}`);
  }

  sInscrireFormation(formationId: number, animatriceId: number): Observable<Formation> {
    return this.http.post<Formation>(
      `${this.apiUrl}/animatrice/formations/${formationId}/inscrire/${animatriceId}`,
      {}
    );
  }
}