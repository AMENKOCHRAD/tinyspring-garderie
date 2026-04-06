import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { DemandeTransport, DemandeTransportPayload } from '../models/transport.models';

@Injectable({
  providedIn: 'root'
})
export class DemandesService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiBaseUrl}/demandes`;

  getMine(): Observable<DemandeTransport[]> {
    return this.http.get<DemandeTransport[]>(this.endpoint);
  }

  create(payload: DemandeTransportPayload): Observable<DemandeTransport> {
    return this.http.post<DemandeTransport>(this.endpoint, payload);
  }

  update(id: number, payload: DemandeTransportPayload): Observable<DemandeTransport> {
    return this.http.put<DemandeTransport>(`${this.endpoint}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
