import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AffectationService } from 'src/app/services/transport/affectation.service';
import { DemandeService } from 'src/app/services/transport/demande.service';
import {
  AffectationTransport,
  DemandeTransport,
  StatutTransport,
  TrajetItem,
  TrajetPayload,
  TransportItem,
  TransportPayload
} from 'src/app/services/transport/transport.models';
import { TrajetService } from 'src/app/services/transport/trajet.service';
import { TransportService } from 'src/app/services/transport/transport.service';
import { noEdgeSpacesValidator, trimmedRequiredValidator } from './transport-form.validators';

interface DemandeStatCard {
  label: string;
  total: number;
  icon: string;
  cardClass: string;
}

@Component({
  selector: 'app-transport-dashboard',
  imports: [SharedModule],
  templateUrl: './transport-dashboard.component.html',
  styleUrls: ['./transport-dashboard.component.scss']
})
export class TransportDashboardComponent implements OnInit {
  activeTabId = 1;
  readonly tomorrowDate = this.getTomorrowDate();

  transports: TransportItem[] = [];
  trajets: TrajetItem[] = [];
  demandes: DemandeTransport[] = [];
  affectations: AffectationTransport[] = [];

  loading = {
    transports: false,
    trajets: false,
    demandes: false,
    affectations: false
  };

  messages = {
    success: '',
    error: ''
  };

  editingTransportId: number | null = null;
  editingTrajetId: number | null = null;
  actionDemandeId: number | null = null;

  filtreTrajetId: number | null = null;
  filtreHeure = '';

  readonly acceptControls: Record<number, FormControl<number | null>> = {};

