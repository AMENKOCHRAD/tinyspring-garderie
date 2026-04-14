import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DemandeTransport, TraitementDemandeTransportResponse } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class DemandeService {
  private readonly apiUrl = 'http://localhost:8082/api/transport';

  constructor(private readonly http: HttpClient) {}

  getDemandes(): Observable<DemandeTransport[]> {
    return this.http.get<DemandeTransport[]>(`${this.apiUrl}/demandes`);
  }

  accepterDemande(id: number): Observable<TraitementDemandeTransportResponse> {
    return this.http.put<TraitementDemandeTransportResponse>(`${this.apiUrl}/accepter/${id}`, {});
  }

  refuserDemande(id: number): Observable<TraitementDemandeTransportResponse> {
    return this.http.put<TraitementDemandeTransportResponse>(`${this.apiUrl}/refuser/${id}`, {});
  }

  supprimerDemande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/demandes/${id}`);
  }
}
