import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AffectationTransport } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class AffectationService {
  private readonly apiUrl = 'http://localhost:8082/api/transport/affectations';

  constructor(private readonly http: HttpClient) {}

  getAffectations(): Observable<AffectationTransport[]> {
    return this.http.get<AffectationTransport[]>(this.apiUrl);
  }
}
