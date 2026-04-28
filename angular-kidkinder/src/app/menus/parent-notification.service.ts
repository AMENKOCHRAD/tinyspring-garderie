import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ParentNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  seen: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ParentNotificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/parent/notifications';

  getNotifications(parentId: number): Observable<ParentNotification[]> {
    return this.http.get<ParentNotification[]>(`${this.apiUrl}?parentId=${parentId}`);
  }

  markAsSeen(id: number): Observable<ParentNotification> {
    return this.http.put<ParentNotification>(`${this.apiUrl}/${id}/seen`, {});
  }
}