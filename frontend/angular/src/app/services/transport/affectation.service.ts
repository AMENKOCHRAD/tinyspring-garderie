import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { AffectationTransport } from './transport.models';

@Injectable({
  providedIn: 'root'
})
export class AffectationService {
  private readonly apiUrl = `${environment.apiBaseUrl}/transport/affectations`;

  constructor(private readonly http: HttpClient) {}

  getAffectations(): Observable<AffectationTransport[]> {
    return this.http.get<AffectationTransport[]>(this.apiUrl);
  }
}
