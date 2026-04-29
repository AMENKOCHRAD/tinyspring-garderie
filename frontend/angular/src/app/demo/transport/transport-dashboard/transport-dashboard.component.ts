import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { forkJoin, interval } from 'rxjs';
import Swal from 'sweetalert2';

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
type DemandeFilterStatut = StatutTransport | 'TOUS';
type DemandeFilterSuspect = 'TOUS' | 'SUSPECTES' | 'NON_SUSPECTES' | 'IA_INDISPONIBLE';

interface DemandeFiltersState {
  searchTerm: string;
  statut: DemandeFilterStatut;
  date: string;
  trajetId: number | null;
  transportId: number | null;
  suspect: DemandeFilterSuspect;
  heure: string;
  pageSize: number;
}

@Component({
  selector: 'app-transport-dashboard',
  imports: [SharedModule, LocationMapPickerComponent],
  templateUrl: './transport-dashboard.component.html',
  styleUrls: ['./transport-dashboard.component.scss']
})
export class TransportDashboardComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly adminDemandeNotificationStorageKey = 'admin-transport-demandes-notifications-v1';
  private readonly demandeFilterStorageKey = 'admin-transport-demandes-filters-v1';
  private readonly liveRefreshIntervalMs = 10000;
  activeTabId = 1;
  readonly tomorrowDate = this.getTomorrowDate();
  readonly adresseGarderieFixe = '15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie';
  readonly garderieLatitude = 36.8065;
  readonly garderieLongitude = 10.1815;
  readonly pageSizeOptions = [5];
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

  demandeSearchTerm = '';
  filtreStatut: DemandeFilterStatut = 'TOUS';
  filtreDate = '';
  filtreTrajetId: number | null = null;
  filtreTransportId: number | null = null;
  filtreSuspect: DemandeFilterSuspect = 'TOUS';
  filtreHeure = '';
  demandesPageSize = 5;
  currentDemandesPage = 1;
  lastRefreshAt: Date | null = null;

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
        Validators.maxLength(200),
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
        Validators.maxLength(200),
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
        Validators.maxLength(200),
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
    this.restoreDemandeFilters();
    this.loadAllData();
    this.startLiveRefresh();
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
        label: 'Revisions parent',
        total: this.countDemandesByStatut('REVISION_PARENT_DEMANDEE'),
        icon: 'feather icon-alert-triangle',
        cardClass: 'bg-c-blue'
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
      const searchHaystack = [
        demande.enfantNomComplet,
        demande.parentNom,
        demande.pointDepart,
        demande.destination,
        demande.pointRamassage,
        demande.statut,
        demande.dateTrajet ?? '',
        demande.dateSouhaitee ?? ''
      ]
        .join(' ')
        .toLowerCase();
      const normalizedSearchTerm = this.demandeSearchTerm.trim().toLowerCase();
      const transport = demande.trajetId ? this.findTransportByTrajetId(demande.trajetId) : null;

      const matchesSearch = normalizedSearchTerm ? searchHaystack.includes(normalizedSearchTerm) : true;
      const matchesStatut = this.filtreStatut === 'TOUS' ? true : demande.statut === this.filtreStatut;
      const matchesDate = this.filtreDate ? (demande.dateTrajet ?? demande.dateSouhaitee ?? '') === this.filtreDate : true;
      const matchesTrajet = this.filtreTrajetId ? demande.trajetId === this.filtreTrajetId : true;
      const matchesTransport = this.filtreTransportId ? transport?.id === this.filtreTransportId : true;
      const matchesSuspect =
        this.filtreSuspect === 'TOUS'
          ? true
          : this.filtreSuspect === 'SUSPECTES'
            ? !!demande.suspicious
            : this.filtreSuspect === 'NON_SUSPECTES'
              ? !demande.suspicious
              : demande.aiAnalysisAvailable === false;
      const matchesHeure = this.filtreHeure ? !!demande.heureDepart && demande.heureDepart.startsWith(this.filtreHeure) : true;

      return matchesSearch && matchesStatut && matchesDate && matchesTrajet && matchesTransport && matchesSuspect && matchesHeure;
    });
  }

  get paginatedDemandes(): DemandeTransport[] {
    const startIndex = (this.currentDemandesPage - 1) * this.demandesPageSize;
    return this.filteredDemandes.slice(startIndex, startIndex + this.demandesPageSize);
  }

  get totalDemandesPages(): number {
    return Math.max(1, Math.ceil(this.filteredDemandes.length / this.demandesPageSize));
  }

  get visibleDemandesStart(): number {
    if (this.filteredDemandes.length === 0) {
      return 0;
    }
    return (this.currentDemandesPage - 1) * this.demandesPageSize + 1;
  }

  get visibleDemandesEnd(): number {
    return Math.min(this.currentDemandesPage * this.demandesPageSize, this.filteredDemandes.length);
  }

  get pendingDemandesCount(): number {
    return this.countDemandesByStatut('EN_ATTENTE');
  }

  get suspiciousDemandesCount(): number {
    return this.demandes.filter((demande) => !!demande.suspicious).length;
  }

  get unavailableAiDemandesCount(): number {
    return this.demandes.filter((demande) => demande.aiAnalysisAvailable === false).length;
  }

  get activeDemandesFiltersCount(): number {
    return [
      this.demandeSearchTerm.trim(),
      this.filtreStatut !== 'TOUS' ? this.filtreStatut : '',
      this.filtreDate,
      this.filtreTrajetId,
      this.filtreTransportId,
      this.filtreSuspect !== 'TOUS' ? this.filtreSuspect : '',
      this.filtreHeure
    ].filter(Boolean).length;
  }

  get demandesPageNumbers(): number[] {
    return Array.from({ length: this.totalDemandesPages }, (_, index) => index + 1);
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
        this.notifyAdminOnPendingDemandes(demandes);
        this.affectations = affectations;
        this.recommandationsAffectation = recommandationsAffectation;
        this.recommandationsNouveauxTrajets = recommandationsNouveauxTrajets;
        this.predictionDemande = predictionDemande;
        this.lastRefreshAt = new Date();
        this.ensureDemandesPaginationInBounds();
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
    const autrePointLatitude = trajet.latitudeDestination;
    const autrePointLongitude = trajet.longitudeDestination;

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
      next: (response) => {
        this.messages.success =
          response.statut === 'REVISION_PARENT_DEMANDEE'
            ? `Une revision a ete demandee au parent de ${demande.enfantNomComplet}.`
            : `La demande de ${demande.enfantNomComplet} a ete acceptee avec affectation automatique.`;
        void Swal.fire({
          icon: response.statut === 'REVISION_PARENT_DEMANDEE' ? 'warning' : 'success',
          title: response.statut === 'REVISION_PARENT_DEMANDEE' ? 'Revision demandee' : 'Demande acceptee',
          text:
            response.statut === 'REVISION_PARENT_DEMANDEE'
              ? `Le parent de ${demande.enfantNomComplet} a ete notifie pour revoir la demande.`
              : `${demande.enfantNomComplet} a ete affecte(e) automatiquement au transport.`,
          timer: 2600,
          showConfirmButton: false,
          toast: true,
          position: 'top-end'
        });
        this.actionDemandeId = null;
        this.reloadDemandes();
        if (response.statut === 'ACCEPTEE') {
          this.reloadAffectations();
          this.reloadTransports();
          this.reloadTrajets();
        }
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
        void Swal.fire({
          icon: 'error',
          title: 'Demande refusee',
          text: `Le parent de ${demande.enfantNomComplet} a ete notifie du refus.`,
          timer: 2600,
          showConfirmButton: false,
          toast: true,
          position: 'top-end'
        });
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

  onDemandesFiltersChanged(): void {
    this.currentDemandesPage = 1;
    this.ensureDemandesPaginationInBounds();
    this.persistDemandeFilters();
  }

  onDemandesPageSizeChanged(): void {
    this.currentDemandesPage = 1;
    this.ensureDemandesPaginationInBounds();
    this.persistDemandeFilters();
  }

  resetDemandesFilters(): void {
    this.demandeSearchTerm = '';
    this.filtreStatut = 'TOUS';
    this.filtreDate = '';
    this.filtreTrajetId = null;
    this.filtreTransportId = null;
    this.filtreSuspect = 'TOUS';
    this.filtreHeure = '';
    this.demandesPageSize = 5;
    this.currentDemandesPage = 1;
    this.persistDemandeFilters();
  }

  goToDemandesPage(page: number): void {
    this.currentDemandesPage = Math.min(Math.max(page, 1), this.totalDemandesPages);
    this.persistDemandeFilters();
  }

  formatStatutLabel(statut: StatutTransport): string {
    switch (statut) {
      case 'EN_ATTENTE':
        return 'En attente';
      case 'ACCEPTEE':
        return 'Acceptee';
      case 'REVISION_PARENT_DEMANDEE':
        return 'Revision';
      case 'REFUSEE':
        return 'Refusee';
      default:
        return statut;
    }
  }

  getStatutBadgeClass(statut: StatutTransport): string {
    switch (statut) {
      case 'ACCEPTEE':
        return 'status-badge status-badge--accepted';
      case 'REVISION_PARENT_DEMANDEE':
        return 'status-badge status-badge--revision';
      case 'REFUSEE':
        return 'status-badge status-badge--rejected';
      default:
        return 'status-badge status-badge--pending';
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
        this.lastRefreshAt = new Date();
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
        this.lastRefreshAt = new Date();
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
        this.notifyAdminOnPendingDemandes(demandes);
        this.lastRefreshAt = new Date();
        this.ensureDemandesPaginationInBounds();
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
        this.lastRefreshAt = new Date();
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
        this.lastRefreshAt = new Date();
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
        latitudeDestination: latitudeAutrePoint,
        longitudeDestination: longitudeAutrePoint
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

  private startLiveRefresh(): void {
    interval(this.liveRefreshIntervalMs)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refreshLiveData());
  }

  private getTomorrowDate(): string {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toISOString().split('T')[0];
  }

  private notifyAdminOnPendingDemandes(demandes: DemandeTransport[]): void {
    const previousSignatures = this.readStoredDemandeSignatures();
    const currentSignatures: Record<number, string> = {};
    const createdDemandes: string[] = [];
    const updatedDemandes: string[] = [];

    for (const demande of demandes) {
      const currentSignature = this.buildAdminDemandeSignature(demande);
      currentSignatures[demande.id] = currentSignature;

      if (demande.statut !== 'EN_ATTENTE') {
        continue;
      }

      const previousSignature = previousSignatures[demande.id];

      if (!previousSignature) {
        createdDemandes.push(demande.enfantNomComplet);
        continue;
      }

      if (previousSignature !== currentSignature) {
        updatedDemandes.push(demande.enfantNomComplet);
      }
    }

    this.storeDemandeSignatures(currentSignatures);

    if (createdDemandes.length > 0) {
      this.showAdminNotification(
        'Nouvelle demande',
        this.buildAdminDemandesMessage(createdDemandes, 'nouvelle demande en attente')
      );
    }

    if (updatedDemandes.length > 0) {
      this.showAdminNotification(
        'Demande modifiee',
        this.buildAdminDemandesMessage(updatedDemandes, 'demande modifiee a verifier')
      );
    }
  }

  private buildAdminDemandeSignature(demande: DemandeTransport): string {
    return [
      demande.statut,
      demande.enfantNomComplet,
      demande.dateSouhaitee ?? '',
      demande.heureSouhaitee ?? '',
      demande.adresseMaison ?? '',
      demande.sensTrajet ?? ''
    ].join('|');
  }

  private readStoredDemandeSignatures(): Record<number, string> {
    try {
      const rawValue = localStorage.getItem(this.adminDemandeNotificationStorageKey);
      return rawValue ? (JSON.parse(rawValue) as Record<number, string>) : {};
    } catch {
      return {};
    }
  }

  private storeDemandeSignatures(signatures: Record<number, string>): void {
    localStorage.setItem(this.adminDemandeNotificationStorageKey, JSON.stringify(signatures));
  }

  private showAdminNotification(title: string, text: string): void {
    void Swal.fire({
      icon: 'info',
      title,
      text,
      timer: 2800,
      showConfirmButton: false,
      toast: true,
      position: 'top-end'
    });
  }

  private buildAdminDemandesMessage(childrenNames: string[], suffix: string): string {
    if (childrenNames.length === 1) {
      return `${childrenNames[0]} a une ${suffix}.`;
    }

    return `${childrenNames.length} demandes sont concernees: ${childrenNames.join(', ')}.`;
  }

  private refreshLiveData(): void {
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
        this.notifyAdminOnPendingDemandes(demandes);
        this.affectations = affectations;
        this.recommandationsAffectation = recommandationsAffectation;
        this.recommandationsNouveauxTrajets = recommandationsNouveauxTrajets;
        this.predictionDemande = predictionDemande;
        this.lastRefreshAt = new Date();
        this.ensureDemandesPaginationInBounds();
      }
    });
  }

  private findTransportByTrajetId(trajetId: number | null): TransportItem | null {
    if (!trajetId) {
      return null;
    }

    const trajet = this.trajets.find((item) => item.id === trajetId);
    if (!trajet?.transportId) {
      return null;
    }

    return this.transports.find((item) => item.id === trajet.transportId) ?? null;
  }

  private ensureDemandesPaginationInBounds(): void {
    this.currentDemandesPage = Math.min(Math.max(this.currentDemandesPage, 1), this.totalDemandesPages);
  }

  private restoreDemandeFilters(): void {
    try {
      const rawValue = localStorage.getItem(this.demandeFilterStorageKey);
      if (!rawValue) {
        return;
      }

      const savedFilters = JSON.parse(rawValue) as Partial<DemandeFiltersState>;
      this.demandeSearchTerm = savedFilters.searchTerm ?? '';
      this.filtreStatut = savedFilters.statut ?? 'TOUS';
      this.filtreDate = savedFilters.date ?? '';
      this.filtreTrajetId = savedFilters.trajetId ?? null;
      this.filtreTransportId = savedFilters.transportId ?? null;
      this.filtreSuspect = savedFilters.suspect ?? 'TOUS';
      this.filtreHeure = savedFilters.heure ?? '';
      this.demandesPageSize = 5;
    } catch {
      this.resetDemandesFilters();
    }
  }

  private persistDemandeFilters(): void {
    const state: DemandeFiltersState = {
      searchTerm: this.demandeSearchTerm,
      statut: this.filtreStatut,
      date: this.filtreDate,
      trajetId: this.filtreTrajetId,
      transportId: this.filtreTransportId,
      suspect: this.filtreSuspect,
      heure: this.filtreHeure,
      pageSize: 5
    };

    localStorage.setItem(this.demandeFilterStorageKey, JSON.stringify(state));
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
