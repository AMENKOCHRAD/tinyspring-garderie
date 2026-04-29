import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { interval } from 'rxjs';
import Swal from 'sweetalert2';

import { LocationMapPickerComponent, PickedLocation } from '../components/location-map-picker.component';
import { AuthService } from '../shared/auth.service';
import { ChildrenService } from '../shared/children.service';
import { DemandesService } from '../shared/demandes.service';
import { DemandeTransport, ParentChild, SensTrajet } from '../shared/transport.models';

type ParentPageKey =
  | 'tableau-de-bord'
  | 'enfants'
  | 'sante'
  | 'activites'
  | 'menus'
  | 'messages'
  | 'trajets'
  | 'boutique';

interface PageMeta {
  chip: string;
  title: string;
  description: string;
}

const pageMetaMap: Record<ParentPageKey, PageMeta> = {
  'tableau-de-bord': {
    chip: 'Espace parent',
    title: 'Bonjour, {{name}}',
    description: 'Un resume rapide de la semaine pour suivre les enfants, les messages et les alertes.'
  },
  enfants: {
    chip: 'Enfants',
    title: 'Profils enfants et documents',
    description: 'Retrouvez les profils, les documents et les informations principales de vos enfants.'
  },
  sante: {
    chip: 'Sante',
    title: 'Suivi sante et incidents',
    description: 'Centralisez allergies, traitements, incidents et informations a ajouter.'
  },
  activites: {
    chip: 'Activites',
    title: 'Calendrier et evenements',
    description: 'Consultez les activites a venir, les sorties et les autorisations.'
  },
  menus: {
    chip: 'Menus',
    title: 'Menus hebdomadaires',
    description: 'Suivez les repas et les reperes mensuels dans un espace dedie.'
  },
  messages: {
    chip: 'Messages',
    title: 'Conversations avec la garderie',
    description: 'Retrouvez les echanges importants avec l equipe dans une interface simple.'
  },
  trajets: {
    chip: 'Trajets',
    title: 'Organisation des trajets',
    description: 'Consultez vos demandes de transport et envoyez un nouveau besoin.'
  },
  boutique: {
    chip: 'Boutique',
    title: 'Catalogue et commandes',
    description: 'Parcourez les articles utiles et suivez votre panier puis vos commandes.'
  }
};

@Component({
  selector: 'app-parent-workspace-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LocationMapPickerComponent],
  templateUrl: './parent-workspace-page.component.html',
  styleUrl: './parent-workspace-page.component.css'
})
export class ParentWorkspacePageComponent {
  private readonly demandeNotificationStorageKey = 'parent-transport-demandes-notifications-v1';
  private readonly demandePollIntervalMs = 30000;
  private readonly adresseGarderieFixe = '15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie';
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly childrenService = inject(ChildrenService);
  private readonly demandesService = inject(DemandesService);

