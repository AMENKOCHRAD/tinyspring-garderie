import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
<<<<<<< HEAD
<<<<<<< HEAD
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../shared/auth.service';
import { Enfant, EnfantService } from '../services/enfant.service';
import { NotificationsService } from '../services/notifications.service';
import { TraitementHistoryComponent } from '../components/traitement-history.component';
import { AnimatriceDeclarerChangementComponent } from '../components/animatrice-declarer-changement.component';
import { AnimatriceSanteAlertesComponent } from '../components/animatrice-sante-alertes.component';
=======
import { AuthService } from '../shared/auth.service';
>>>>>>> origin/gestion-evenements
=======
import { AuthService } from '../shared/auth.service';
>>>>>>> origin/gestion_boutique

type AnimatorPageKey =
  | 'tableau-de-bord'
  | 'groupes'
  | 'sante'
  | 'activites'
  | 'messages'
  | 'formations'
  | 'planning';

interface PageMeta {
  chip: string;
  title: string;
  description: string;
}

const pageMetaMap: Record<AnimatorPageKey, PageMeta> = {
  'tableau-de-bord': {
    chip: 'Espace animateur',
    title: 'Bonjour, {{name}}',
    description: 'Une vue rapide de la journee pour suivre les groupes, la sante et les messages.'
  },
  groupes: {
    chip: 'Mes groupes',
    title: 'Classes et enfants du jour',
    description: 'Retrouvez les groupes, les capacites et la liste des enfants suivis.'
  },
  sante: {
    chip: 'Sante et alertes',
    title: 'Soins, allergies et incidents',
    description: 'Une section claire pour agir rapidement sur les points de sante importants.'
  },
  activites: {
    chip: 'Activites',
    title: 'Programme et menus',
    description: 'Le planning pedagogique et les reperes repas restent visibles ensemble.'
  },
  messages: {
    chip: 'Messages',
    title: 'Echanges avec les familles',
    description: 'Conservez une communication fluide avec les parents.'
  },
  formations: {
    chip: 'Formations',
    title: 'Progression et certifications',
    description: 'Retrouvez vos formations, le catalogue disponible et vos certificats.'
  },
  planning: {
    chip: 'Planning',
    title: 'Calendrier et absences',
    description: 'Consultez votre planning mensuel et preparez vos demandes de conge.'
  }
};

@Component({
  selector: 'app-animator-workspace-page',
  standalone: true,
<<<<<<< HEAD
<<<<<<< HEAD
  imports: [
    CommonModule,
    FormsModule,
    TraitementHistoryComponent,
    AnimatriceDeclarerChangementComponent,
    AnimatriceSanteAlertesComponent
  ],
=======
  imports: [CommonModule],
>>>>>>> origin/gestion-evenements
=======
  imports: [CommonModule],
>>>>>>> origin/gestion_boutique
  templateUrl: './animator-workspace-page.component.html',
  styleUrl: './animator-workspace-page.component.css'
})
export class AnimatorWorkspacePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
<<<<<<< HEAD
<<<<<<< HEAD
  private readonly enfantService = inject(EnfantService);
  private readonly notificationsService = inject(NotificationsService);
  private readonly dayLabelFormatter = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit'
  });
=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique

  protected readonly page = signal<AnimatorPageKey>('tableau-de-bord');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());
  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
    const firstName = this.authService.getCurrentUser()?.nom.split(' ')[0] ?? 'Animatrice';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

