import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { WeeklyMenu, WeeklyMenuRequest } from '../models/weekly-menu.model';

@Injectable({
  providedIn: 'root'
})
export class WeeklyMenuService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/menus/weekly`;

  getAll(): Observable<WeeklyMenu[]> {
    return this.http.get<WeeklyMenu[]>(this.apiUrl);
  }

  getById(id: number): Observable<WeeklyMenu> {
    return this.http.get<WeeklyMenu>(`${this.apiUrl}/${id}`);
  }

  create(payload: WeeklyMenuRequest): Observable<WeeklyMenu> {
    return this.http.post<WeeklyMenu>(this.apiUrl, payload);
  }

  update(id: number, payload: WeeklyMenuRequest): Observable<WeeklyMenu> {
    return this.http.put<WeeklyMenu>(`${this.apiUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  duplicate(id: number): Observable<WeeklyMenu> {
    return this.http.post<WeeklyMenu>(`${this.apiUrl}/${id}/duplicate`, {});
  }


generateWithAI(payload: { weekStartDate: string }): Observable<WeeklyMenuRequest> {
  return this.http.post<WeeklyMenuRequest>(`${this.apiUrl}/ai/generate`, payload);
}
}
