import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface QuotaConge {
  id?: number;
  type: string;
  nbJoursMax: number;
  delaiPrevenanceJours: number;
  effectifMinimum: number;
  autoApprobation: boolean;
  joursUtilisesAnneeEnCours?: number;
  joursRestants?: number;
}

@Injectable({ providedIn: 'root' })
export class QuotaService {
  private apiUrl = 'http://localhost:8081/api/admin/quotas';

  constructor(private http: HttpClient) {}

  getAllQuotas(): Observable<QuotaConge[]> {
    return this.http.get<QuotaConge[]>(this.apiUrl);
  }

  updateQuota(type: string, quota: QuotaConge): Observable<QuotaConge> {
    return this.http.put<QuotaConge>(`${this.apiUrl}/${type}`, quota);
  }
}