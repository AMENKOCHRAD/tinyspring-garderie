import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ValidationTraitementsService {
  private http = inject(HttpClient);
  private apiUrl = '/api/traitements';

  getTraitementsEnAttente(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/en-attente-validation`);
  }

  consulterTraitement(traitementId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/details/${traitementId}`);
  }

  validerTraitement(traitementId: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/valider/${traitementId}`, {});
  }

  refuserTraitement(traitementId: number, note?: string): Observable<any> {
    const qs = note ? `?note=${encodeURIComponent(note)}` : '';
    return this.http.put<any>(`${this.apiUrl}/refuser/${traitementId}${qs}`, {});
  }

  getValidationHistory(traitementId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${traitementId}/validation-history`);
  }

  getLatestValidationEvents(limit = 200): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/validation-events?limit=${limit}`);
  }
}
