import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AbsenceConge, StatutAbsenceConge } from '../../RH/absence-conge/absence-conge.model';

@Injectable({
  providedIn: 'root'
})
export class AbsenceCongeService {

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
  getAllAbsenceConges(): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/admin/absences-conges`,
      { headers: this.getAdminHeaders() });
  }

  getByStatut(statut: StatutAbsenceConge): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/admin/absences-conges/statut/${statut}`,
      { headers: this.getAdminHeaders() });
  }

  validerDemande(id: number): Observable<AbsenceConge> {
    return this.http.put<AbsenceConge>(`${this.apiUrl}/admin/absences-conges/${id}/valider`,
      {}, { headers: this.getAdminHeaders() });
  }

  refuserDemande(id: number): Observable<AbsenceConge> {
    return this.http.put<AbsenceConge>(`${this.apiUrl}/admin/absences-conges/${id}/refuser`,
      {}, { headers: this.getAdminHeaders() });
  }

  deleteAbsenceConge(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/absences-conges/${id}`,
      { headers: this.getAdminHeaders() });
  }

  // ===== ANIMATRICE =====
  getMesAbsenceConges(animatriceId: number): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(`${this.apiUrl}/animatrice/absences-conges/${animatriceId}`,
      { headers: this.getAnimatriceHeaders() });
  }

  soumettreDemande(absenceConge: AbsenceConge): Observable<AbsenceConge> {
    return this.http.post<AbsenceConge>(`${this.apiUrl}/animatrice/absences-conges`,
      absenceConge, { headers: this.getAnimatriceHeaders() });
  }
}