<<<<<<< HEAD
<<<<<<< HEAD
  errorMessage = '';
  successMessage = '';
  needsReauth = false;

  isLoadingSante = false;
  isSavingPrise = false;

  enfants: Enfant[] = [];
  enfantsAvecSante: Array<{ enfant: Enfant; conditions: ConditionSanitaireFront[]; prisesClefs: Set<string> }> = [];
  alertesDuJour: AlerteDose[] = [];
  planningDates: string[] = [];
  planningFromIso = this.startOfWeekIso(this.getTodayIso());
  planningToIso = this.addDaysIso(this.startOfWeekIso(this.getTodayIso()), 6);
  planningEventsByDate: Record<string, PlanningEvent[]> = {};
  observationsRecentes: ObservationFront[] = [];
  searchTerm = '';

  incidentsPage = 1;
  incidentsPageSize = 2;

  get filteredObservationsRecentesPagines(): ObservationFront[] {
    const start = (this.incidentsPage - 1) * this.incidentsPageSize;
    const end = start + this.incidentsPageSize;

    return this.filteredObservationsRecentes.slice(start, end);
  }

  get totalIncidentsPages(): number {
    return Math.ceil(this.filteredObservationsRecentes.length / this.incidentsPageSize);
  }

  goToIncidentsPage(page: number): void {
    if (page < 1 || page > this.totalIncidentsPages) {
      return;
    }

    this.incidentsPage = page;
  }

  nextIncidentsPage(): void {
    this.goToIncidentsPage(this.incidentsPage + 1);
  }

  previousIncidentsPage(): void {
    this.goToIncidentsPage(this.incidentsPage - 1);
  }

  getIncidentsPages(): number[] {
    return Array.from({ length: this.totalIncidentsPages }, (_, i) => i + 1);
  }

  resetIncidentsPage(): void {
    this.incidentsPage = 1;
  }


  santePage = 1;
  santePageSize = 2;

  get filteredEnfantsAvecSantePagines(): Array<{ enfant: Enfant; conditions: ConditionSanitaireFront[]; prisesClefs: Set<string> }> {
    const start = (this.santePage - 1) * this.santePageSize;
    const end = start + this.santePageSize;
    return this.filteredEnfantsAvecSante.slice(start, end);
  }

  get totalSantePages(): number {
    return Math.ceil(this.filteredEnfantsAvecSante.length / this.santePageSize);
  }

  goToSantePage(page: number): void {
    if (page < 1 || page > this.totalSantePages) {
      return;
    }
    this.santePage = page;
  }

  nextSantePage(): void {
    this.goToSantePage(this.santePage + 1);
  }

  previousSantePage(): void {
    this.goToSantePage(this.santePage - 1);
  }

  getSantePages(): number[] {
    return Array.from({ length: this.totalSantePages }, (_, i) => i + 1);
  }

  resetSantePage(): void {
    this.santePage = 1;
  }

  get notificationMessages(): string[] {
    return this.notificationsService.animatriceReminderMessages();
  }

  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as AnimatorPageKey);
      if (this.page() === 'sante') {
        this.chargerSante();
      }

      if (this.page() === 'groupes') {
        this.chargerEnfantsPourGroupes();
      }
    });
  }

  private getTodayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private addDaysIso(dateIso: string, days: number): string {
    const d = new Date(dateIso);
    if (isNaN(d.getTime())) {
      return this.getTodayIso();
    }
    d.setDate(d.getDate() + days);
    return d.toISOString().slice(0, 10);
  }

  private startOfWeekIso(dateIso: string): string {
    const d = new Date(dateIso);
    if (isNaN(d.getTime())) {
      return this.getTodayIso();
    }

    // Monday-based week (FR)
    const day = d.getDay(); // 0=Sun..6=Sat
    const diff = (day === 0 ? -6 : 1) - day;
    d.setDate(d.getDate() + diff);
    return d.toISOString().slice(0, 10);
  }

  private buildDateRange(fromIso: string, toIso: string): string[] {
    const start = new Date(fromIso);
    const end = new Date(toIso);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return [];
    }

    if (end.getTime() < start.getTime()) {
      return this.buildDateRange(toIso, fromIso);
    }

    const out: string[] = [];
    const cursor = new Date(start);
    while (cursor.getTime() <= end.getTime() && out.length < 7) {
      out.push(cursor.toISOString().slice(0, 10));
      cursor.setDate(cursor.getDate() + 1);
    }
    return out;
  }

  private keyPrise(traitementId: number, dateIso: string, heure: string): string {
    return `${traitementId}|${dateIso}|${heure}`;
  }

  chargerSante(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoadingSante = true;

    const todayIso = this.getTodayIso();

    forkJoin({
      enfants: this.enfantService.getAllEnfants().pipe(catchError(() => of([] as Enfant[]))),
      observations: this.enfantService.listerDernieresObservations().pipe(catchError(() => of([] as ObservationFront[])))
    }).subscribe({
      next: ({ enfants, observations }) => {
        const enfantsActifs = (enfants ?? []).filter((e) => !e.archive);
        this.enfants = enfantsActifs;
        this.observationsRecentes = (observations ?? []) as ObservationFront[];
        this.incidentsPage = 1;

        if (enfantsActifs.length === 0) {
          this.enfantsAvecSante = [];
          this.alertesDuJour = [];
          this.planningDates = [];
          this.planningEventsByDate = {};
          this.isLoadingSante = false;
          return;
        }

        const requetes = enfantsActifs.map((enfant) =>
          forkJoin({
            conditions: this.enfantService.getConditionsParEnfant(enfant.id).pipe(catchError(() => of([] as any[]))),
            prises: this.enfantService.getPrisesTraitementParEnfant(enfant.id, todayIso).pipe(catchError(() => of([] as any[])))
          }).pipe(
            switchMap(({ conditions, prises }) => {
              const priseKeys = new Set<string>();
              for (const p of prises as PriseTraitementFront[]) {
                if (p?.traitementId && p?.datePrise && p?.heurePrevue) {
                  priseKeys.add(this.keyPrise(p.traitementId, p.datePrise, p.heurePrevue));
                }
              }

              const baseConditions = (conditions ?? []) as ConditionSanitaireFront[];
              if (!baseConditions.length) {
                return of({ enfant, conditions: [] as ConditionSanitaireFront[], prisesClefs: priseKeys });
              }

              const traitementsReqs = baseConditions.map((c) =>
                this.enfantService.getTraitementsParCondition(c.id).pipe(
                  catchError(() => of([] as any[])),
                  map((traitements) => ({ ...c, traitements: (traitements ?? []) as TraitementFront[] }))
                )
              );

              return forkJoin(traitementsReqs).pipe(
                map((conditionsAvecTraitements) => ({
                  enfant,
                  conditions: conditionsAvecTraitements as ConditionSanitaireFront[],
                  prisesClefs: priseKeys
                }))
              );
            })
          )
        );

        forkJoin(requetes).subscribe({
          next: (rows) => {
            this.enfantsAvecSante = rows;
            this.santePage = 1;
            this.alertesDuJour = this.buildAlertesDuJour(todayIso);
            this.rebuildPlanning();
            this.isLoadingSante = false;
          },
          error: () => {
            this.errorMessage = 'Impossible de charger les informations de sante.';
            this.isLoadingSante = false;
          }
        });
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les informations de sante.';
        this.isLoadingSante = false;
      }
    });
  }


  refreshPlanning(): void {
    this.rebuildPlanning();
  }

  isFutureDate(dateIso: string): boolean {
    const d = new Date(dateIso);
    if (isNaN(d.getTime())) {
      return false;
    }
    const today = new Date(this.getTodayIso());
    return d.getTime() > today.getTime();
  }

  formatPlanningDayLabel(dateIso: string): string {
    const d = new Date(dateIso);
    if (isNaN(d.getTime())) {
      return dateIso;
    }
    return this.dayLabelFormatter.format(d);
  }

  goPlanningToday(): void {
    const start = this.startOfWeekIso(this.getTodayIso());
    this.planningFromIso = start;
    this.planningToIso = this.addDaysIso(start, 6);
    this.rebuildPlanning();
    this.selectedPlanningDate = this.getTodayIso();
  }

  goPlanningPrevWeek(): void {
    const start = this.addDaysIso(this.planningFromIso, -7);
    this.planningFromIso = this.startOfWeekIso(start);
    this.planningToIso = this.addDaysIso(this.planningFromIso, 6);
    this.rebuildPlanning();
    this.selectedPlanningDate = this.planningFromIso;
  }

  goPlanningNextWeek(): void {
    const start = this.addDaysIso(this.planningFromIso, 7);
    this.planningFromIso = this.startOfWeekIso(start);
    this.planningToIso = this.addDaysIso(this.planningFromIso, 6);
    this.rebuildPlanning();
    this.selectedPlanningDate = this.planningFromIso;
  }

  onPlanningStartChanged(): void {
    const start = this.startOfWeekIso(this.planningFromIso || this.getTodayIso());
    this.planningFromIso = start;
    this.planningToIso = this.addDaysIso(start, 6);
    this.rebuildPlanning();
  }

  private rebuildPlanning(): void {
    const fromIso = this.planningFromIso || this.getTodayIso();
    const toIso = this.planningToIso || fromIso;
    this.planningFromIso = fromIso;
    this.planningToIso = toIso;
    this.planningDates = this.buildDateRange(fromIso, toIso);

    if (!this.enfants?.length || this.planningDates.length === 0) {
      this.planningEventsByDate = {};
      return;
    }

    const enfantsActifs = (this.enfants ?? []).filter((e) => !e.archive);
    const from = this.planningDates[0];
    const to = this.planningDates[this.planningDates.length - 1];

    forkJoin(
      enfantsActifs.map((enfant) =>
        this.enfantService.getPrisesTraitementParEnfantPeriode(enfant.id, from, to).pipe(
          catchError(() => of([])),
          map((prises) => ({
            enfantId: enfant.id,
            prises: (prises ?? []) as PriseTraitementFront[]
          }))
        )
      )
    ).subscribe({
      next: (chunks) => {
        const prisesByEnfant: Record<number, Set<string>> = {};
        for (const c of chunks as any[]) {
          const set = new Set<string>();
          for (const p of (c?.prises ?? []) as PriseTraitementFront[]) {
            if (p?.traitementId && p?.datePrise && p?.heurePrevue) {
              set.add(this.keyPrise(p.traitementId, p.datePrise, p.heurePrevue));
            }
          }
          prisesByEnfant[c.enfantId] = set;
        }

        const byDate: Record<string, PlanningEvent[]> = {};
        for (const d of this.planningDates) {
          byDate[d] = [];
        }

        for (const row of this.enfantsAvecSante ?? []) {
          const enfantPrises = prisesByEnfant[row.enfant.id] ?? new Set<string>();
          for (const condition of row.conditions ?? []) {
            for (const traitement of condition.traitements ?? []) {
              if (!this.isTraitementValidePourCalendrier(traitement)) {
                continue;
              }
              if (!traitement?.heuresPrises?.length) {
                continue;
              }

              for (const dateIso of this.planningDates) {
                if (!this.isDateInTraitementRange(dateIso, traitement)) {
                  continue;
                }
                for (const heure of traitement.heuresPrises) {
                  const key = this.keyPrise(traitement.id, dateIso, heure);
                  byDate[dateIso].push({
                    dateIso,
                    heure,
                    dejaDonne: enfantPrises.has(key),
                    enfant: row.enfant,
                    condition,
                    traitement
                  });
                }
              }
            }
          }
        }

        for (const d of this.planningDates) {
          byDate[d] = (byDate[d] ?? []).sort((a, b) => {
            const t = (a.heure ?? '').localeCompare(b.heure ?? '');
            if (t !== 0) return t;
            const n1 = `${a.enfant?.prenom ?? ''} ${a.enfant?.nom ?? ''}`.trim();
            const n2 = `${b.enfant?.prenom ?? ''} ${b.enfant?.nom ?? ''}`.trim();
            return n1.localeCompare(n2);
          });
        }

        this.planningEventsByDate = byDate;
      },
      error: () => {
        // ignore planning errors
      }
    });
  }

  private isDateInTraitementRange(dateIso: string, traitement: TraitementFront): boolean {
    if (!dateIso || !traitement?.dateDebut) {
      return false;
    }
    const d = new Date(dateIso).getTime();
    const start = new Date(traitement.dateDebut).getTime();
    if (!isFinite(d) || !isFinite(start)) {
      return false;
    }
    if (d < start) {
      return false;
    }
    if (traitement.dateFin) {
      const end = new Date(traitement.dateFin).getTime();
      if (isFinite(end) && d > end) {
        return false;
      }
    }
    return true;
  }

  private chargerEnfantsPourGroupes(): void {
    if (this.enfants.length) {
      return;
    }

    this.enfantService.getAllEnfants().pipe(catchError(() => of([] as Enfant[]))).subscribe({
      next: (data) => {
        this.enfants = (data ?? []).filter((e) => !e.archive);
      }
    });
  }

  private buildAlertesDuJour(todayIso: string): AlerteDose[] {
    const alertes: AlerteDose[] = [];

    for (const row of this.enfantsAvecSante) {
      for (const condition of row.conditions) {
        for (const traitement of condition.traitements ?? []) {
          if (!this.isTraitementAdministrable(traitement)) {
            continue;
          }

          if (!traitement?.heuresPrises?.length) {
            continue;
          }

          for (const heure of traitement.heuresPrises) {
            const key = this.keyPrise(traitement.id, todayIso, heure);
            alertes.push({
              enfant: row.enfant,
              traitement,
              condition,
              heure,
              dejaDonne: row.prisesClefs.has(key)
            });
          }
        }
      }
    }

    return alertes.sort((a, b) => a.heure.localeCompare(b.heure));
  }

  private isTraitementAdministrable(traitement: TraitementFront | null | undefined): boolean {
    const statut = traitement?.statut;
    return statut === 'VALIDE' || statut === 'ACTIF';
  }

  private isTraitementValidePourCalendrier(traitement: TraitementFront | null | undefined): boolean {
    return traitement?.statut === 'VALIDE';
  }

  getGroupeLabel(enfant: Enfant): string {
    const age = this.getAgeYears(enfant?.dateNaissance);
    if (age !== null && age < 3) {
      return 'Tournesols';
    }
    return 'Papillons';
  }

  getGroupCount(label: string): number {
    return (this.enfants ?? []).filter((e) => this.getGroupeLabel(e) === label).length;
  }

  private getAgeYears(dateIso?: string): number | null {
    if (!dateIso) {
      return null;
    }

    const birth = new Date(dateIso);
    if (isNaN(birth.getTime())) {
      return null;
    }

    const now = new Date();
    let years = now.getFullYear() - birth.getFullYear();
    const m = now.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) {
      years--;
    }
    return years;
  }

  onObservationError(message: string): void {
    this.errorMessage = message || '';
  }

  onObservationSuccess(message: string): void {
    this.successMessage = message || '';
  }

  onObservationNeedsReauth(needs: boolean): void {
    this.needsReauth = !!needs;
  }

  onObservationSent(): void {
    this.refreshObservationsRecentes();
  }

  private refreshObservationsRecentes(): void {
    this.enfantService
      .listerDernieresObservations()
      .pipe(catchError(() => of([] as ObservationFront[])))
      .subscribe({
        next: (data) => {
          this.observationsRecentes = (data ?? []) as ObservationFront[];
          this.incidentsPage = 1;
        }
      });
  }

  requestBrowserNotifications(): void {
    if (!('Notification' in window)) {
      this.errorMessage = "Les notifications navigateur ne sont pas supportees sur cet appareil.";
      return;
    }

    void Notification.requestPermission();
  }

  marquerDonne(traitementId: number, enfantId: number, heurePrevue: string, dateIso?: string): void {
    if (this.isSavingPrise) {
      return;
    }

    const traitement = this.findTraitementById(traitementId);
    if (traitement && !this.isTraitementAdministrable(traitement)) {
      this.errorMessage = 'Vous ne pouvez enregistrer une prise que pour un traitement valide par l admin.';
      return;
    }

    const targetDate = dateIso || this.getTodayIso();
    if (this.isFutureDate(targetDate)) {
      this.errorMessage = 'Impossible d enregistrer une prise dans le futur.';
      return;
    }

    const restriction = this.checkWeekendDoseRestrictions(targetDate, heurePrevue);
    if (restriction) {
      this.errorMessage = restriction;
      return;
    }

    const confirme = window.confirm(`Confirmer: traitement donne le ${targetDate} a ${heurePrevue} ?`);
    if (!confirme) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.isSavingPrise = true;

    const payload = { datePrise: targetDate, heurePrevue };

    this.enfantService.enregistrerPriseTraitement(traitementId, payload).subscribe({
      next: (created) => {
        this.successMessage = 'Prise enregistree.';
        this.isSavingPrise = false;
        const priseId = created?.id as number | undefined;
        if (priseId) {
          this.telechargerJustificatifPrise(priseId);
        }

        // refresh prises for this enfant on the current planning range
        const fromIso = this.planningFromIso || this.getTodayIso();
        const toIso = this.planningToIso || fromIso;

        this.enfantService.getPrisesTraitementParEnfantPeriode(enfantId, fromIso, toIso).subscribe({
          next: (prises) => {
            const row = this.enfantsAvecSante.find((r) => r.enfant.id === enfantId);
            if (row) {
              row.prisesClefs.clear();
              for (const p of prises as PriseTraitementFront[]) {
                if (p?.traitementId && p?.datePrise && p?.heurePrevue) {
                  row.prisesClefs.add(this.keyPrise(p.traitementId, p.datePrise, p.heurePrevue));
                }
              }
              this.alertesDuJour = this.buildAlertesDuJour(this.getTodayIso());
              this.rebuildPlanning();
            }
          },
          error: () => {
            // ignore refresh error
          }
        });
      },
      error: (err) => {
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        const details = err?.error?.message || err?.error || err?.message || '';
        this.errorMessage = `Impossible d'enregistrer la prise${status}. ${details}`.trim();
        this.isSavingPrise = false;
      }
    });
  }

  private checkWeekendDoseRestrictions(dateIso: string, heure: string): string | null {
    // Dimanche: interdit
    // Samedi: autorisé uniquement jusqu'à 12:30 (inclus)
    try {
      const d = new Date(`${dateIso}T00:00:00`);
      const dow = d.getDay(); // 0=Sunday, 6=Saturday
      if (dow === 0) {
        return 'Aucune prise ne peut etre enregistree le dimanche.';
      }
      if (dow === 6) {
        const parts = String(heure || '').split(':');
        if (parts.length >= 2) {
          const hh = Number(parts[0]);
          const mm = Number(parts[1]);
          if (!Number.isNaN(hh) && !Number.isNaN(mm)) {
            if (hh > 12 || (hh === 12 && mm > 30)) {
              return "Le samedi, les prises sont autorisees uniquement jusqu'a 12:30.";
            }
          }
        }
      }
    } catch {
      // ignore parse errors
    }
    return null;
  }

  private telechargerJustificatifPrise(priseId: number): void {
    this.enfantService.telechargerJustificatifPrise(priseId).subscribe({
      next: (blob) => {
        if (!blob) {
          return;
        }

        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `prise-traitement-${priseId}.pdf`;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        this.extractHttpErrorDetails(err, (details) => {
          this.errorMessage = `Prise enregistree mais impossible de telecharger le PDF${status}. ${details}`.trim();
        });
      }
    });
  }

  private extractHttpErrorDetails(err: any, cb: (details: string) => void): void {
    const fallback = err?.error?.message || err?.message || 'Erreur inconnue.';
    const raw = err?.error;

    if (raw instanceof Blob) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const text = String(reader.result ?? '');
          const parsed = JSON.parse(text);
          cb(parsed?.message || text || fallback);
        } catch {
          cb(String(reader.result ?? '') || fallback);
        }
      };
      reader.onerror = () => cb(fallback);
      reader.readAsText(raw);
      return;
    }

    cb(typeof raw === 'string' ? raw : fallback);
  }

  private findTraitementById(traitementId: number): TraitementFront | null {
    for (const row of this.enfantsAvecSante ?? []) {
      for (const condition of row.conditions ?? []) {
        const match = (condition.traitements ?? []).find((t) => t?.id === traitementId);
        if (match) {
          return match;
        }
      }
    }
    return null;
  }

  goToLogin(): void {
    window.location.href = '/connexion';
  }

  labelObservationType(type: string): string {
    switch (type) {
      case 'SANTE':
        return 'Sante';
      case 'COMPORTEMENT':
        return 'Comportement';
      case 'SOMMEIL':
        return 'Sommeil';
      case 'ALIMENTATION':
        return 'Alimentation';
      case 'INCIDENT':
        return 'Incident';
      case 'HUMEUR':
        return 'Humeur';
      case 'HYGIENE':
        return 'Hygiene';
      case 'AUTRE':
        return 'Autre';
      default:
        return type || 'Autre';
    }
  }

  hasAllergiesRenseignees(): boolean {
    return (this.enfants ?? []).some((enfant) => !!enfant?.allergies?.trim());
  }

  get filteredEnfantsAvecSante(): Array<{ enfant: Enfant; conditions: ConditionSanitaireFront[]; prisesClefs: Set<string> }> {
    const term = this.normalizeSearch(this.searchTerm);
    if (!term) {
      return this.enfantsAvecSante;
    }

    return (this.enfantsAvecSante ?? []).filter((row) => {
      const child = `${row.enfant?.prenom ?? ''} ${row.enfant?.nom ?? ''}`;
      const conditions = (row.conditions ?? []).map((c) => c?.nomCondition ?? '').join(' ');
      const traitements = (row.conditions ?? [])
        .flatMap((c) => c.traitements ?? [])
        .map((t) => t?.nomTraitement ?? '')
        .join(' ');

      const hay = `${child} ${conditions} ${traitements}`;
      return this.normalizeSearch(hay).includes(term);
    });
  }

  get filteredObservationsRecentes(): ObservationFront[] {
    const term = this.normalizeSearch(this.searchTerm);
    if (!term) {
      return this.observationsRecentes;
    }

    return (this.observationsRecentes ?? []).filter((obs) => {
      const hay = [
        `${obs.enfantPrenom ?? ''} ${obs.enfantNom ?? ''}`,
        obs.type ?? '',
        obs.titre ?? '',
        obs.description ?? ''
      ].join(' ');
      return this.normalizeSearch(hay).includes(term);
    });
  }

  private normalizeSearch(value: string): string {
    return (value ?? '')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim();
  }

  // =========================
  // Stats (Sante & alertes)
  // =========================
  get statsEnfantsCount(): number {
    return (this.enfants ?? []).filter((e) => !e.archive).length;
  }

  get statsTraitementsActifsCount(): number {
    const today = this.getTodayIso();
    let count = 0;
    for (const row of this.enfantsAvecSante ?? []) {
      for (const condition of row.conditions ?? []) {
        for (const traitement of condition.traitements ?? []) {
          if (!this.isTraitementAdministrable(traitement)) {
            continue;
          }
          if (!this.isDateInTraitementRange(today, traitement)) {
            continue;
          }
          count++;
        }
      }
    }
    return count;
  }

  get statsDosesTodayTotal(): number {
    return (this.alertesDuJour ?? []).length;
  }

  get statsDosesTodayDone(): number {
    return (this.alertesDuJour ?? []).filter((a) => a.dejaDonne).length;
  }

  get statsDosesTodayRemaining(): number {
    return Math.max(0, this.statsDosesTodayTotal - this.statsDosesTodayDone);
  }

  get statsDosesLate(): number {
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    const current = `${hh}:${mm}`;
    return (this.alertesDuJour ?? []).filter((a) => !a.dejaDonne && (a.heure ?? '') < current).length;
  }

  get statsObservationsRecentesCount(): number {
    return (this.observationsRecentes ?? []).length;
  }

  get statsIncidentsRecentCount(): number {
    return (this.observationsRecentes ?? []).filter((o) => (o.type ?? '') === 'INCIDENT').length;
  }
  selectedPlanningDate = this.getTodayIso();

