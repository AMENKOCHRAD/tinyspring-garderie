import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AbsenceConge, StatutAbsenceConge } from '../../RH/absence-conge/absence-conge.model';

@Injectable({
  providedIn: 'root'
})
export class AbsenceCongeService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  // ✅ Supprimé getAdminHeaders() et getAnimatriceHeaders()
  // L'intercepteur JWT ajoute le token automatiquement

  // ===== ADMIN =====
  getAllAbsenceConges(): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/admin/absences-conges`);
  }

  getByStatut(statut: StatutAbsenceConge): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/admin/absences-conges/statut/${statut}`);
  }

  validerDemande(id: number): Observable<AbsenceConge> {
    return this.http.put<AbsenceConge>(`${this.apiUrl}/admin/absences-conges/${id}/valider`, {});
  }

  refuserDemande(id: number): Observable<AbsenceConge> {
    return this.http.put<AbsenceConge>(`${this.apiUrl}/admin/absences-conges/${id}/refuser`, {});
  }

  deleteAbsenceConge(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/absences-conges/${id}`);
  }

  // ===== ANIMATRICE =====
  getMesAbsenceConges(animatriceId: number): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/animatrice/absences-conges/${animatriceId}`);
  }

  soumettreDemande(absenceConge: AbsenceConge): Observable<AbsenceConge> {
    return this.http.post<AbsenceConge>(`${this.apiUrl}/animatrice/absences-conges`, absenceConge);
  }
}