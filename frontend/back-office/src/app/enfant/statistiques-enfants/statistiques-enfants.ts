import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EnfantService, Enfant } from '../enfant';
import { EtatSanitaireService } from 'src/app/services/etat-sanitaire';
import { NgApexchartsModule } from 'ng-apexcharts';
import type {
  ApexChart,
  ApexDataLabels,
  ApexLegend,
  ApexNonAxisChartSeries,
  ApexPlotOptions,
  ApexResponsive,
  ApexStroke,
  ApexTheme
} from 'ng-apexcharts';

interface ConditionSanitaire {
  id?: number;
  nomCondition?: string;
  type?: string;
}

interface Traitement {
  id?: number;
  nomTraitement?: string;
}

@Component({
  selector: 'app-statistiques-enfants',
  standalone: true,
  imports: [CommonModule, NgApexchartsModule],
  templateUrl: './statistiques-enfants.html',
  styleUrls: ['./statistiques-enfants.scss']
})
export class StatistiquesEnfantsComponent implements OnInit {
  private readonly enfantService = inject(EnfantService);
  private readonly etatSanitaireService = inject(EtatSanitaireService);
  private readonly cdr = inject(ChangeDetectorRef);

  isLoading = false;
  error = '';

  totalEnfants = 0;
  totalConditions = 0;
  totalTraitements = 0;

  conditionsChroniques = 0;
  conditionsTemporaires = 0;
  conditionsAutres = 0;

  allergiesTop: Array<{ allergie: string; count: number }> = [];

  niveauGroups: Array<{
    key: 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU';
    label: string;
    ageRangeLabel: string;
    enfants: Enfant[];
  }> = [
    { key: 'CRECHE', label: 'Crèche', ageRangeLabel: '0 à 3 ans', enfants: [] },
    { key: 'PRESCOLAIRE', label: 'Préscolaire', ageRangeLabel: '3 à 5 ans', enfants: [] },
    { key: 'PREPARATOIRE', label: 'Préparatoire', ageRangeLabel: '5 à 6 ans', enfants: [] },
    { key: 'HORS_NIVEAU', label: 'Hors niveau', ageRangeLabel: 'Autre', enfants: [] }
  ];

  niveauChart: {
    series: ApexNonAxisChartSeries;
    chart: ApexChart;
    labels: string[];
    legend: ApexLegend;
    dataLabels: ApexDataLabels;
    plotOptions: ApexPlotOptions;
    stroke: ApexStroke;
    theme: ApexTheme;
    responsive: ApexResponsive[];
  } | null = null;

  conditionChart: {
    series: ApexNonAxisChartSeries;
    chart: ApexChart;
    labels: string[];
    legend: ApexLegend;
    dataLabels: ApexDataLabels;
    plotOptions: ApexPlotOptions;
    stroke: ApexStroke;
    theme: ApexTheme;
    responsive: ApexResponsive[];
  } | null = null;

  allergiesChart: {
    series: ApexNonAxisChartSeries;
    chart: ApexChart;
    labels: string[];
    legend: ApexLegend;
    dataLabels: ApexDataLabels;
    plotOptions: ApexPlotOptions;
    stroke: ApexStroke;
    theme: ApexTheme;
    responsive: ApexResponsive[];
  } | null = null;

  ngOnInit(): void {
    this.chargerStats();
  }