get selectedPlanningEvents(): PlanningEvent[] {
  return this.planningEventsByDate[this.selectedPlanningDate] || [];
}
selectPlanningDate(dateIso: string): void {
  this.selectedPlanningDate = dateIso;
  this.planningEventPage = 1;
}
planningEventPage = 1;
planningEventPageSize = 5;

get selectedPlanningEventsPagines(): PlanningEvent[] {
  const start = (this.planningEventPage - 1) * this.planningEventPageSize;
  const end = start + this.planningEventPageSize;

  return this.selectedPlanningEvents.slice(start, end);
}

get totalPlanningEventPages(): number {
  return Math.ceil(this.selectedPlanningEvents.length / this.planningEventPageSize);
}

goToPlanningEventPage(page: number): void {
  if (page < 1 || page > this.totalPlanningEventPages) {
    return;
  }

  this.planningEventPage = page;
}

nextPlanningEventPage(): void {
  this.goToPlanningEventPage(this.planningEventPage + 1);
}

previousPlanningEventPage(): void {
  this.goToPlanningEventPage(this.planningEventPage - 1);
}

getPlanningEventPages(): number[] {
  return Array.from({ length: this.totalPlanningEventPages }, (_, i) => i + 1);
}
}

interface ConditionSanitaireFront {
  id: number;
  nomCondition: string;
  type: 'MALADIE_CHRONIQUE' | 'MALADIE_TEMPORAIRE';
  description: string;
  dateDebut: string;
  dateFin?: string | null;
  traitements?: TraitementFront[];
}

