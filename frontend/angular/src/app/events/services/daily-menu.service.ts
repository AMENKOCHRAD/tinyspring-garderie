import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { DailyMenu, MenuDayOfWeek } from '../models/daily-menu.model';

export interface DailyMenuRequest {
  weeklyMenuId: number;
  menuDate: string | null;
  dayOfWeek: MenuDayOfWeek;
  isVisibleToParents: boolean;
  publishedAt?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DailyMenuService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/menus/daily`;

  create(payload: DailyMenuRequest): Observable<DailyMenu> {
    return this.http.post<DailyMenu>(this.apiUrl, payload);
  }

  update(id: number, payload: DailyMenuRequest): Observable<DailyMenu> {
    return this.http.put<DailyMenu>(`${this.apiUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
