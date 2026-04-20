import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription, interval } from 'rxjs';
import { Prediction } from 'src/app/models/boutique/prediction.model';

@Injectable({ providedIn: 'root' })
export class PredictionService {
  private http = inject(HttpClient);
  private readonly adminUrl = 'http://localhost:8081/api/admin/boutique/predictions';
  private pollingSub: Subscription | null = null;

  readonly alertCount = signal(0);

  getAll() {
    return this.http.get<Prediction[]>(this.adminUrl);
  }

  getAlertes() {
    return this.http.get<Prediction[]>(`${this.adminUrl}/alertes`);
  }

  recalculer() {
    return this.http.post(`${this.adminUrl}/recalculer`, {}, { responseType: 'text' });
  }

  refreshAlertCount(): void {
    this.getAlertes().subscribe({
      next: (predictions) => this.alertCount.set(predictions.length),
      error: () => this.alertCount.set(0)
    });
  }

  startAlertPolling(): void {
    if (this.pollingSub) {
      return;
    }

    this.refreshAlertCount();
    this.pollingSub = interval(60_000).subscribe(() => this.refreshAlertCount());
  }

  stopAlertPolling(): void {
    this.pollingSub?.unsubscribe();
    this.pollingSub = null;
    this.alertCount.set(0);
  }
}
