import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DemandeTransport, TraitementDemandeTransportResponse } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class DemandeService {
  private readonly apiUrl = 'http://localhost:8081/api/transport';

  constructor(private readonly http: HttpClient) {}

  getDemandes(): Observable<DemandeTransport[]> {
    return this.http.get<DemandeTransport[]>(`${this.apiUrl}/demandes`);
  }

  accepterDemande(id: number, transportId: number): Observable<TraitementDemandeTransportResponse> {
    return this.http.put<TraitementDemandeTransportResponse>(`${this.apiUrl}/accepter/${id}`, { transportId });
  }

  refuserDemande(id: number): Observable<TraitementDemandeTransportResponse> {
    return this.http.put<TraitementDemandeTransportResponse>(`${this.apiUrl}/refuser/${id}`, {});
  }
}
