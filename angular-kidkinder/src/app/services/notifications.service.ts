import { Injectable, DestroyRef, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../shared/auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly parentUnreadObservationsCount = signal<number>(0);

  private intervalId: any = null;

  start(): void {
    if (this.intervalId) {
      return;
    }

    this.refresh();
    this.intervalId = setInterval(() => this.refresh(), 60_000);

    this.destroyRef.onDestroy(() => {
      if (this.intervalId) {
        clearInterval(this.intervalId);
        this.intervalId = null;
      }
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
      next: (data) => this.parentUnreadObservationsCount.set(Number(data?.count ?? 0)),
      error: () => {
        // ignore polling errors
      }
    });
  }
}

