import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { ParentChild } from '../models/transport.models';

@Injectable({
  providedIn: 'root'
})
export class ChildrenService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiBaseUrl}/parent/enfants`;

  getMine(): Observable<ParentChild[]> {
    return this.http.get<ParentChild[]>(this.endpoint);
  }
}
