import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EnfantService, Enfant } from '../enfant';
import { EtatSanitaireService } from 'src/app/services/etat-sanitaire';
import { ValidationTraitementsService } from 'src/app/services/validation-traitements';

interface ConditionSanitaire {
  id?: number;
  nomCondition?: string;
  type?: string;
}

interface Traitement {
  id?: number;
  nomTraitement?: string;
  statut?: string;
}

@Component({
  selector: 'app-statistiques-enfants',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './statistiques-enfants.html',
  styleUrls: ['./statistiques-enfants.scss']
})
export class StatistiquesEnfantsComponent implements OnInit {
  private readonly enfantService = inject(EnfantService);
  private readonly etatSanitaireService = inject(EtatSanitaireService);
  private readonly validationService = inject(ValidationTraitementsService);

  isLoading = false;
  error = '';

  totalEnfants = 0;
  totalConditions = 0;
  totalTraitements = 0;

  conditionsChroniques = 0;
  conditionsTemporaires = 0;
  conditionsAutres = 0;

  traitementsValides = 0;
  traitementsEnAttente = 0;
  traitementsRefuses = 0;
  traitementsActifs = 0;
  traitementsAnnules = 0;
  traitementsAutres = 0;

  allergiesTop: Array<{ allergie: string; count: number }> = [];

  ngOnInit(): void {
    this.chargerStats();
  }

  chargerStats(): void {
    this.isLoading = true;
    this.error = '';

    forkJoin({
      enfants: this.enfantService.getAllEnfants().pipe(catchError(() => of([] as Enfant[]))),
      enAttente: this.validationService.getTraitementsEnAttente().pipe(catchError(() => of([] as any[])))
    }).subscribe({
      next: ({ enfants, enAttente }) => {
        const enfantsList = enfants ?? [];
        this.totalEnfants = enfantsList.length;
        this.traitementsEnAttente = (enAttente ?? []).length;

        this.allergiesTop = this.computeAllergiesTop(enfantsList);

        if (!enfantsList.length) {
          this.resetHealthCountsKeepChildren();
          this.isLoading = false;
          return;
        }

        const requetes = enfantsList.map((enfant) => {
          const enfantId = Number(enfant.id);
          if (!enfantId) {
            return of({ conditions: [], traitements: [] });
          }

          return forkJoin({
            conditions: this.etatSanitaireService.getConditionsByEnfant(enfantId).pipe(catchError(() => of([]))),
            traitements: this.etatSanitaireService.getTraitementsByEnfant(enfantId).pipe(catchError(() => of([])))
          });
        });

        forkJoin(requetes).subscribe({
          next: (rows: any[]) => {
            const allConditions: ConditionSanitaire[] = [];
            const allTraitements: Traitement[] = [];

            for (const row of rows ?? []) {
              const conditions = this.toArray<ConditionSanitaire>(row?.conditions);
              const traitements = this.toArray<Traitement>(row?.traitements);
              allConditions.push(...conditions);
              allTraitements.push(...traitements);
            }

            this.totalConditions = allConditions.length;
            this.totalTraitements = allTraitements.length;

            this.computeConditionStats(allConditions);
            this.computeTraitementStats(allTraitements);

            // Si on a déjà un compteur en attente via endpoint dédié, on garde le max
            this.traitementsEnAttente = Math.max(this.traitementsEnAttente, this.countByStatut(allTraitements, 'EN_ATTENTE_VALIDATION'));

            this.isLoading = false;
          },
          error: () => {
            this.error = 'Erreur lors du chargement des statistiques sanitaires';
            this.isLoading = false;
          }
        });
      },
      error: () => {
        this.error = 'Erreur lors du chargement des statistiques';
        this.isLoading = false;
      }
    });
  }

  private resetHealthCountsKeepChildren(): void {
    this.totalConditions = 0;
    this.totalTraitements = 0;
    this.conditionsChroniques = 0;
    this.conditionsTemporaires = 0;
    this.conditionsAutres = 0;
    this.traitementsValides = 0;
    this.traitementsRefuses = 0;
    this.traitementsActifs = 0;
    this.traitementsAnnules = 0;
    this.traitementsAutres = 0;
  }

  private toArray<T>(data: any): T[] {
    if (Array.isArray(data)) {
      return data;
    }
    if (data === null || data === undefined) {
      return [];
    }
    return [data];
  }

  private computeAllergiesTop(enfants: Enfant[]): Array<{ allergie: string; count: number }> {
    const counts = new Map<string, number>();

    for (const enfant of enfants ?? []) {
      const raw = (enfant.allergies ?? '').toString().trim();
      if (!raw) continue;

      const parts = raw
        .split(/[,;|/]/g)
        .map((p) => p.trim())
        .filter(Boolean);

      for (const part of parts) {
        const key = part.toLowerCase();
        counts.set(key, (counts.get(key) ?? 0) + 1);
      }
    }

    return Array.from(counts.entries())
      .map(([k, v]) => ({ allergie: this.titleCase(k), count: v }))
      .sort((a, b) => b.count - a.count || a.allergie.localeCompare(b.allergie))
      .slice(0, 8);
  }

  private computeConditionStats(conditions: ConditionSanitaire[]): void {
    let chroniques = 0;
    let temporaires = 0;
    let autres = 0;

    for (const c of conditions ?? []) {
      const t = (c?.type ?? '').toString().toLowerCase();
      if (t.includes('chronique')) {
        chroniques++;
      } else if (t.includes('temporaire')) {
        temporaires++;
      } else {
        autres++;
      }
    }

    this.conditionsChroniques = chroniques;
    this.conditionsTemporaires = temporaires;
    this.conditionsAutres = autres;
  }

  private computeTraitementStats(traitements: Traitement[]): void {
    this.traitementsValides = this.countByStatut(traitements, 'VALIDE');
    this.traitementsRefuses = this.countByStatut(traitements, 'REFUSE');
    this.traitementsActifs = this.countByStatut(traitements, 'ACTIF');
    this.traitementsAnnules = this.countByStatut(traitements, 'ANNULE');

    const known = new Set(['VALIDE', 'REFUSE', 'ACTIF', 'ANNULE', 'EN_ATTENTE_VALIDATION']);
    let autres = 0;
    for (const t of traitements ?? []) {
      const s = (t?.statut ?? '').toString().toUpperCase();
      if (!s || !known.has(s)) {
        autres++;
      }
    }
    this.traitementsAutres = autres;
  }

  private countByStatut(traitements: Traitement[], statut: string): number {
    const target = (statut ?? '').toUpperCase();
    return (traitements ?? []).filter((t) => (t?.statut ?? '').toString().toUpperCase() === target).length;
  }

  private titleCase(value: string): string {
    const s = (value ?? '').trim();
    if (!s) return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
  }
}

