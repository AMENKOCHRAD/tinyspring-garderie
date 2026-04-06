import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Trajet } from '../models/transport.models';

@Injectable({
  providedIn: 'root'
})
export class TrajetsService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiBaseUrl}/trajets`;

  getAll(): Observable<Trajet[]> {
    return this.http.get<Trajet[]>(this.endpoint);
  }
}