  protected readonly page = signal<ParentPageKey>('tableau-de-bord');
  protected readonly children = signal<ParentChild[]>([]);
  protected readonly demandes = signal<DemandeTransport[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly message = signal('');
  protected readonly hasError = signal(false);
  protected readonly adresseMaisonSelectionnee = signal('');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
    const firstName = this.authService.getCurrentUser()?.nom.split(' ')[0] ?? 'Parent';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });
  protected readonly pendingDemandesCount = computed(
    () => this.demandes().filter((demande) => demande.statut === 'EN_ATTENTE' || demande.statut === 'REVISION_PARENT_DEMANDEE').length
  );
  protected readonly acceptedDemandesCount = computed(
    () => this.demandes().filter((demande) => demande.statut === 'ACCEPTEE').length
  );
  protected readonly childrenWithTransportCount = computed(
    () => new Set(this.demandes().filter((demande) => demande.statut !== 'REFUSEE').map((demande) => demande.enfantId)).size
  );
  protected readonly tomorrowDate = this.getTomorrowDate();
  protected readonly adresseGarderie = computed(
    () => this.demandes()[0]?.adresseGarderie || this.adresseGarderieFixe
  );
  protected readonly form = this.fb.nonNullable.group({
    enfantId: [0, [Validators.required, Validators.min(1)]],
    sensTrajet: ['MAISON_VERS_GARDERIE' as SensTrajet, [Validators.required]],
    adresseMaison: ['', [Validators.required, Validators.minLength(8)]],
    latitudeMaison: [0, [Validators.required]],
    longitudeMaison: [0, [Validators.required]],
    dateSouhaitee: [this.getTomorrowDate(), [Validators.required]],
    heureSouhaitee: ['07:30', [Validators.required]]
  });

  public constructor() {
    this.loadChildren();
    this.loadDemandes();
    this.startDemandesPolling();

    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as ParentPageKey);
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();
    const request$ = this.editingId()
      ? this.demandesService.update(this.editingId()!, payload)
      : this.demandesService.create(payload);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        const isEditing = this.editingId() !== null;
        this.hasError.set(false);
        this.message.set(isEditing ? 'Demande mise a jour.' : 'Demande creee.');
        this.showSuccessNotification(
          isEditing ? 'Modification envoyee' : 'Demande envoyee',
          isEditing
            ? 'La demande a ete mise a jour avec succes. L admin verra la modification.'
            : 'La demande a ete enregistree avec succes. L admin sera notifie.'
        );
        this.resetForm();
        this.loadDemandes();
      },
      error: (error) => {
        this.hasError.set(true);
        this.message.set(this.extractErrorMessage(error));
      }
    });
  }

  protected edit(demande: DemandeTransport): void {
    this.editingId.set(demande.id);
    this.message.set('');
    this.adresseMaisonSelectionnee.set(demande.adresseMaison);
    this.form.patchValue({
      enfantId: demande.enfantId,
      sensTrajet: demande.sensTrajet,
      adresseMaison: demande.adresseMaison,
      latitudeMaison: demande.latitudeMaison,
      longitudeMaison: demande.longitudeMaison,
      dateSouhaitee: demande.dateSouhaitee,
      heureSouhaitee: demande.heureSouhaitee.slice(0, 5)
    });
  }

  protected remove(id: number): void {
    const demande = this.demandes().find((item) => item.id === id);
    const confirmationMessage =
      demande?.statut === 'ACCEPTEE'
        ? 'Cette demande est deja affectee. La supprimer retirera aussi son affectation transport. Continuer ?'
        : 'Supprimer cette demande de transport ?';

    if (!window.confirm(confirmationMessage)) {
      return;
    }

    this.demandesService
      .delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.hasError.set(false);
          this.message.set(
            demande?.statut === 'ACCEPTEE'
              ? 'Demande et affectation supprimees.'
              : 'Demande supprimee.'
          );
          if (this.editingId() === id) {
            this.resetForm();
          }
          this.loadDemandes();
        },
        error: (error) => {
          this.hasError.set(true);
          this.message.set(this.extractErrorMessage(error, 'Suppression impossible.'));
        }
      });
  }

  protected resetForm(): void {
    this.editingId.set(null);
    this.adresseMaisonSelectionnee.set('');
    this.form.reset({
      enfantId: 0,
      sensTrajet: 'MAISON_VERS_GARDERIE',
      adresseMaison: '',
      latitudeMaison: 0,
      longitudeMaison: 0,
      dateSouhaitee: this.getTomorrowDate(),
      heureSouhaitee: '07:30'
    });
  }

  protected isFieldInvalid(fieldName: 'enfantId' | 'adresseMaison' | 'dateSouhaitee' | 'heureSouhaitee'): boolean {
    const control = this.form.controls[fieldName];
    return control.invalid && control.touched;
  }

  protected selectedChildHasActiveRequest(): boolean {
    if (this.editingId()) {
      return false;
    }

    const enfantId = this.form.controls.enfantId.value;
    if (!enfantId || enfantId < 1) {
      return false;
    }

    return this.demandes().some(
      (demande) =>
        demande.enfantId === enfantId &&
        (demande.statut === 'EN_ATTENTE' || demande.statut === 'REVISION_PARENT_DEMANDEE' || demande.statut === 'ACCEPTEE')
    );
  }

  protected formatStatus(status: string): string {
    return status.replace('_', ' ');
  }

  protected getSensLabel(sens: SensTrajet): string {
    return sens === 'MAISON_VERS_GARDERIE' ? 'Maison -> Garderie' : 'Garderie -> Maison';
  }

  protected getDepartLabel(): string {
    return this.form.controls.sensTrajet.value === 'GARDERIE_VERS_MAISON'
      ? this.adresseGarderie()
      : this.adresseMaisonSelectionnee() || 'Maison a selectionner sur la carte';
  }

  protected getArriveeLabel(): string {
    return this.form.controls.sensTrajet.value === 'GARDERIE_VERS_MAISON'
      ? this.adresseMaisonSelectionnee() || 'Maison a selectionner sur la carte'
      : this.adresseGarderie();
  }

  protected getStatusClass(status: string): string {
    switch (status) {
      case 'ACCEPTEE':
        return 'tag tag--mint';
      case 'REVISION_PARENT_DEMANDEE':
        return 'tag tag--lime';
      case 'REFUSEE':
        return 'tag tag--blush';
      default:
        return 'tag tag--lime';
    }
  }

  protected childInitials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  protected onLocationSelected(location: PickedLocation): void {
    this.adresseMaisonSelectionnee.set(location.address);
    this.form.patchValue({
      adresseMaison: location.address,
      latitudeMaison: location.latitude,
      longitudeMaison: location.longitude
    });
    this.form.controls.adresseMaison.markAsTouched();
  }

  protected formatAnomalyScore(score: number | null): string {
    return score == null ? 'N/A' : score.toFixed(3);
  }

  protected getAiStatusLabel(demande: DemandeTransport): string {
    if (!demande.aiAnalysisAvailable) {
      return 'IA indisponible';
    }
    return demande.suspicious ? 'Demande suspecte' : 'Demande normale';
  }

  protected getAiStatusClass(demande: DemandeTransport): string {
    if (!demande.aiAnalysisAvailable) {
      return 'tag tag--blush';
    }
    return demande.suspicious ? 'tag tag--blush' : 'tag tag--mint';
  }

  protected getAiAnalysisLabel(demande: DemandeTransport): string {
    if (!demande.aiAnalysisAvailable) {
      return 'Analyse indisponible pour le moment.';
    }

    const parts = [`Demande ${demande.suspicious ? 'suspecte' : 'normale'}`];

    if (demande.anomalyLevel && demande.anomalyLevel !== 'UNKNOWN') {
      parts.push(`Niveau: ${demande.anomalyLevel}`);
    }

    if (demande.anomalyScore !== null) {
      parts.push(`Score: ${this.formatAnomalyScore(demande.anomalyScore)}`);
    }

    return parts.join(' | ');
  }

  private loadChildren(): void {
    this.childrenService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (children) => this.children.set(children),
        error: (error) => {
          this.hasError.set(true);
          this.message.set(this.extractErrorMessage(error, 'Chargement des enfants impossible.'));
        }
      });
  }

  private loadDemandes(): void {
    this.demandesService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (demandes) => {
          const normalizedDemandes = demandes.map((demande) =>
            demande.aiAnalysisAvailable
              ? demande
              : {
                  ...demande,
                  anomalyLevel: null,
                  anomalyScore: null,
                  anomalyReasons: []
                }
          );

          this.demandes.set(normalizedDemandes);
          this.notifyParentOnStatusChanges(normalizedDemandes);
        },
        error: (error) => {
          this.hasError.set(true);
          this.message.set(this.extractErrorMessage(error, 'Chargement des demandes impossible.'));
        }
      });
  }

  private extractErrorMessage(error: any, fallback = 'Operation impossible.'): string {
    if (Array.isArray(error?.error?.details) && error.error.details.length > 0) {
      return error.error.details[0];
    }

    return error?.error?.message ?? (typeof error?.error === 'string' ? error.error : fallback);
  }

  private getTomorrowDate(): string {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toISOString().split('T')[0];
  }

  private startDemandesPolling(): void {
    interval(this.demandePollIntervalMs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadDemandes());
  }

  private notifyParentOnStatusChanges(demandes: DemandeTransport[]): void {
    const previousStatuses = this.readStoredDemandesStatuses();
    const currentStatuses: Record<number, string> = {};
    const accepted: string[] = [];
    const refused: string[] = [];
    const revisions: string[] = [];

    for (const demande of demandes) {
      currentStatuses[demande.id] = demande.statut;
      const previousStatus = previousStatuses[demande.id];

      if (previousStatus === demande.statut) {
        continue;
      }

      if (demande.statut === 'ACCEPTEE') {
        accepted.push(demande.enfantNomComplet);
      } else if (demande.statut === 'REFUSEE') {
        refused.push(demande.enfantNomComplet);
      } else if (demande.statut === 'REVISION_PARENT_DEMANDEE') {
        revisions.push(demande.enfantNomComplet);
      }
    }

    this.storeDemandesStatuses(currentStatuses);

    if (accepted.length > 0) {
      this.showSuccessNotification(
        accepted.length > 1 ? 'Demandes acceptees' : 'Demande acceptee',
        this.buildChildrenMessage(accepted, 'a ete acceptee par l admin.')
      );
    }

    if (refused.length > 0) {
      void Swal.fire({
        icon: 'error',
        title: refused.length > 1 ? 'Demandes refusees' : 'Demande refusee',
        text: this.buildChildrenMessage(refused, 'a ete refusee par l admin.'),
        confirmButtonText: 'Compris'
      });
    }

    if (revisions.length > 0) {
      void Swal.fire({
        icon: 'warning',
        title: revisions.length > 1 ? 'Revisions demandees' : 'Revision demandee',
        text: this.buildChildrenMessage(revisions, 'necessite une revision de votre part.'),
        confirmButtonText: 'D accord'
      });
    }
  }

  private readStoredDemandesStatuses(): Record<number, string> {
    try {
      const rawValue = localStorage.getItem(this.demandeNotificationStorageKey);
      return rawValue ? JSON.parse(rawValue) as Record<number, string> : {};
    } catch {
      return {};
    }
  }

  private storeDemandesStatuses(statuses: Record<number, string>): void {
    localStorage.setItem(this.demandeNotificationStorageKey, JSON.stringify(statuses));
  }

  private showSuccessNotification(title: string, text: string): void {
    void Swal.fire({
      icon: 'success',
      title,
      text,
      timer: 2600,
      showConfirmButton: false,
      toast: true,
      position: 'top-end'
    });
  }

  private buildChildrenMessage(childrenNames: string[], suffix: string): string {
    if (childrenNames.length === 1) {
      return `${childrenNames[0]} ${suffix}`;
    }

    return `${childrenNames.length} demandes sont concernees: ${childrenNames.join(', ')}.`;
  }
}
