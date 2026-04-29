import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';

export interface Notification {
  id: number;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private apiUrl = 'http://localhost:8081/api/admin/notifications';
  private eventSource!: EventSource;

  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private countSubject = new BehaviorSubject<number>(0);
  public count$ = this.countSubject.asObservable();

  constructor(private http: HttpClient, private zone: NgZone) {}

  // ✅ Supprimé getHeaders() — l'intercepteur JWT gère ça automatiquement

  // ✅ SSE — EventSource ne supporte pas les headers custom
  // On passe le token en query param pour contourner cette limitation
  connectSSE(): void {
    const token = localStorage.getItem('auth_token');
    const url = token
      ? `${this.apiUrl}/stream?token=${token}`
      : `${this.apiUrl}/stream`;

    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('notification', (event: any) => {
      this.zone.run(() => {
        const notification: Notification = JSON.parse(event.data);
        const current = this.notificationsSubject.value;
        this.notificationsSubject.next([notification, ...current]);
        this.countSubject.next(this.countSubject.value + 1);
      });
    });

    this.eventSource.onerror = () => {
      this.eventSource.close();
      setTimeout(() => this.connectSSE(), 5000);
    };
  }

  disconnectSSE(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }

  // ✅ Requêtes HTTP — intercepteur ajoute Bearer token automatiquement
  chargerNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl);
  }

  chargerCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/count`);
  }

  marquerLue(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/lire`, {});
  }

  marquerToutLu(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/lire-tout`, {});
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  setNotifications(notifs: Notification[]): void {
    this.notificationsSubject.next(notifs);
    this.countSubject.next(notifs.filter(n => !n.read).length);
  }
}