  readonly transportForm = this.fb.group({
    nom: [
      '',
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(3),
        Validators.maxLength(60),
        Validators.pattern(/^[A-Za-zÀ-ÿ0-9\s\-()]+$/)
      ]
    ],
    matricule: [
      '',
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(4),
        Validators.maxLength(20),
        Validators.pattern(/^[A-Za-z0-9\-]+$/)
      ]
    ],
    capacite: [1, [Validators.required, Validators.min(1), Validators.max(100)]]
  });

  readonly trajetForm = this.fb.group({
    pointDepart: [
      '',
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(3),
        Validators.maxLength(80),
        Validators.pattern(/^[A-Za-zÀ-ÿ0-9\s\-()',]+$/)
      ]
    ],
    destination: [
      '',
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(3),
        Validators.maxLength(80),
        Validators.pattern(/^[A-Za-zÀ-ÿ0-9\s\-()',]+$/)
      ]
    ],
    dateTrajet: [this.tomorrowDate, [Validators.required]],
    heureDepart: ['', [Validators.required]],
    transportId: [null as number | null, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly transportService: TransportService,
    private readonly trajetService: TrajetService,
    private readonly demandeService: DemandeService,
    private readonly affectationService: AffectationService
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  get demandeStatsCards(): DemandeStatCard[] {
    return [
      {
        label: 'Demandes en attente',
        total: this.countDemandesByStatut('EN_ATTENTE'),
        icon: 'feather icon-clock',
        cardClass: 'bg-c-yellow'
      },
      {
        label: 'Demandes acceptees',
        total: this.countDemandesByStatut('ACCEPTEE'),
        icon: 'feather icon-check-circle',
        cardClass: 'bg-c-green'
      },
      {
        label: 'Demandes refusees',
        total: this.countDemandesByStatut('REFUSEE'),
        icon: 'feather icon-x-circle',
        cardClass: 'bg-c-red'
      }
    ];
  }

  get filteredDemandes(): DemandeTransport[] {
    return this.demandes.filter((demande) => {
      const matchesTrajet = this.filtreTrajetId ? demande.trajetId === this.filtreTrajetId : true;
      const matchesHeure = this.filtreHeure ? demande.heureDepart.startsWith(this.filtreHeure) : true;
      return matchesTrajet && matchesHeure;
    });
  }

  getTransportControl(demandeId: number): FormControl<number | null> {
    if (!this.acceptControls[demandeId]) {
      this.acceptControls[demandeId] = new FormControl<number | null>(null, {
        nonNullable: false,
        validators: [Validators.required]
      });
    }

    return this.acceptControls[demandeId];
  }

  loadAllData(): void {
    this.setAllLoading(true);
    this.clearMessages();

    forkJoin({
      transports: this.transportService.getTransports(),
      trajets: this.trajetService.getTrajets(),
      demandes: this.demandeService.getDemandes(),
      affectations: this.affectationService.getAffectations()
    }).subscribe({
      next: ({ transports, trajets, demandes, affectations }) => {
        this.transports = transports;
        this.trajets = trajets;
        this.demandes = demandes;
        this.affectations = affectations;
        this.setAllLoading(false);
      },
      error: (error) => {
        this.setAllLoading(false);
        this.messages.error = this.extractErrorMessage(error, 'Impossible de charger le module transport.');
      }
    });
  }

  submitTransport(): void {
    if (this.transportForm.invalid) {
      this.transportForm.markAllAsTouched();
      return;
    }

    const payload = this.normalizeTransportPayload(this.transportForm.getRawValue() as TransportPayload);
    this.loading.transports = true;
    this.clearMessages();

    const request$ = this.editingTransportId
      ? this.transportService.updateTransport(this.editingTransportId, payload)
      : this.transportService.createTransport(payload);

    request$.subscribe({
      next: () => {
        this.messages.success = this.editingTransportId
          ? 'Le transport a ete modifie avec succes.'
          : 'Le transport a ete ajoute avec succes.';
        this.resetTransportForm();
        this.reloadTransports();
      },
      error: (error) => {
        this.loading.transports = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible d enregistrer le transport.');
      }
    });
  }

  editTransport(transport: TransportItem): void {
    this.editingTransportId = transport.id;
    this.transportForm.patchValue({
      nom: transport.nom,
      matricule: transport.matricule,
      capacite: transport.capacite
    });
    this.activeTabId = 1;
  }

  deleteTransport(id: number): void {
    if (!window.confirm('Supprimer ce transport ?')) {
      return;
    }

    this.loading.transports = true;
    this.clearMessages();

    this.transportService.deleteTransport(id).subscribe({
      next: () => {
        this.messages.success = 'Le transport a ete supprime.';
        this.resetTransportForm();
        this.reloadTransports();
        this.reloadTrajets();
      },
      error: (error) => {
        this.loading.transports = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de supprimer le transport.');
      }
    });
  }

  submitTrajet(): void {
    if (this.trajetForm.invalid) {
      this.trajetForm.markAllAsTouched();
      return;
    }

    const payload = this.normalizeTrajetPayload(this.trajetForm.getRawValue() as TrajetPayload);
    this.loading.trajets = true;
    this.clearMessages();

    const request$ = this.editingTrajetId
      ? this.trajetService.updateTrajet(this.editingTrajetId, payload)
      : this.trajetService.createTrajet(payload);

    request$.subscribe({
      next: () => {
        this.messages.success = this.editingTrajetId
          ? 'Le trajet a ete modifie avec succes.'
          : 'Le trajet a ete ajoute avec succes.';
        this.resetTrajetForm();
        this.reloadTrajets();
      },
      error: (error) => {
        this.loading.trajets = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible d enregistrer le trajet.');
      }
    });
  }

  editTrajet(trajet: TrajetItem): void {
    this.editingTrajetId = trajet.id;
    this.trajetForm.patchValue({
      pointDepart: trajet.pointDepart,
      destination: trajet.destination,
      dateTrajet: trajet.dateTrajet,
      heureDepart: trajet.heureDepart,
      transportId: trajet.transportId
    });
    this.activeTabId = 2;
  }

  deleteTrajet(id: number): void {
    if (!window.confirm('Supprimer ce trajet ?')) {
      return;
    }

    this.loading.trajets = true;
    this.clearMessages();

    this.trajetService.deleteTrajet(id).subscribe({
      next: () => {
        this.messages.success = 'Le trajet a ete supprime.';
        this.resetTrajetForm();
        this.reloadTrajets();
      },
      error: (error) => {
        this.loading.trajets = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de supprimer le trajet.');
      }
    });
  }

  accepterDemande(demande: DemandeTransport): void {
    const control = this.getTransportControl(demande.id);
    if (control.invalid || control.value === null) {
      control.markAsTouched();
      return;
    }

    this.actionDemandeId = demande.id;
    this.clearMessages();

    this.demandeService.accepterDemande(demande.id, control.value).subscribe({
      next: () => {
        this.messages.success = `La demande de ${demande.enfantNomComplet} a ete acceptee.`;
        this.actionDemandeId = null;
        this.reloadDemandes();
        this.reloadAffectations();
        this.reloadTransports();
      },
      error: (error) => {
        this.actionDemandeId = null;
        this.messages.error = this.extractErrorMessage(error, 'Impossible d accepter la demande.');
      }
    });
  }

  refuserDemande(demande: DemandeTransport): void {
    this.actionDemandeId = demande.id;
    this.clearMessages();

    this.demandeService.refuserDemande(demande.id).subscribe({
      next: () => {
        this.messages.success = `La demande de ${demande.enfantNomComplet} a ete refusee.`;
        this.actionDemandeId = null;
        this.reloadDemandes();
      },
      error: (error) => {
        this.actionDemandeId = null;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de refuser la demande.');
      }
    });
  }

  resetTransportForm(): void {
    this.editingTransportId = null;
    this.transportForm.reset({ nom: '', matricule: '', capacite: 1 });
  }

  resetTrajetForm(): void {
    this.editingTrajetId = null;
    this.trajetForm.reset({ pointDepart: '', destination: '', dateTrajet: this.tomorrowDate, heureDepart: '', transportId: null });
  }

  getStatutBadgeClass(statut: StatutTransport): string {
    switch (statut) {
      case 'ACCEPTEE':
        return 'badge-light-success';
      case 'REFUSEE':
        return 'badge-light-danger';
      default:
        return 'badge-light-warning';
    }
  }

  hasTransportError(
    field: 'nom' | 'matricule' | 'capacite',
    error: 'required' | 'trimmedRequired' | 'edgeSpaces' | 'min' | 'max' | 'minlength' | 'maxlength' | 'pattern'
  ): boolean {
    const control = this.transportForm.get(field);
    return !!control && control.hasError(error) && (control.touched || control.dirty);
  }

  hasTrajetError(
    field: 'pointDepart' | 'destination' | 'dateTrajet' | 'heureDepart' | 'transportId',
    error: 'required' | 'trimmedRequired' | 'edgeSpaces' | 'minlength' | 'maxlength' | 'pattern' = 'required'
  ): boolean {
    const control = this.trajetForm.get(field);
    return !!control && control.hasError(error) && (control.touched || control.dirty);
  }

  private reloadTransports(): void {
    this.loading.transports = true;
    this.transportService.getTransports().subscribe({
      next: (transports) => {
        this.transports = transports;
        this.loading.transports = false;
      },
      error: (error) => {
        this.loading.transports = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de recharger les transports.');
      }
    });
  }

  private reloadTrajets(): void {
    this.loading.trajets = true;
    this.trajetService.getTrajets().subscribe({
      next: (trajets) => {
        this.trajets = trajets;
        this.loading.trajets = false;
      },
      error: (error) => {
        this.loading.trajets = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de recharger les trajets.');
      }
    });
  }

  private reloadDemandes(): void {
    this.loading.demandes = true;
    this.demandeService.getDemandes().subscribe({
      next: (demandes) => {
        this.demandes = demandes;
        this.loading.demandes = false;
      },
      error: (error) => {
        this.loading.demandes = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de recharger les demandes.');
      }
    });
  }

  private reloadAffectations(): void {
    this.loading.affectations = true;
    this.affectationService.getAffectations().subscribe({
      next: (affectations) => {
        this.affectations = affectations;
        this.loading.affectations = false;
      },
      error: (error) => {
        this.loading.affectations = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de recharger les affectations.');
      }
    });
  }

  private setAllLoading(value: boolean): void {
    this.loading = {
      transports: value,
      trajets: value,
      demandes: value,
      affectations: value
    };
  }

  private clearMessages(): void {
    this.messages.success = '';
    this.messages.error = '';
  }

  private normalizeTransportPayload(payload: TransportPayload): TransportPayload {
    return {
      ...payload,
      nom: payload.nom.trim(),
      matricule: payload.matricule.trim().toUpperCase()
    };
  }

  private normalizeTrajetPayload(payload: TrajetPayload): TrajetPayload {
    return {
      ...payload,
      pointDepart: payload.pointDepart.trim(),
      destination: payload.destination.trim()
    };
  }

  private countDemandesByStatut(statut: StatutTransport): number {
    return this.demandes.filter((demande) => demande.statut === statut).length;
  }

  private getTomorrowDate(): string {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toISOString().split('T')[0];
  }

  private extractErrorMessage(error: unknown, fallbackMessage: string): string {
    if (error instanceof HttpErrorResponse) {
      const details = error.error?.details;

      if (Array.isArray(details) && details.length > 0) {
        return details.join(' ');
      }

      if (typeof error.error === 'string' && error.error.trim().length > 0) {
        return error.error;
      }
    }

    return fallbackMessage;
  }
}
