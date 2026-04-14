import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../shared/auth.service';
import { ChildrenService } from '../shared/children.service';
import { DemandesService } from '../shared/demandes.service';
import { DemandeTransport, ParentChild } from '../shared/transport.models';

@Component({
  selector: 'app-parent-transport-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="container-fluid bg-light py-5">
      <div class="container">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center mb-4">
          <div>
            <p class="section-title pr-5 mb-2"><span class="pr-2">Transport parent</span></p>
            <h1 class="mb-0">Mes demandes de transport</h1>
            <small class="text-muted" *ngIf="currentUserEmail()">{{ currentUserEmail() }}</small>
          </div>
          <div class="mt-3 mt-lg-0">
            <a class="btn btn-outline-primary mr-2" routerLink="/parent/portal">Retour au portail</a>
            <a class="btn btn-primary" routerLink="/">Accueil</a>
          </div>
        </div>

        <div class="row">
          <div class="col-lg-5 mb-4">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <h4 class="mb-3">{{ editingId() ? 'Modifier une demande' : 'Nouvelle demande de transport' }}</h4>
              <p>Choisissez un enfant, renseignez l adresse maison et l horaire souhaite. L analyse IA et l affectation au trajet admin se feront ensuite cote backend.</p>

              <div *ngIf="message()" class="alert" [ngClass]="hasError() ? 'alert-danger' : 'alert-success'">
                {{ message() }}
              </div>

              <div *ngIf="selectedChildHasActiveRequest()" class="alert alert-warning">
                Une demande active existe deja pour cet enfant. Modifiez la demande existante dans le tableau a droite.
              </div>

              <form [formGroup]="form" (ngSubmit)="submit()">
                <div class="form-group">
                  <label>Enfant</label>
                  <select class="form-control" formControlName="enfantId">
                    <option [ngValue]="0">Choisir un enfant</option>
                    <option *ngFor="let child of children()" [ngValue]="child.id">{{ child.nomComplet }}</option>
                  </select>
                  <small class="text-danger" *ngIf="isFieldInvalid('enfantId')">Selection obligatoire.</small>
                </div>

                <div class="form-group">
                  <label>Sens du trajet</label>
                  <select class="form-control" formControlName="sensTrajet">
                    <option value="MAISON_VERS_GARDERIE">Maison -> Garderie</option>
                    <option value="GARDERIE_VERS_MAISON">Garderie -> Maison</option>
                  </select>
                </div>

                <div class="form-group">
                  <label>Adresse maison</label>
                  <input class="form-control" type="text" formControlName="adresseMaison" placeholder="Ex: 45 Avenue Habib Bourguiba, Tunis">
                  <small class="text-danger" *ngIf="isFieldInvalid('adresseMaison')">
                    L adresse maison est obligatoire et doit contenir au moins 8 caracteres.
                  </small>
                </div>

                <div class="form-row">
                  <div class="form-group col-md-6">
                    <label>Latitude</label>
                    <input class="form-control" type="number" step="0.000001" formControlName="latitudeMaison">
                  </div>
                  <div class="form-group col-md-6">
                    <label>Longitude</label>
                    <input class="form-control" type="number" step="0.000001" formControlName="longitudeMaison">
                  </div>
                </div>

                <div class="form-row">
                  <div class="form-group col-md-6">
                    <label>Date souhaitee</label>
                    <input class="form-control" type="date" formControlName="dateSouhaitee">
                    <small class="text-danger" *ngIf="isFieldInvalid('dateSouhaitee')">Date obligatoire.</small>
                  </div>
                  <div class="form-group col-md-6">
                    <label>Heure souhaitee</label>
                    <input class="form-control" type="time" formControlName="heureSouhaitee">
                    <small class="text-danger" *ngIf="isFieldInvalid('heureSouhaitee')">Heure obligatoire.</small>
                  </div>
                </div>

                <button class="btn btn-primary mr-2" type="submit" [disabled]="form.invalid || selectedChildHasActiveRequest()">
                  {{ editingId() ? 'Mettre a jour' : 'Envoyer la demande' }}
                </button>
                <button class="btn btn-outline-secondary" type="button" *ngIf="editingId()" (click)="resetForm()">Annuler</button>
              </form>
            </div>
          </div>

          <div class="col-lg-7 mb-4">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <p class="section-title pr-5 mb-2"><span class="pr-2">Historique</span></p>
                  <h4 class="mb-0">Demandes existantes</h4>
                </div>
              </div>

              <div class="table-responsive">
                <table class="table table-hover mb-0">
                  <thead class="thead-light">
                    <tr>
                      <th>ID</th>
                      <th>Enfant</th>
                      <th>Trajet</th>
                      <th>Date</th>
                      <th>Statut</th>
                      <th class="text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let demande of demandes()">
                      <td>#{{ demande.id }}</td>
                      <td>{{ demande.enfantNomComplet }}</td>
                      <td>
                        {{ demande.pointDepart }} -> {{ demande.destination }}
                        <div class="text-muted small">Souhaite: {{ demande.dateSouhaitee }} a {{ demande.heureSouhaitee }}</div>
                      </td>
                      <td>{{ demande.dateTrajet ? (demande.dateTrajet + ' a ' + demande.heureDepart) : (demande.dateSouhaitee + ' a ' + demande.heureSouhaitee) }}</td>
                      <td>
                        <span class="badge px-3 py-2" [ngClass]="getStatusClass(demande.statut)">
                          {{ formatStatus(demande.statut) }}
                        </span>
                        <div class="small mt-2" [class.text-danger]="demande.suspicious" [class.text-muted]="!demande.suspicious">
                          {{ getAiStatusLabel(demande) }}
                          <span *ngIf="demande.anomalyScore !== null">| Score: {{ formatAnomalyScore(demande.anomalyScore) }}</span>
                        </div>
                        <div class="small text-muted" *ngIf="demande.anomalyReasons.length">{{ demande.anomalyReasons.join(' | ') }}</div>
                        <div class="small text-danger" *ngIf="demande.aiAnalysisError">{{ demande.aiAnalysisError }}</div>
                      </td>
                      <td class="text-right">
                        <button class="btn btn-sm btn-outline-primary mr-2" type="button" (click)="edit(demande)">Modifier</button>
                        <button class="btn btn-sm btn-outline-danger" type="button" (click)="remove(demande.id)">Supprimer</button>
                      </td>
                    </tr>
                    <tr *ngIf="!demandes().length">
                      <td colspan="6" class="text-center text-muted">Aucune demande disponible.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ParentTransportPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);
  private readonly childrenService = inject(ChildrenService);
  private readonly demandesService = inject(DemandesService);

  protected readonly currentUserEmail = signal(this.authService.getCurrentUser()?.email ?? '');
  protected readonly children = signal<ParentChild[]>([]);
  protected readonly demandes = signal<DemandeTransport[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly message = signal('');
  protected readonly hasError = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    enfantId: [0, [Validators.required, Validators.min(1)]],
    sensTrajet: ['MAISON_VERS_GARDERIE' as const, [Validators.required]],
    adresseMaison: ['', [Validators.required, Validators.minLength(8)]],
    latitudeMaison: [36.8065, [Validators.required]],
    longitudeMaison: [10.1815, [Validators.required]],
    dateSouhaitee: [this.getTomorrowDate(), [Validators.required]],
    heureSouhaitee: ['07:30', [Validators.required]]
  });

  constructor() {
    this.loadChildren();
    this.loadDemandes();
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
        this.hasError.set(false);
        this.message.set(this.editingId() ? 'Demande mise a jour.' : 'Demande creee.');
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
    this.form.patchValue({
      enfantId: demande.enfantId,
      sensTrajet: demande.sensTrajet,
      adresseMaison: demande.adresseMaison,
      latitudeMaison: demande.latitudeMaison,
      longitudeMaison: demande.longitudeMaison,
      dateSouhaitee: demande.dateSouhaitee,
      heureSouhaitee: demande.heureSouhaitee?.slice(0, 5) ?? '07:30'
    });
  }

  protected remove(id: number): void {
    this.demandesService
      .delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.hasError.set(false);
          this.message.set('Demande supprimee.');
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
    this.form.reset({
      enfantId: 0,
      sensTrajet: 'MAISON_VERS_GARDERIE',
      adresseMaison: '',
      latitudeMaison: 36.8065,
      longitudeMaison: 10.1815,
      dateSouhaitee: this.getTomorrowDate(),
      heureSouhaitee: '07:30'
    });
  }

  protected isFieldInvalid(
    fieldName: 'enfantId' | 'adresseMaison' | 'dateSouhaitee' | 'heureSouhaitee'
  ): boolean {
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
        (demande.statut === 'EN_ATTENTE' || demande.statut === 'ACCEPTEE')
    );
  }

  protected formatStatus(status: string): string {
    return status.replace('_', ' ');
  }

  protected getStatusClass(status: string): string {
    switch (status) {
      case 'ACCEPTEE':
        return 'badge-success';
      case 'REFUSEE':
        return 'badge-danger';
      default:
        return 'badge-warning';
    }
  }

  protected getAiStatusLabel(demande: DemandeTransport): string {
    if (!demande.aiAnalysisAvailable) {
      return 'IA indisponible';
    }

    return demande.suspicious ? `Demande suspecte (${demande.anomalyLevel || 'UNKNOWN'})` : 'Demande analysee';
  }

  protected formatAnomalyScore(score: number | null): string {
    return score == null ? 'N/A' : score.toFixed(3);
  }

  private loadDemandes(): void {
    this.demandesService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((demandes) => this.demandes.set(demandes));
  }

  private loadChildren(): void {
    this.childrenService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((children) => this.children.set(children));
  }

  private getTomorrowDate(): string {
    const date = new Date();
    date.setDate(date.getDate() + 1);
    return date.toISOString().split('T')[0];
  }

  private extractErrorMessage(error: any, fallback = 'Operation impossible.'): string {
    if (Array.isArray(error?.error?.details) && error.error.details.length > 0) {
      return error.error.details[0];
    }

    return error?.error?.message ?? fallback;
  }
}
