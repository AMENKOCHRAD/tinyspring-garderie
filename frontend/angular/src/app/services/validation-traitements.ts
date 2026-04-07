import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ValidationTraitementsService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/traitements';

  getTraitementsEnAttente(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/en-attente-validation`);
  }

  consulterTraitement(traitementId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/details/${traitementId}`);
  }

  validerTraitement(traitementId: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/valider/${traitementId}`, {});
  }
}