import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { TrajetItem, TrajetPayload } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class TrajetService {
  private readonly apiUrl = `${environment.apiBaseUrl}/transport/trajets`;

  constructor(private readonly http: HttpClient) {}

  getTrajets(): Observable<TrajetItem[]> {
    return this.http.get<TrajetItem[]>(this.apiUrl);
  }

  createTrajet(payload: TrajetPayload): Observable<TrajetItem> {
    return this.http.post<TrajetItem>(this.apiUrl, payload);
  }

  updateTrajet(id: number, payload: TrajetPayload): Observable<TrajetItem> {
    return this.http.put<TrajetItem>(`${this.apiUrl}/${id}`, payload);
  }

  deleteTrajet(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
