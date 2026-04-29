import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { TransportItem, TransportPayload } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class TransportService {
  private readonly apiUrl = `${environment.apiBaseUrl}/transport/transports`;

  constructor(private readonly http: HttpClient) {}

  getTransports(): Observable<TransportItem[]> {
    return this.http.get<TransportItem[]>(this.apiUrl);
  }

  createTransport(payload: TransportPayload): Observable<TransportItem> {
    return this.http.post<TransportItem>(this.apiUrl, payload);
  }

  updateTransport(id: number, payload: TransportPayload): Observable<TransportItem> {
    return this.http.put<TransportItem>(`${this.apiUrl}/${id}`, payload);
  }

  deleteTransport(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
