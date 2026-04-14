import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  DemandeAffectationRecommendation,
  NouveauTrajetRecommendation
} from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class TransportRecommendationService {
  private readonly apiUrl = 'http://localhost:8081/api/transport/recommandations';

  constructor(private readonly http: HttpClient) {}

  getAffectationRecommendations(): Observable<DemandeAffectationRecommendation[]> {
    return this.http.get<DemandeAffectationRecommendation[]>(`${this.apiUrl}/affectations`);
  }

  getNewRouteRecommendations(): Observable<NouveauTrajetRecommendation[]> {
    return this.http.get<NouveauTrajetRecommendation[]>(`${this.apiUrl}/nouveaux-trajets`);
  }
}
