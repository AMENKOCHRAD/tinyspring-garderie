import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { DataTableComponent } from '../components/data-table.component';
import { AuthService } from '../shared/auth.service';
import { ChildrenService } from '../shared/children.service';
import { DemandesService } from '../shared/demandes.service';
import { DemandeTransport, ParentChild } from '../shared/transport.models';
import { parentChildrenColumns, parentModules, parentPaymentsColumns, parentPaymentsRows } from '../shared/site-data';

@Component({
  selector: 'app-parent-portal-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DataTableComponent, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="container-fluid bg-light py-4">
      <div class="container">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center mb-4">
          <div>
            <p class="section-title pr-5 mb-2"><span class="pr-2">Portail parent</span></p>
            <h1 class="mb-0">Bienvenue dans votre espace</h1>
            <small class="text-muted" *ngIf="currentUserEmail()">Connecte avec {{ currentUserEmail() }}</small>
          </div>
          <div class="mt-3 mt-lg-0">
            <a class="btn btn-outline-primary mr-2" routerLink="/">Retour au site</a>
            <button type="button" class="btn btn-danger" (click)="logout()">Deconnexion</button>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-md-6 col-lg-3 mb-3" *ngFor="let item of overview()">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <small class="text-muted d-block mb-2">{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-md-6 col-xl-3 mb-4" *ngFor="let module of modules">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <span class="badge text-white mb-3" [ngClass]="module.colorClass">{{ module.title }}</span>
                <h5>{{ module.title }}</h5>
                <p>{{ module.description }}</p>
                <a class="btn btn-outline-primary btn-sm" [routerLink]="module.route">{{ module.action }}</a>
              </div>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="col-lg-7 mb-4">
            <h4 class="mb-3">Mes enfants</h4>
            <app-data-table [columns]="childrenColumns" [rows]="childrenRows()"></app-data-table>
          </div>
          <div class="col-lg-5 mb-4">
            <h4 class="mb-3">Paiements</h4>
            <app-data-table [columns]="paymentsColumns" [rows]="paymentsRows"></app-data-table>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-lg-7 mb-4">
            <h4 class="mb-3">Demandes de transport</h4>
            <div class="table-responsive bg-white rounded shadow-sm p-3">
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

          <div class="col-lg-5 mb-4">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <h4 class="mb-3">{{ editingId() ? 'Modifier une demande' : 'Nouvelle demande de transport' }}</h4>
              <p>Le parent choisit un enfant, renseigne l adresse maison et l horaire souhaite. L analyse IA et le rapprochement avec un trajet admin se font ensuite cote backend.</p>

              <div *ngIf="message()" class="alert" [ngClass]="hasError() ? 'alert-danger' : 'alert-success'">
                {{ message() }}
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

                <button class="btn btn-primary mr-2" type="submit" [disabled]="form.invalid">
                  {{ editingId() ? 'Mettre a jour' : 'Envoyer la demande' }}
                </button>
                <button class="btn btn-outline-secondary" type="button" *ngIf="editingId()" (click)="resetForm()">Annuler</button>
              </form>
            </div>
          </div>
        </div>

        <div class="bg-white rounded shadow-sm p-4">
          <h4 class="mb-3">Actions rapides</h4>
          <div class="row">
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Mettre a jour le dossier</h5>
                <p class="mb-0">Modifier les informations parent et enfant.</p>
              </div>
            </div>
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Telecharger une facture</h5>
                <p class="mb-0">Acceder a l historique administratif.</p>
              </div>
            </div>
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Contacter la garderie</h5>
                <p class="mb-0">Envoyer un message au secretariat.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ParentPortalPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly childrenService = inject(ChildrenService);
  private readonly demandesService = inject(DemandesService);

  protected readonly children = signal<ParentChild[]>([]);
  protected readonly demandes = signal<DemandeTransport[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly message = signal('');
  protected readonly hasError = signal(false);
  protected readonly modules = parentModules;
  protected readonly childrenColumns = parentChildrenColumns;
  protected readonly paymentsColumns = parentPaymentsColumns;
  protected readonly paymentsRows = parentPaymentsRows;
  protected readonly currentUserEmail = computed(() => this.authService.getCurrentUser()?.email ?? '');
  protected readonly overview = computed(() => [
    { label: 'Enfants suivis', value: `${this.children().length}` },
    { label: 'Demandes transport', value: `${this.demandes().length}` },
    { label: 'Demandes en attente', value: `${this.demandes().filter((demande) => demande.statut === 'EN_ATTENTE').length}` },
    { label: 'Session', value: this.currentUserEmail() || 'Active' }
  ]);
  protected readonly childrenRows = computed(() =>
    this.children().map((child) => ({
      name: child.nomComplet,
      class: 'Garderie TinySpring',
      schedule: 'Selon inscription',
      status: 'Actif'
    }))
  );
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

  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
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
      .subscribe({
        next: (demandes) => this.demandes.set(demandes),
        error: () => {
          this.hasError.set(true);
          this.message.set('Chargement des demandes impossible.');
        }
      });
  }

  private loadChildren(): void {
    this.childrenService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (children) => this.children.set(children),
        error: () => {
          this.hasError.set(true);
          this.message.set('Chargement des enfants impossible.');
        }
      });
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
