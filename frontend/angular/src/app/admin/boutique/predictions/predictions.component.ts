import { Component, OnInit, ViewChild, TemplateRef, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { PredictionService } from 'src/app/services/boutique/prediction.service';
import { Prediction, PredictionSemaine } from 'src/app/models/boutique/prediction.model';

type PredictionFilter = 'all' | 'alerts' | 'ok';

interface PredictionViewModel extends Prediction {
  semaines: PredictionSemaine[];
  couverture: number;
  confianceMoyenne: number;
}

@Component({
  selector: 'app-admin-predictions',
  standalone: true,
  imports: [SharedModule, CommonModule],
  templateUrl: './predictions.component.html',
  styleUrls: ['./predictions.component.scss']
})
export class PredictionsComponent implements OnInit {
  private predictionService = inject(PredictionService);
  private cdr = inject(ChangeDetectorRef);
  private modalService = inject(NgbModal);

  @ViewChild('detailModal') detailModal!: TemplateRef<any>;

  predictions: PredictionViewModel[] = [];
  filteredPredictions: PredictionViewModel[] = [];
  selectedPrediction: PredictionViewModel | null = null;

  loading = true;
  recalculating = false;
  errorMsg = '';
  successMsg = '';
  activeFilter: PredictionFilter = 'all';

  ngOnInit(): void {
    this.loadPredictions();
  }

  loadPredictions(): void {
    this.loading = true;
    this.errorMsg = '';

    this.predictionService.getAll().subscribe({
      next: (predictions) => {
        this.predictions = predictions.map((prediction) => this.toViewModel(prediction)).sort(this.sortPredictions);
        this.applyFilter();
        this.predictionService.alertCount.set(this.alertesCount);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.errorMsg = this.buildErrorMessage(error);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get alertesCount(): number {
    return this.predictions.filter((prediction) => prediction.alerteRupture).length;
  }

  get stockOkCount(): number {
    return this.predictions.length - this.alertesCount;
  }

  get lastUpdated(): string | null {
    return this.predictions[0]?.dateCalcul ?? null;
  }

  setFilter(filter: PredictionFilter): void {
    this.activeFilter = filter;
    this.applyFilter();
  }

  applyFilter(): void {
    switch (this.activeFilter) {
      case 'alerts':
        this.filteredPredictions = this.predictions.filter((prediction) => prediction.alerteRupture);
        break;
      case 'ok':
        this.filteredPredictions = this.predictions.filter((prediction) => !prediction.alerteRupture);
        break;
      default:
        this.filteredPredictions = [...this.predictions];
        break;
    }
  }

  recalculerMaintenant(): void {
    this.recalculating = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.predictionService.recalculer().subscribe({
      next: (msg) => {
        console.log('Recalcul OK:', msg);
        this.successMsg = msg || 'Recalcul ML lance.';
        this.predictionService.refreshAlertCount();
        this.loadPredictions();
        this.recalculating = false;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Erreur recalcul:', error);
        this.errorMsg = this.buildErrorMessage(error);
        this.recalculating = false;
        this.cdr.detectChanges();
      }
    });
  }

  openDetail(prediction: PredictionViewModel): void {
    this.selectedPrediction = prediction;
    this.modalService.open(this.detailModal, { size: 'xl', centered: true });
  }

  getCoverageBarClass(prediction: PredictionViewModel): string {
    if (prediction.couverture > 100) {
      return 'b-progress__fill--green';
    }
    if (prediction.couverture >= 50) {
      return 'b-progress__fill--orange';
    }
    return 'b-progress__fill--red';
  }

  getConfianceBarClass(confiance: number): string {
    if (confiance >= 80) {
      return 'b-progress__fill--green';
    }
    if (confiance >= 50) {
      return 'b-progress__fill--orange';
    }
    return 'b-progress__fill--red';
  }

  getCoverageLabel(prediction: PredictionViewModel): string {
    if (prediction.totalPrevu4Semaines <= 0) {
      return 'Couverture complete';
    }
    return `${prediction.couverture}%`;
  }

  getCoverageDisplayWidth(prediction: PredictionViewModel): number {
    return Math.min(Math.max(prediction.couverture, 0), 100);
  }

  getStatusBadgeClass(prediction: PredictionViewModel): string {
    return prediction.alerteRupture ? 'b-badge--danger' : 'b-badge--success';
  }

  getStatusLabel(prediction: PredictionViewModel): string {
    return prediction.alerteRupture ? 'Alerte rupture' : 'Stock OK';
  }

  getFilterButtonClass(filter: PredictionFilter): string {
    return this.activeFilter === filter ? 'b-filter-btn--active' : 'b-filter-btn--outline';
  }

  getConfianceBadgeLabel(confiance: number): string {
    return `${Math.round(confiance)}%`;
  }

  getRemainingStock(prediction: PredictionViewModel): number {
    return prediction.stockActuel - prediction.totalPrevu4Semaines;
  }

  getRemainingStockLabel(prediction: PredictionViewModel): string {
    const remaining = this.getRemainingStock(prediction);
    if (remaining >= 0) {
      return `Reste apres 4 sem : ${remaining} unite(s)`;
    }
    return `Manque apres 4 sem : ${Math.abs(remaining)} unite(s)`;
  }

  getNeedBarWidth(prediction: PredictionViewModel): number {
    const max = Math.max(prediction.stockActuel, prediction.totalPrevu4Semaines, 1);
    return Math.min(100, Math.round((prediction.totalPrevu4Semaines / max) * 100));
  }

  formatRange(semaine: PredictionSemaine): string {
    const start = this.formatShortDate(semaine.periode_debut);
    const end = this.formatShortDate(semaine.periode_fin);
    return `${start} - ${end}`;
  }

  trackPrediction(_index: number, prediction: PredictionViewModel): number {
    return prediction.id;
  }

  trackWeek(_index: number, semaine: PredictionSemaine): string {
    return `${semaine.semaine}-${semaine.periode_debut}-${semaine.periode_fin}`;
  }

  private toViewModel(prediction: Prediction): PredictionViewModel {
    const semaines = this.parseDetails(prediction.detailsJson);
    const totalPrevu4Semaines = Number(prediction.totalPrevu4Semaines ?? 0);
    const stockActuel = Number(prediction.stockActuel ?? 0);
    const couverture = totalPrevu4Semaines > 0 ? Math.round((stockActuel / totalPrevu4Semaines) * 100) : 999;
    const confianceMoyenne =
      semaines.length > 0
        ? semaines.reduce((sum, semaine) => sum + Number(semaine.confiance ?? 0), 0) / semaines.length
        : 0;

    return {
      ...prediction,
      stockActuel,
      totalPrevu4Semaines,
      semaines,
      couverture,
      confianceMoyenne
    };
  }

  private parseDetails(detailsJson: string): PredictionSemaine[] {
    if (!detailsJson) {
      return [];
    }

    try {
      const parsed = JSON.parse(detailsJson) as PredictionSemaine[];
      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed.map((semaine) => ({
        semaine: Number(semaine.semaine ?? 0),
        periode_debut: semaine.periode_debut,
        periode_fin: semaine.periode_fin,
        quantite_prevue: Number(semaine.quantite_prevue ?? 0),
        confiance: this.normalizeConfidence(Number(semaine.confiance ?? 0))
      }));
    } catch {
      return [];
    }
  }

  private normalizeConfidence(value: number): number {
    if (value <= 1) {
      return Math.round(value * 100);
    }
    return Math.round(value);
  }

  private formatShortDate(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short' }).format(date);
    } catch {
      return dateStr;
    }
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error.trim();
    }

    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message.trim();
    }

    if (error.status === 401) {
      return 'Acces refuse : JWT absent ou invalide.';
    }

    if (error.status === 403) {
      return 'Acces admin requis pour les predictions ML.';
    }

    if (error.status === 0) {
      return 'Backend inaccessible ou probleme reseau.';
    }

    return `Erreur backend (${error.status || 'inconnue'}).`;
  }

  private sortPredictions(a: PredictionViewModel, b: PredictionViewModel): number {
    if (a.alerteRupture !== b.alerteRupture) {
      return Number(b.alerteRupture) - Number(a.alerteRupture);
    }
    return b.totalPrevu4Semaines - a.totalPrevu4Semaines;
  }
}
