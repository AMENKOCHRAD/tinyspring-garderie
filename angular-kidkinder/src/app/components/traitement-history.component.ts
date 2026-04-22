import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EnfantService } from '../services/enfant.service';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../shared/auth.service';

interface PriseTraitementDto {
  id: number;
  traitementId: number;
  nomTraitement: string;
  enfantId: number;
  enfantNom: string;
  enfantPrenom: string;
  datePrise: string;
  heurePrevue: string;
  donneLe: string;
  donneParNom: string;
  note?: string | null;
}

@Component({
  selector: 'app-traitement-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="table-card">
      <div style="display:flex; justify-content:space-between; gap:12px; align-items:center; flex-wrap:wrap;">
        <h3>Historique de mes prises</h3>
        <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
          <label class="muted">Du</label>
          <input type="date" [(ngModel)]="fromIso" name="historyFrom" (change)="charger()" />
          <label class="muted">Au</label>
          <input type="date" [(ngModel)]="toIso" name="historyTo" (change)="charger()" />
        </div>
      </div>

      <div style="margin-top:12px; display:flex; gap:12px; flex-wrap:wrap; align-items:center;">
        <input
          [(ngModel)]="term"
          name="historySearch"
          placeholder="Recherche (enfant, traitement, heure...)"
          style="min-width:260px;"
        />

        <button type="button" class="ts-button" (click)="fromIso=todayIso; toIso=todayIso; charger()" [disabled]="isLoading">
          Aujourd'hui
        </button>

        <button type="button" class="ts-button" (click)="charger()" [disabled]="isLoading">
          {{ isLoading ? 'Chargement...' : 'Rafraichir' }}
        </button>
      </div>

      <div *ngIf="errorMessage" class="alert alert-danger" style="margin-top:12px;">
        {{ errorMessage }}
      </div>

      <div class="stack-list" style="margin-top:12px;" *ngIf="!isLoading">
        <article class="stack-item" *ngFor="let p of filteredPrises()">
          <strong>{{ p.enfantPrenom }} {{ p.enfantNom }}</strong>
          <p class="muted">
            {{ p.nomTraitement }} &bull; prevu {{ p.heurePrevue }} &bull; donne {{ formatDateTime(p.donneLe) }} &bull; par {{ p.donneParNom }}
          </p>
          <p class="muted" *ngIf="p.note">{{ p.note }}</p>
        </article>

        <p class="muted" *ngIf="filteredPrises().length === 0">Aucune prise enregistree.</p>
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `
  ]
})
export class TraitementHistoryComponent {
  private readonly enfantService = inject(EnfantService);
  private readonly authService = inject(AuthService);

  isLoading = false;
  errorMessage = '';

  readonly todayIso = new Date().toISOString().slice(0, 10);
  dateIso = new Date().toISOString().slice(0, 10);
  fromIso = this.addDays(this.dateIso, -30);
  toIso = this.dateIso;
  term = '';
  prises: PriseTraitementDto[] = [];

  public constructor() {
    this.charger();
  }

  charger(): void {
    this.errorMessage = '';
    this.isLoading = true;

    const token = this.authService.getToken();
    if (!token) {
      this.errorMessage = 'Non authentifie (token manquant ou invalide). Veuillez vous reconnecter.';
      this.isLoading = false;
      return;
    }

    const from = this.fromIso || this.todayIso;
    const to = this.toIso || from;

    this.enfantService.getMesPrisesTraitements(from, to).subscribe({
      next: (data) => {
        this.prises = ((data ?? []) as PriseTraitementDto[]).sort((a, b) => {
          const keyA = `${a.datePrise ?? ''} ${a.heurePrevue ?? ''} ${a.enfantPrenom ?? ''} ${a.enfantNom ?? ''}`;
          const keyB = `${b.datePrise ?? ''} ${b.heurePrevue ?? ''} ${b.enfantPrenom ?? ''} ${b.enfantNom ?? ''}`;
          return keyB.localeCompare(keyA);
        });
        this.isLoading = false;
      },
      error: (err) => {
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        const details = err?.error?.message || err?.error || err?.message || '';
        this.errorMessage = `Impossible de charger l'historique${status}. ${details}`.trim();
        this.isLoading = false;
      }
    });
  }

  formatDateTime(value: string): string {
    if (!value) {
      return '';
    }

    // ISO: 2026-04-20T09:03:12.123 -> 2026-04-20 09:03
    const normalized = value.replace('T', ' ');
    return normalized.length >= 16 ? normalized.slice(0, 16) : normalized;
  }

  filteredPrises(): PriseTraitementDto[] {
    const normalized = this.normalize(this.term);
    if (!normalized) {
      return this.prises;
    }

    return (this.prises ?? []).filter((p) => {
      const hay = [
        `${p.enfantPrenom ?? ''} ${p.enfantNom ?? ''}`,
        p.nomTraitement ?? '',
        p.heurePrevue ?? '',
        p.donneParNom ?? '',
        p.note ?? ''
      ].join(' ');
      return this.normalize(hay).includes(normalized);
    });
  }

  private normalize(value: string): string {
    return (value ?? '')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim();
  }

  private addDays(dateIso: string, days: number): string {
    const d = new Date(dateIso);
    if (isNaN(d.getTime())) {
      return dateIso;
    }
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  }
}
