import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import {
  AdminDemandPredictionResponse,
  DemandeAffectationRecommendation,
  NouveauTrajetRecommendation
} from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class TransportRecommendationService {
  private readonly apiUrl = `${environment.apiBaseUrl}/transport/recommandations`;

  constructor(private readonly http: HttpClient) {}

  getAffectationRecommendations(): Observable<DemandeAffectationRecommendation[]> {
    return this.http.get<DemandeAffectationRecommendation[]>(`${this.apiUrl}/affectations`);
  }

  getNewRouteRecommendations(): Observable<NouveauTrajetRecommendation[]> {
    return this.http.get<NouveauTrajetRecommendation[]>(`${this.apiUrl}/nouveaux-trajets`);
  }

  getDemandPrediction(
    targetDate: string,
    hour: number,
    rainFlag = false,
    schoolBreakFlag = false
  ): Observable<AdminDemandPredictionResponse> {
    return this.http.get<AdminDemandPredictionResponse>(`${this.apiUrl}/prediction-demande`, {
      params: {
        targetDate,
        hour,
        rainFlag,
        schoolBreakFlag
      }
    });
  }
}
