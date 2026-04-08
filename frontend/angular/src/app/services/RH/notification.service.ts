import { Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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

  // ✅ Signal réactif — tous les composants s'y abonnent
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private countSubject = new BehaviorSubject<number>(0);
  public count$ = this.countSubject.asObservable();

  constructor(private http: HttpClient, private zone: NgZone) {}

  private getHeaders(): HttpHeaders {
    const credentials = btoa('admin@garderie.com:admin123');
    return new HttpHeaders({ 'Authorization': `Basic ${credentials}` });
  }

  // ✅ Connexion SSE
  connectSSE(): void {
    const credentials = btoa('admin@garderie.com:admin123');
    this.eventSource = new EventSource(
      `${this.apiUrl}/stream`,
      { withCredentials: false }
    );

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
      // Reconnexion après 5 secondes
      setTimeout(() => this.connectSSE(), 5000);
    };
  }

  disconnectSSE(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }

  chargerNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl, { headers: this.getHeaders() });
  }

  chargerCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/count`, { headers: this.getHeaders() });
  }

  marquerLue(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/lire`, {}, { headers: this.getHeaders() });
  }

  marquerToutLu(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/lire-tout`, {}, { headers: this.getHeaders() });
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  setNotifications(notifs: Notification[]): void {
    this.notificationsSubject.next(notifs);
    this.countSubject.next(notifs.filter(n => !n.read).length);
  }
}