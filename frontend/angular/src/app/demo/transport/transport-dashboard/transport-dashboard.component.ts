import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AffectationService } from 'src/app/services/transport/affectation.service';
import { DemandeService } from 'src/app/services/transport/demande.service';
import {
  AdminDemandPredictionResponse,
  AffectationTransport,
  DemandeAffectationRecommendation,
  DemandeTransport,
  NouveauTrajetRecommendation,
  StatutTransport,
  TrajetItem,
  TrajetPayload,
  TransportItem,
  TransportPayload
} from 'src/app/services/transport/transport.models';
import { TrajetService } from 'src/app/services/transport/trajet.service';
import { TransportRecommendationService } from 'src/app/services/transport/transport-recommendation.service';
import { TransportService } from 'src/app/services/transport/transport.service';
import { LocationMapPickerComponent, PickedLocation } from './location-map-picker.component';
import { noEdgeSpacesValidator, trimmedRequiredValidator } from './transport-form.validators';

interface DemandeStatCard {
  label: string;
  total: number;
  icon: string;
  cardClass: string;
}

type TrajetSense = 'MAISON_VERS_GARDERIE' | 'GARDERIE_VERS_MAISON';

@Component({
  selector: 'app-transport-dashboard',
  imports: [SharedModule, LocationMapPickerComponent],
  templateUrl: './transport-dashboard.component.html',
  styleUrls: ['./transport-dashboard.component.scss']
})
export class TransportDashboardComponent implements OnInit {
  activeTabId = 1;
  readonly tomorrowDate = this.getTomorrowDate();
  readonly adresseGarderieFixe = '15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie';
  readonly garderieLatitude = 36.8065;
  readonly garderieLongitude = 10.1815;
  trajetAutrePointLatitude: number | null = null;
  trajetAutrePointLongitude: number | null = null;

  transports: TransportItem[] = [];
  trajets: TrajetItem[] = [];
  demandes: DemandeTransport[] = [];
  affectations: AffectationTransport[] = [];
  recommandationsAffectation: DemandeAffectationRecommendation[] = [];
  recommandationsNouveauxTrajets: NouveauTrajetRecommendation[] = [];
  predictionDemande: AdminDemandPredictionResponse | null = null;