  chargerStats(): void {
    this.isLoading = true;
    this.error = '';

    this.enfantService
      .getAllEnfants()
      .pipe(catchError(() => of([] as Enfant[])))
      .subscribe({
        next: (enfants) => {
          const enfantsList = enfants ?? [];
          this.totalEnfants = enfantsList.length;

          this.niveauGroups = this.computeNiveauGroups(enfantsList);
          this.niveauChart = this.buildNiveauChart(this.niveauGroups);

          this.allergiesTop = this.computeAllergiesTop(enfantsList);
          this.allergiesChart = this.buildAllergiesChart(this.allergiesTop);

          if (!enfantsList.length) {
            this.resetHealthCountsKeepChildren();
            this.isLoading = false;
            this.cdr.detectChanges();
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
            this.conditionChart = this.buildConditionsChart(this.conditionsChroniques, this.conditionsTemporaires, this.conditionsAutres);

            // Si on a déjà un compteur en attente via endpoint dédié, on garde le max

            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.error = 'Erreur lors du chargement des statistiques sanitaires';
            this.isLoading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: () => {
        this.error = 'Erreur lors du chargement des statistiques';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private resetHealthCountsKeepChildren(): void {
    this.totalConditions = 0;
    this.totalTraitements = 0;
    this.conditionsChroniques = 0;
    this.conditionsTemporaires = 0;
    this.conditionsAutres = 0;
    this.conditionChart = this.buildConditionsChart(0, 0, 0);
    this.allergiesChart = this.buildAllergiesChart([]);
    this.niveauChart = this.buildNiveauChart(this.niveauGroups);
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

  private buildConditionsChart(chroniques: number, temporaires: number, autres: number) {
    return this.buildDonutChart([chroniques, temporaires, autres], ['Chroniques', 'Temporaires', 'Autres'], 'Aucune condition');
  }

  private buildAllergiesChart(allergies: Array<{ allergie: string; count: number }>) {
    const top = (allergies ?? []).slice(0, 6);
    const series = top.map((x) => Number(x.count) || 0);
    const labels = top.map((x) => x.allergie);
    return this.buildDonutChart(series, labels, 'Aucune allergie');
  }

  private buildDonutChart(series: number[], labels: string[], emptyLabel: string) {
    const cleanSeries = (series ?? []).map((n) => (Number.isFinite(Number(n)) ? Number(n) : 0));
    const hasAny = cleanSeries.some((n) => n > 0);

    const finalSeries: ApexNonAxisChartSeries = hasAny ? cleanSeries : [1];
    const finalLabels = hasAny ? labels : [emptyLabel];

    const chart: ApexChart = { type: 'donut', height: 260, toolbar: { show: false } };
    const legend: ApexLegend = { position: 'bottom' };
    const dataLabels: ApexDataLabels = { enabled: true };
    const plotOptions: ApexPlotOptions = { pie: { donut: { size: '62%' } } };
    const stroke: ApexStroke = { width: 2 };
    const theme: ApexTheme = { mode: 'light' };
    const responsive: ApexResponsive[] = [
      {
        breakpoint: 992,
        options: { chart: { height: 240 }, legend: { position: 'bottom' } }
      }
    ];

    return {
      series: finalSeries,
      labels: finalLabels,
      chart,
      legend,
      dataLabels,
      plotOptions,
      stroke,
      theme,
      responsive
    };
  }

  private titleCase(value: string): string {
    const s = (value ?? '').trim();
    if (!s) return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  private buildNiveauChart(groups: Array<{ label: string; enfants: Enfant[] }>) {
    const series = (groups ?? []).map((g) => (g?.enfants?.length ?? 0));
    const labels = (groups ?? []).map((g) => g.label);
    return this.buildDonutChart(series, labels, 'Aucun enfant');
  }

  private computeNiveauGroups(enfants: Enfant[]) {
    const groups: Array<{
      key: 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU';
      label: string;
      ageRangeLabel: string;
      enfants: Enfant[];
    }> = [
      { key: 'CRECHE', label: 'Crèche', ageRangeLabel: '0 à 3 ans', enfants: [] },
      { key: 'PRESCOLAIRE', label: 'Préscolaire', ageRangeLabel: '3 à 5 ans', enfants: [] },
      { key: 'PREPARATOIRE', label: 'Préparatoire', ageRangeLabel: '5 à 6 ans', enfants: [] },
      { key: 'HORS_NIVEAU', label: 'Hors niveau', ageRangeLabel: 'Autre', enfants: [] }
    ];

    for (const enfant of enfants ?? []) {
      const age = this.computeAgeYears(enfant?.dateNaissance);
      const key =
        age === null
          ? 'HORS_NIVEAU'
          : age < 3
            ? 'CRECHE'
            : age < 5
              ? 'PRESCOLAIRE'
              : age < 6
                ? 'PREPARATOIRE'
                : 'HORS_NIVEAU';

      const g = groups.find((x) => x.key === key);
      if (g) g.enfants.push(enfant);
    }

    // tri par prénom/nom pour lisibilité
    for (const g of groups) {
      g.enfants.sort((a, b) => {
        const ap = `${a?.prenom ?? ''} ${a?.nom ?? ''}`.trim();
        const bp = `${b?.prenom ?? ''} ${b?.nom ?? ''}`.trim();
        return ap.localeCompare(bp);
      });
    }

    return groups;
  }

  private computeAgeYears(dateNaissance: string | null | undefined): number | null {
    const raw = (dateNaissance ?? '').toString().trim();
    if (!raw) return null;
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return null;

    const now = new Date();
    let years = now.getFullYear() - d.getFullYear();
    const m = now.getMonth() - d.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < d.getDate())) {
      years--;
    }
    return Math.max(0, years);
  }
}