interface TraitementFront {
  id: number;
  nomTraitement: string;
  description: string;
  ordonnance: string;
  dateDebut: string;
  dateFin?: string | null;
  heuresPrises: string[];
  statut: 'EN_ATTENTE_VALIDATION' | 'VALIDE' | 'REFUSE' | 'ACTIF' | 'ANNULE';
}

interface AlerteDose {
  enfant: Enfant;
  condition: ConditionSanitaireFront;
  traitement: TraitementFront;
  heure: string;
  dejaDonne: boolean;
}

interface ObservationFront {
  id: number;
  type: string;
  titre: string;
  description: string;
  creeLe: string;
  enfantId: number;
  enfantNom: string;
  enfantPrenom: string;
  creeParNom: string;
  urgence?: string | null;
  temperature?: number | null;
  lieu?: string | null;
}

interface PriseTraitementFront {
  id: number;
  datePrise: string;
  heurePrevue: string;
  traitementId: number;
}

interface PlanningEvent {
  dateIso: string;
  heure: string;
  dejaDonne: boolean;
  enfant: Enfant;
  condition: ConditionSanitaireFront;
  traitement: TraitementFront;
}


=======
=======
>>>>>>> origin/gestion_boutique
  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as AnimatorPageKey);
    });
  }
}
<<<<<<< HEAD
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
