import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { ClassroomOption } from '../models/classroom-option.model';

interface ClassroomApiResponse {
  id: number;
  nom?: string | null;
  niveau?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ClassroomService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/classes`;

  getAll(): Observable<ClassroomOption[]> {
    return this.http.get<ClassroomApiResponse[]>(this.apiUrl).pipe(
      map((classes) =>
        (classes ?? []).map((classroom) => ({
          id: classroom.id,
          nom: classroom.nom ?? '',
          niveau: classroom.niveau ?? ''
        }))
      )
    );
  }
}
