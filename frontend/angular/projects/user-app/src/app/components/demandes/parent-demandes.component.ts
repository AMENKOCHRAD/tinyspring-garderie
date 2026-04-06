import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { DemandeTransport, ParentChild, Trajet } from '../../models/transport.models';
import { ChildrenService } from '../../services/children.service';
import { DemandesService } from '../../services/demandes.service';
import { TrajetsService } from '../../services/trajets.service';
import { PageHeroComponent } from '../shared/page-hero.component';

@Component({
  selector: 'app-parent-demandes',
  imports: [CommonModule, ReactiveFormsModule, PageHeroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-hero title="Mes demandes"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="row">
          <div class="col-lg-5 mb-4">
            <div class="portal-card bg-white p-4 h-100">
              <p class="section-title pr-5 mb-2"><span class="pr-2">Demande transport</span></p>
              <h2 class="mb-4">{{ editingId() ? 'Modifier la demande' : 'Nouvelle demande' }}</h2>

              <form [formGroup]="form" (ngSubmit)="submit()">
                <div class="form-group">
                  <label>Enfant</label>
                  <select class="form-control" formControlName="enfantId">
                    <option [ngValue]="0">Choisir un enfant</option>
                    <option *ngFor="let child of children()" [ngValue]="child.id">{{ child.nomComplet }}</option>
                  </select>
                </div>

                <div class="form-group">
                  <label>Trajet</label>
                  <select class="form-control" formControlName="trajetId">
                    <option [ngValue]="0">Choisir un trajet</option>
                    <option *ngFor="let trajet of trajets()" [ngValue]="trajet.id">
                      {{ trajet.pointDepart }} -> {{ trajet.destination }} | {{ trajet.dateTrajet }}
                    </option>
                  </select>
                </div>

                <div class="form-group">
                  <label>Point de ramassage</label>
                  <input class="form-control" formControlName="pointRamassage" placeholder="Ex: Lac 1, porte principale" />
                </div>

                <div *ngIf="message()" class="alert" [ngClass]="hasError() ? 'alert-danger' : 'alert-success'">
                  {{ message() }}
                </div>

                <div class="d-flex flex-wrap gap-2">
                  <button class="btn btn-primary mr-2" type="submit" [disabled]="form.invalid">
                    {{ editingId() ? 'Mettre a jour' : 'Creer' }}
                  </button>
                  <button class="btn btn-outline-secondary" type="button" *ngIf="editingId()" (click)="resetForm()">
                    Annuler
                  </button>
                </div>
              </form>
            </div>
          </div>

          <div class="col-lg-7 mb-4">
            <div class="portal-card bg-white p-4 h-100">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <p class="section-title pr-5 mb-2"><span class="pr-2">Historique</span></p>
                  <h2 class="mb-0">CRUD DemandeTransport</h2>
                </div>
              </div>

              <div class="table-responsive portal-table">
                <table class="table table-hover mb-0">
                  <thead class="thead-light">
                    <tr>
                      <th>ID</th>
                      <th>Trajet</th>
                      <th>Date Demande</th>
                      <th>Statut</th>
                      <th class="text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let demande of demandes()">
                      <td>#{{ demande.id }}</td>
                      <td>{{ demande.pointDepart }} -> {{ demande.destination }}</td>
                      <td>{{ demande.dateDemande }}</td>
                      <td>{{ demande.statut }}</td>
                      <td class="text-right">
                        <button class="btn btn-sm btn-outline-primary mr-2" type="button" (click)="edit(demande)">
                          Modifier
                        </button>
                        <button class="btn btn-sm btn-outline-danger" type="button" (click)="remove(demande.id)">
                          Supprimer
                        </button>
                      </td>
                    </tr>
                    <tr *ngIf="!demandes().length">
                      <td colspan="5" class="text-center text-muted">Aucune demande disponible.</td>
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
export class ParentDemandesComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly demandesService = inject(DemandesService);
  private readonly trajetsService = inject(TrajetsService);
  private readonly childrenService = inject(ChildrenService);

  protected readonly demandes = signal<DemandeTransport[]>([]);
  protected readonly trajets = signal<Trajet[]>([]);
  protected readonly children = signal<ParentChild[]>([]);
  protected readonly editingId = signal<number | null>(null);
  protected readonly message = signal('');
  protected readonly hasError = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    enfantId: [0, [Validators.required, Validators.min(1)]],
    trajetId: [0, [Validators.required, Validators.min(1)]],
    pointRamassage: ['', [Validators.required, Validators.minLength(3)]]
  });

  constructor() {
    this.loadChildren();
    this.loadTrajets();
    this.loadDemandes();
  }

  submit(): void {
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
        this.message.set(error.error?.message ?? 'Operation impossible.');
      }
    });
  }

  edit(demande: DemandeTransport): void {
    this.editingId.set(demande.id);
    this.message.set('');
    this.form.patchValue({
      enfantId: demande.enfantId,
      trajetId: demande.trajetId,
      pointRamassage: demande.pointRamassage
    });
  }

  remove(id: number): void {
    this.demandesService
      .delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.hasError.set(false);
          this.message.set('Demande supprimee.');
          this.loadDemandes();
          if (this.editingId() === id) {
            this.resetForm();
          }
        },
        error: (error) => {
          this.hasError.set(true);
          this.message.set(error.error?.message ?? 'Suppression impossible.');
        }
      });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.form.reset({ enfantId: 0, trajetId: 0, pointRamassage: '' });
  }

  private loadDemandes(): void {
    this.demandesService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((demandes) => this.demandes.set(demandes));
  }

  private loadTrajets(): void {
    this.trajetsService
      .getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((trajets) => this.trajets.set(trajets));
  }

  private loadChildren(): void {
    this.childrenService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((children) => this.children.set(children));
  }
}
