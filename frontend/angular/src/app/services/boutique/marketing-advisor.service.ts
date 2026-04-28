import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, timeout } from 'rxjs';

import {
  MarketingAnalysis,
  MarketingAutomationRequest,
  MarketingAutomationResponse
} from 'src/app/models/boutique/marketing-advisor.model';

@Injectable({ providedIn: 'root' })
export class MarketingAdvisorService {
  private readonly http = inject(HttpClient);
  private readonly adminUrl = 'http://localhost:8081/api/admin/marketing';

  analyserTous(): Observable<MarketingAnalysis[]> {
    const headers = new HttpHeaders({
      Accept: 'application/json'
    });

    return this.http
      .get<MarketingAnalysis[]>(`${this.adminUrl}/analyser`, {
        headers,
        observe: 'response'
      })
      .pipe(
        timeout(300000),
        map((response) => response.body ?? [])
      );
  }

  analyserProduit(produitId: number): Observable<MarketingAnalysis> {
    return this.http.get<MarketingAnalysis>(`${this.adminUrl}/analyser/${produitId}`);
  }

  automatiserProduit(
    produitId: number,
    payload: MarketingAutomationRequest
  ): Observable<MarketingAutomationResponse> {
    return this.http.post<MarketingAutomationResponse>(`${this.adminUrl}/automatiser/${produitId}`, payload);
  }
}
