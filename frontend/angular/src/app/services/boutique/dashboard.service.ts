import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardStats } from 'src/app/models/boutique/dashboard-stats.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private adminUrl = 'http://localhost:8081/api/admin/boutique/dashboard/stats';
  private http = inject(HttpClient);

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.adminUrl);
  }
}