  loading = {
    transports: false,
    trajets: false,
    demandes: false,
    affectations: false,
    recommandations: false
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
    sensTrajet: ['MAISON_VERS_GARDERIE' as TrajetSense, [Validators.required]],
    autrePoint: [
      '',
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(3),
        Validators.maxLength(80),
        Validators.pattern(/^[\p{L}0-9\s\-()',]+$/u)
      ]
    ],
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
      this.adresseGarderieFixe,
      [
        Validators.required,
        trimmedRequiredValidator(),
        noEdgeSpacesValidator(),
        Validators.minLength(3),
        Validators.maxLength(80),
        Validators.pattern(/^[A-Za-zÀ-ÿ0-9\s\-()',]+$/)
      ]
    ],
    latitudeAutrePoint: [null as number | null],
    longitudeAutrePoint: [null as number | null],
    zoneDesservie: ['', [Validators.maxLength(120)]],
    latitudeDestination: [this.garderieLatitude as number | null],
    longitudeDestination: [this.garderieLongitude as number | null],
    dateTrajet: [this.tomorrowDate, [Validators.required]],
    heureDepart: ['', [Validators.required]],
    transportId: [null as number | null, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly transportService: TransportService,
    private readonly trajetService: TrajetService,
    private readonly demandeService: DemandeService,
    private readonly affectationService: AffectationService,
    private readonly transportRecommendationService: TransportRecommendationService
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
      const matchesHeure = this.filtreHeure ? !!demande.heureDepart && demande.heureDepart.startsWith(this.filtreHeure) : true;
      return matchesTrajet && matchesHeure;
    });
  }

  loadAllData(): void {
    this.setAllLoading(true);
    this.clearMessages();

    forkJoin({
      transports: this.transportService.getTransports(),
      trajets: this.trajetService.getTrajets(),
      demandes: this.demandeService.getDemandes(),
      affectations: this.affectationService.getAffectations(),
      recommandationsAffectation: this.transportRecommendationService.getAffectationRecommendations(),
      recommandationsNouveauxTrajets: this.transportRecommendationService.getNewRouteRecommendations(),
      predictionDemande: this.transportRecommendationService.getDemandPrediction(this.tomorrowDate, 8)
    }).subscribe({
      next: ({ transports, trajets, demandes, affectations, recommandationsAffectation, recommandationsNouveauxTrajets, predictionDemande }) => {
        this.transports = transports;
        this.trajets = trajets;
        this.demandes = demandes;
        this.affectations = affectations;
        this.recommandationsAffectation = recommandationsAffectation;
        this.recommandationsNouveauxTrajets = recommandationsNouveauxTrajets;
        this.predictionDemande = predictionDemande;
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

    const payload = this.buildTrajetPayload();
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
        this.reloadRecommendations();
      },
      error: (error) => {
        this.loading.trajets = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible d enregistrer le trajet.');
      }
    });
  }

  editTrajet(trajet: TrajetItem): void {
    this.editingTrajetId = trajet.id;
    const sensTrajet = this.inferTrajetSense(trajet);
    const autrePoint = sensTrajet === 'GARDERIE_VERS_MAISON' ? trajet.destination : trajet.pointDepart;
    const autrePointLatitude = sensTrajet === 'GARDERIE_VERS_MAISON' ? trajet.latitudeDestination : null;
    const autrePointLongitude = sensTrajet === 'GARDERIE_VERS_MAISON' ? trajet.longitudeDestination : null;

    this.trajetAutrePointLatitude = autrePointLatitude;
    this.trajetAutrePointLongitude = autrePointLongitude;
    this.trajetForm.patchValue({
      sensTrajet,
      autrePoint,
      pointDepart: trajet.pointDepart,
      destination: trajet.destination,
      latitudeAutrePoint: autrePointLatitude,
      longitudeAutrePoint: autrePointLongitude,
      zoneDesservie: trajet.zoneDesservie,
      latitudeDestination: trajet.latitudeDestination,
      longitudeDestination: trajet.longitudeDestination,
      dateTrajet: trajet.dateTrajet,
      heureDepart: trajet.heureDepart,
      transportId: trajet.transportId
    });
    this.syncTrajetEndpoints();
    this.activeTabId = 2;
  }

  onTrajetSenseChanged(): void {
    this.syncTrajetEndpoints();
  }

  onTrajetOtherLocationSelected(location: PickedLocation): void {
    this.trajetAutrePointLatitude = location.latitude;
    this.trajetAutrePointLongitude = location.longitude;
    this.trajetForm.patchValue({
      autrePoint: location.address,
      latitudeAutrePoint: location.latitude,
      longitudeAutrePoint: location.longitude
    });
    this.trajetForm.get('autrePoint')?.markAsDirty();
    this.trajetForm.get('autrePoint')?.markAsTouched();
    this.syncTrajetEndpoints();
  }

  clearTrajetOtherLocation(): void {
    this.trajetAutrePointLatitude = null;
    this.trajetAutrePointLongitude = null;
    this.trajetForm.patchValue({
      autrePoint: '',
      latitudeAutrePoint: null,
      longitudeAutrePoint: null
    });
    this.trajetForm.get('autrePoint')?.markAsTouched();
    this.syncTrajetEndpoints();
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
        this.reloadRecommendations();
      },
      error: (error) => {
        this.loading.trajets = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de supprimer le trajet.');
      }
    });
  }

  accepterDemande(demande: DemandeTransport): void {
    this.actionDemandeId = demande.id;
    this.clearMessages();

    this.demandeService.accepterDemande(demande.id).subscribe({
      next: () => {
        this.messages.success = `La demande de ${demande.enfantNomComplet} a ete acceptee avec affectation automatique.`;
        this.actionDemandeId = null;
        this.reloadDemandes();
        this.reloadAffectations();
        this.reloadTransports();
        this.reloadTrajets();
        this.reloadRecommendations();
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
        this.reloadRecommendations();
      },
      error: (error) => {
        this.actionDemandeId = null;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de refuser la demande.');
      }
    });
  }

  supprimerDemande(demande: DemandeTransport): void {
    const confirmationMessage =
      demande.statut === 'ACCEPTEE'
        ? 'Cette demande est deja affectee. La supprimer retirera aussi son affectation transport. Continuer ?'
        : 'Supprimer cette demande de transport ?';

    if (!window.confirm(confirmationMessage)) {
      return;
    }

    this.actionDemandeId = demande.id;
    this.clearMessages();

    this.demandeService.supprimerDemande(demande.id).subscribe({
      next: () => {
        this.messages.success =
          demande.statut === 'ACCEPTEE'
            ? `La demande de ${demande.enfantNomComplet} et son affectation ont ete supprimees.`
            : `La demande de ${demande.enfantNomComplet} a ete supprimee.`;
        this.actionDemandeId = null;
        this.reloadDemandes();
        this.reloadAffectations();
        this.reloadTransports();
        this.reloadTrajets();
        this.reloadRecommendations();
      },
      error: (error) => {
        this.actionDemandeId = null;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de supprimer la demande.');
      }
    });
  }

  resetTransportForm(): void {
    this.editingTransportId = null;
    this.transportForm.reset({ nom: '', matricule: '', capacite: 1 });
  }

  resetTrajetForm(): void {
    this.editingTrajetId = null;
    this.trajetAutrePointLatitude = null;
    this.trajetAutrePointLongitude = null;
    this.trajetForm.reset({
      sensTrajet: 'MAISON_VERS_GARDERIE',
      autrePoint: '',
      pointDepart: '',
      destination: this.adresseGarderieFixe,
      latitudeAutrePoint: null,
      longitudeAutrePoint: null,
      zoneDesservie: '',
      latitudeDestination: this.garderieLatitude,
      longitudeDestination: this.garderieLongitude,
      dateTrajet: this.tomorrowDate,
      heureDepart: '',
      transportId: null
    });
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

  getAnomalyBadgeClass(demande: DemandeTransport): string {
    if (demande.aiAnalysisAvailable === false) {
      return 'badge-light-secondary';
    }

    if (demande.suspicious) {
      return 'badge-light-danger';
    }

    return 'badge-light-info';
  }

  formatAiStatus(demande: DemandeTransport): string {
    if (demande.aiAnalysisAvailable === false) {
      return 'Analyse IA indisponible';
    }

    return demande.suspicious ? 'Demande suspecte' : 'Demande analysee';
  }

  formatAnomalyScore(score: number | null | undefined): string {
    return score == null ? 'N/A' : score.toFixed(3);
  }

  hasTransportError(
    field: 'nom' | 'matricule' | 'capacite',
    error: 'required' | 'trimmedRequired' | 'edgeSpaces' | 'min' | 'max' | 'minlength' | 'maxlength' | 'pattern'
  ): boolean {
    const control = this.transportForm.get(field);
    return !!control && control.hasError(error) && (control.touched || control.dirty);
  }

  hasTrajetError(
    field: 'sensTrajet' | 'autrePoint' | 'pointDepart' | 'destination' | 'zoneDesservie' | 'dateTrajet' | 'heureDepart' | 'transportId',
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

  private reloadRecommendations(): void {
    this.loading.recommandations = true;
    forkJoin({
      affectations: this.transportRecommendationService.getAffectationRecommendations(),
      nouveauxTrajets: this.transportRecommendationService.getNewRouteRecommendations(),
      predictionDemande: this.transportRecommendationService.getDemandPrediction(this.tomorrowDate, 8)
    }).subscribe({
      next: ({ affectations, nouveauxTrajets, predictionDemande }) => {
        this.recommandationsAffectation = affectations;
        this.recommandationsNouveauxTrajets = nouveauxTrajets;
        this.predictionDemande = predictionDemande;
        this.loading.recommandations = false;
      },
      error: (error) => {
        this.loading.recommandations = false;
        this.messages.error = this.extractErrorMessage(error, 'Impossible de recharger les recommandations.');
      }
    });
  }

  private setAllLoading(value: boolean): void {
    this.loading = {
      transports: value,
      trajets: value,
      demandes: value,
      affectations: value,
      recommandations: value
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

  getTrajetDepartLabel(): string {
    return this.trajetForm.controls.pointDepart.value || 'Adresse non definie pour le moment.';
  }

  getTrajetArriveeLabel(): string {
    return this.trajetForm.controls.destination.value || 'Adresse non definie pour le moment.';
  }

  getTrajetOtherPointLabel(): string {
    return this.trajetForm.controls.autrePoint.value || 'Aucun autre point selectionne pour le moment.';
  }

  getTrajetOtherPointTitle(): string {
    return this.trajetForm.controls.sensTrajet.value === 'GARDERIE_VERS_MAISON' ? 'Maison / destination' : 'Maison / point de depart';
  }

  private buildTrajetPayload(): TrajetPayload {
    this.syncTrajetEndpoints();
    const payload = this.trajetForm.getRawValue();

    return this.normalizeTrajetPayload({
      pointDepart: payload.pointDepart ?? '',
      destination: payload.destination ?? '',
      zoneDesservie: payload.zoneDesservie,
      latitudeDestination: payload.latitudeDestination,
      longitudeDestination: payload.longitudeDestination,
      dateTrajet: payload.dateTrajet ?? this.tomorrowDate,
      heureDepart: payload.heureDepart ?? '',
      transportId: payload.transportId as number
    });
  }

  private normalizeTrajetPayload(payload: TrajetPayload): TrajetPayload {
    return {
      ...payload,
      pointDepart: payload.pointDepart.trim(),
      destination: payload.destination.trim(),
      zoneDesservie: payload.zoneDesservie?.trim() ? payload.zoneDesservie.trim() : null,
      latitudeDestination: payload.latitudeDestination ?? null,
      longitudeDestination: payload.longitudeDestination ?? null
    };
  }

  private syncTrajetEndpoints(): void {
    const sensTrajet = this.trajetForm.controls.sensTrajet.value ?? 'MAISON_VERS_GARDERIE';
    const autrePoint = this.trajetForm.controls.autrePoint.value?.trim() ?? '';
    const latitudeAutrePoint = this.trajetForm.controls.latitudeAutrePoint.value ?? null;
    const longitudeAutrePoint = this.trajetForm.controls.longitudeAutrePoint.value ?? null;

    if (sensTrajet === 'GARDERIE_VERS_MAISON') {
      this.trajetForm.patchValue(
        {
          pointDepart: this.adresseGarderieFixe,
          destination: autrePoint,
          latitudeDestination: latitudeAutrePoint,
          longitudeDestination: longitudeAutrePoint
        },
        { emitEvent: false }
      );
      return;
    }

    this.trajetForm.patchValue(
      {
        pointDepart: autrePoint,
        destination: this.adresseGarderieFixe,
        latitudeDestination: this.garderieLatitude,
        longitudeDestination: this.garderieLongitude
      },
      { emitEvent: false }
    );
  }

  private inferTrajetSense(trajet: TrajetItem): TrajetSense {
    return this.isGarderieAddress(trajet.pointDepart) ? 'GARDERIE_VERS_MAISON' : 'MAISON_VERS_GARDERIE';
  }

  private isGarderieAddress(address: string | null | undefined): boolean {
    return (address ?? '').trim().toLowerCase() === this.adresseGarderieFixe.trim().toLowerCase();
  }

  formatRecommendationDistance(distanceKm: number | null): string {
    return distanceKm == null ? 'N/A' : `${distanceKm.toFixed(2)} km`;
  }

  formatRecommendationStatus(item: DemandeAffectationRecommendation): string {
    return item.affectationAutomatiquePossible ? 'Affectable automatiquement' : 'Validation manuelle requise';
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
