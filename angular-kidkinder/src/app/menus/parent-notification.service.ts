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
  priority: 'URGENT' | 'NORMAL';
  relatedEntityId?: number;
  relatedEntityType?: 'MENU' | 'EVENT';
}

@Injectable({ providedIn: 'root' })
export class ParentNotificationService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8081/api/parent/notifications';

  getNotifications(parentId: number): Observable<ParentNotification[]> {
    return this.http.get<ParentNotification[]>(`${this.api}?parentId=${parentId}`);
  }

  getUnreadCount(parentId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/unread-count?parentId=${parentId}`);
  }

  markAsSeen(id: number): Observable<ParentNotification> {
    return this.http.put<ParentNotification>(`${this.api}/${id}/seen`, {});
  }

  markAllRead(parentId: number): Observable<void> {
    return this.http.put<void>(`${this.api}/mark-all-read?parentId=${parentId}`, {});
  }
}