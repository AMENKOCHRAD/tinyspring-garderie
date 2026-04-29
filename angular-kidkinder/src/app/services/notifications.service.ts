import { Injectable, DestroyRef, Injector, effect, inject, runInInjectionContext, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../shared/auth.service';

type RealtimeNotification =
  | {
      type: 'PARENT_UNREAD_OBSERVATIONS_COUNT';
      unreadObservationsCount?: number;
      enfantId?: number;
      observationId?: number;
      titre?: string;
      creeLe?: string;
    }
  | {
      type: 'PARENT_TRAITEMENT_VALIDATION';
      enfantId?: number;
      traitementId?: number;
      nomTraitement?: string;
      titre?: string; // statut
      validationNote?: string;
    }
  | { type: string; [key: string]: any };

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  readonly parentUnreadObservationsCount = signal<number>(0);
  readonly animatriceReminderMessages = signal<string[]>([]);

  private started = false;
  private socket: WebSocket | null = null;
  private lastToken: string | null = null;
  private reconnectTimer: any = null;
  private reconnectDelayMs = 1000;
  private lastUnreadCount = 0;

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;

    runInInjectionContext(this.injector, () => {
      effect(() => {
        const user = this.authService.currentUser();
        const token = user?.token ?? null;
        const role = user?.role ?? null;

        if (!token || (role !== 'PARENT' && role !== 'ANIMATRICE')) {
          this.disconnect();
          this.parentUnreadObservationsCount.set(0);
          this.animatriceReminderMessages.set([]);
          return;
        }

        this.connect(token);
      });
    });

    this.destroyRef.onDestroy(() => {
      this.disconnect();
    });
  }

  refresh(): void {
    const token = this.authService.getToken();
    if (!token) {
      this.parentUnreadObservationsCount.set(0);
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user || user.role !== 'PARENT') {
      return;
    }

    this.http.get<{ count: number }>(`/api/parent/observations/unread-count`).subscribe({
      next: (data) => {
        const count = Number(data?.count ?? 0);
        const normalized = Number.isFinite(count) ? count : 0;
        this.lastUnreadCount = normalized;
        this.parentUnreadObservationsCount.set(normalized);
      },
      error: () => {
        // ignore polling errors
      }
    });
  }

  private connect(token: string): void {
    if (this.socket && this.lastToken === token && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.disconnect(false);
    this.lastToken = token;

    const url = this.buildWebSocketUrl(token);
    try {
      this.socket = new WebSocket(url);
    } catch {
      this.scheduleReconnect();
      return;
    }

    this.socket.onopen = () => {
      this.reconnectDelayMs = 1000;
      this.refresh();
    };

    this.socket.onmessage = (event) => {
      const message = this.safeParse(event?.data);
      if (!message) return;

      const notif = message as RealtimeNotification;
      if (notif.type === 'PARENT_UNREAD_OBSERVATIONS_COUNT') {
        const count = Number((notif as any).unreadObservationsCount ?? 0);
        const normalized = Number.isFinite(count) ? count : 0;
        const titre = (notif as any).titre ? String((notif as any).titre) : '';

        if (normalized > this.lastUnreadCount) {
          this.maybeShowBrowserNotification(titre || 'Nouvelle observation disponible');
        }

        this.lastUnreadCount = normalized;
        this.parentUnreadObservationsCount.set(normalized);
      }

      if (notif.type === 'PARENT_TRAITEMENT_VALIDATION') {
        const statut = (notif as any).titre ? String((notif as any).titre) : '';
        const nomTraitement = (notif as any).nomTraitement ? String((notif as any).nomTraitement) : 'Traitement';
        const body = statut ? `${nomTraitement} : ${statut}` : `${nomTraitement} mis a jour`;
        this.maybeShowBrowserNotification(body);
      }

      if (notif.type === 'ANIMATRICE_MEDICAMENT_DUE') {
        const enfantPrenom = (notif as any).enfantPrenom ? String((notif as any).enfantPrenom) : '';
        const enfantNom = (notif as any).enfantNom ? String((notif as any).enfantNom) : '';
        const nomTraitement = (notif as any).nomTraitement ? String((notif as any).nomTraitement) : '';
        const heure = (notif as any).heurePrevue ? String((notif as any).heurePrevue) : '';
        const date = (notif as any).datePrise ? String((notif as any).datePrise) : '';

        const msg = `Rappel ${heure}${date ? ' (' + date + ')' : ''}: ${enfantPrenom} ${enfantNom} - ${nomTraitement}`.trim();
        const next = [msg, ...this.animatriceReminderMessages()].slice(0, 3);
        this.animatriceReminderMessages.set(next);
        this.maybeShowBrowserNotification(msg || 'Rappel traitement');
      }
    };

    this.socket.onerror = () => {
      // ignore; close handler will schedule reconnect
    };

    this.socket.onclose = () => {
      this.socket = null;
      this.scheduleReconnect();
    };
  }

  private disconnect(clearToken = true): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (clearToken) {
      this.lastToken = null;
    }

    if (this.socket) {
      try {
        this.socket.onopen = null;
        this.socket.onmessage = null;
        this.socket.onerror = null;
        this.socket.onclose = null;
        this.socket.close();
      } catch {
        // ignore
      } finally {
        this.socket = null;
      }
    }
  }

  private scheduleReconnect(): void {
    const user = this.authService.getCurrentUser();
    if (!user || (user.role !== 'PARENT' && user.role !== 'ANIMATRICE') || !user.token) {
      return;
    }

    if (this.reconnectTimer) {
      return;
    }

    const delay = this.reconnectDelayMs;
    this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, 30_000);

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect(user.token);
    }, delay);
  }

  private buildWebSocketUrl(token: string): string {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.hostname || 'localhost';

    // Dev: Angular runs on 4200 while backend is 8081
    const isDev = window.location.port === '4200';
    const origin = isDev ? `${proto}://${host}:8081` : `${proto}://${window.location.host}`;

    return `${origin}/ws/notifications?token=${encodeURIComponent(token)}`;
  }

  private safeParse(value: any): unknown | null {
    if (typeof value !== 'string' || !value.trim()) {
      return null;
    }
    try {
      return JSON.parse(value);
    } catch {
      return null;
    }
  }

  private maybeShowBrowserNotification(message: string): void {
    try {
      if (typeof Notification === 'undefined') return;
      if (Notification.permission !== 'granted') return;
      new Notification('TinySpring - Notification', { body: message });
    } catch {
      // ignore
    }
  }
}
