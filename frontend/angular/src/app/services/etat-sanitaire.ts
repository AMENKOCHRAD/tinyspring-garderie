import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EtatSanitaireService {
  private http = inject(HttpClient);

  private apiEnfants = '/api/enfants';
  private apiConditions = '/api/conditions';
  private apiTraitements = '/api/traitements';

  getAllEnfants(): Observable<any[]> {
    return this.http.get<any[]>(this.apiEnfants);
  }

  getConditionsByEnfant(enfantId: number): Observable<any> {
    return this.http.get<any>(`${this.apiConditions}/enfant/${enfantId}`);
  }

  getTraitementsByEnfant(enfantId: number): Observable<any> {
    return this.http.get<any>(`${this.apiTraitements}/enfant/${enfantId}`);
  }
}
