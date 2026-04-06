import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from 'src/app/services/auth.service';
import {
  MessagerieService,
  Reclamation
} from 'src/app/services/messagerie.service';

@Component({
  selector: 'app-reclamation-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="reclamation-page">
      <div class="reclamation-wrapper">

        <div class="page-header">
          <h2>Gestion des Réclamations</h2>
          <p>Créer, consulter, modifier, supprimer et suivre les réclamations</p>
        </div>

        <!-- CREATE -->
        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Créer une réclamation</h4>

            <div *ngIf="createError" class="alert alert-danger">
              {{ createError }}
            </div>

            <div *ngIf="createSuccess" class="alert alert-success">
              {{ createSuccess }}
            </div>

            <div class="mb-3">
              <label class="form-label">Titre</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="newReclamation.title"
                placeholder="Ex: Problème de repas"
              />
            </div>

            <div class="mb-3">
              <label class="form-label">Description</label>
              <textarea
                class="form-control"
                rows="4"
                [(ngModel)]="newReclamation.description"
                placeholder="Décrire votre réclamation..."
              ></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label">Priorité</label>
              <select
                class="form-control"
                [(ngModel)]="newReclamation.priority"
              >
                <option value="">-- Choisir --</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <button class="btn btn-primary" (click)="createReclamation()">
              Créer réclamation
            </button>
          </div>
        </div>

        <!-- UPDATE -->
        <div class="card shadow-sm border-0 mb-4" *ngIf="editingReclamationId !== null">
          <div class="card-body">
            <h4 class="mb-3">Modifier la réclamation</h4>

            <div *ngIf="updateError" class="alert alert-danger">
              {{ updateError }}
            </div>

            <div *ngIf="updateSuccess" class="alert alert-success">
              {{ updateSuccess }}
            </div>

            <div class="mb-3">
              <label class="form-label">Titre</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="editedReclamation.title"
              />
            </div>

            <div class="mb-3">
              <label class="form-label">Description</label>
              <textarea
                class="form-control"
                rows="4"
                [(ngModel)]="editedReclamation.description"
              ></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label">Priorité</label>
              <select
                class="form-control"
                [(ngModel)]="editedReclamation.priority"
              >
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <button class="btn btn-success me-2" (click)="updateReclamation()">
              Enregistrer
            </button>

            <button class="btn btn-secondary" (click)="cancelEdit()">
              Annuler
            </button>
          </div>
        </div>

        <!-- FILTERS -->
        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Recherche et filtres</h4>

            <div class="row">
              <div class="col-md-4 mb-3">
                <label class="form-label">Recherche par titre</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="searchTitle"
                  placeholder="Chercher une réclamation..."
                />
              </div>

              <div class="col-md-4 mb-3">
                <label class="form-label">Filtrer par statut</label>
                <select class="form-control" [(ngModel)]="filterStatus">
                  <option value="">Tous</option>
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </div>

              <div class="col-md-4 mb-3">
                <label class="form-label">Filtrer par priorité</label>
                <select class="form-control" [(ngModel)]="filterPriority">
                  <option value="">Toutes</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- LIST -->
        <div class="card shadow-sm border-0">
          <div class="card-body">
            <h4 class="mb-3">Liste des réclamations</h4>

            <div *ngIf="loading">Chargement...</div>

            <div *ngIf="error" class="alert alert-danger">
              {{ error }}
            </div>

            <div *ngIf="deleteSuccess" class="alert alert-success">
              {{ deleteSuccess }}
            </div>

            <div *ngIf="statusSuccess" class="alert alert-success">
              {{ statusSuccess }}
            </div>

            <div *ngIf="!loading && filteredReclamations().length === 0" class="alert alert-info">
              Aucune réclamation trouvée.
            </div>

            <div class="reclamation-list" *ngIf="filteredReclamations().length > 0">
              <div class="reclamation-card" *ngFor="let rec of filteredReclamations()">
                <h5>{{ rec.title }}</h5>
                <p class="mb-2">{{ rec.description }}</p>

                <p class="mb-1">
                  <strong>Priorité :</strong>
                  <span class="badge bg-secondary">{{ rec.priority }}</span>
                </p>

                <p class="mb-2">
                  <strong>Statut actuel :</strong>
                  <span
                    class="badge"
                    [ngClass]="{
                      'bg-success': rec.status === 'RESOLVED',
                      'bg-warning text-dark': rec.status === 'IN_PROGRESS',
                      'bg-danger': rec.status === 'REJECTED',
                      'bg-primary': rec.status === 'OPEN'
                    }"
                  >
                    {{ rec.status }}
                  </span>
                </p>

                <div class="mb-3" *ngIf="isAdmin()">
                  <label class="form-label">Changer le statut</label>
                  <select
                    class="form-control"
                    [ngModel]="rec.status"
                    (ngModelChange)="changeStatus(rec.id, $event)"
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                    <option value="REJECTED">REJECTED</option>
                  </select>
                </div>

                <div class="mb-3" *ngIf="!isAdmin()">
                  <label class="form-label">Suivi de traitement</label>
                  <div class="form-control bg-light">
                    {{ rec.status }}
                  </div>
                </div>

                <p class="mb-3 text-muted">
                  Créée le : {{ rec.createdAt | date:'short' }}
                </p>

                <button class="btn btn-sm btn-warning me-2" (click)="editReclamation(rec)">
                  Modifier
                </button>

                <button class="btn btn-sm btn-danger" (click)="deleteReclamation(rec.id)">
                  Supprimer
                </button>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .reclamation-page {
      width: 100%;
      display: flex;
      justify-content: center;
      padding: 24px;
      box-sizing: border-box;
    }

    .reclamation-wrapper {
      width: 100%;
      max-width: 1100px;
      margin: 0 auto;
    }

    .page-header {
      margin-bottom: 20px;
    }

    .page-header h2 {
      margin: 0;
      font-size: 30px;
      font-weight: 700;
      color: #212529;
    }

    .page-header p {
      margin: 6px 0 0 0;
      color: #6c757d;
      font-size: 16px;
    }

    .reclamation-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-top: 16px;
    }

    .reclamation-card {
      background: #fff;
      border: 1px solid #e9ecef;
      border-radius: 14px;
      padding: 16px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
  `]
})
export class ReclamationPageComponent implements OnInit {
  reclamations: Reclamation[] = [];

  loading = false;
  error = '';

  createError = '';
  createSuccess = '';

  updateError = '';
  updateSuccess = '';

  deleteSuccess = '';
  statusSuccess = '';

  editingReclamationId: number | null = null;

  searchTitle = '';
  filterStatus = '';
  filterPriority = '';

  newReclamation = {
    title: '',
    description: '',
    priority: ''
  };

  editedReclamation = {
    title: '',
    description: '',
    priority: ''
  };

  constructor(
    private messagerieService: MessagerieService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadReclamations();
  }

  isAdmin(): boolean {
    return this.authService.getRole() === 'ADMIN';
  }

  filteredReclamations(): Reclamation[] {
    return this.reclamations.filter((rec) => {
      const matchTitle =
        !this.searchTitle ||
        rec.title.toLowerCase().includes(this.searchTitle.toLowerCase());

      const matchStatus =
        !this.filterStatus || rec.status === this.filterStatus;

      const matchPriority =
        !this.filterPriority || rec.priority === this.filterPriority;

      return matchTitle && matchStatus && matchPriority;
    });
  }

  loadReclamations(): void {
    this.loading = true;
    this.error = '';
    this.deleteSuccess = '';
    this.statusSuccess = '';

    this.messagerieService.getMyReclamations().subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.log('Erreur chargement réclamations = ', err);
        this.error = 'Impossible de charger les réclamations.';
        this.loading = false;
      }
    });
  }

  createReclamation(): void {
    this.createError = '';
    this.createSuccess = '';

    if (!this.newReclamation.title.trim()) {
      this.createError = 'Le titre est obligatoire.';
      return;
    }

    if (!this.newReclamation.description.trim()) {
      this.createError = 'La description est obligatoire.';
      return;
    }

    if (!this.newReclamation.priority) {
      this.createError = 'La priorité est obligatoire.';
      return;
    }

    this.messagerieService.createReclamation({
      title: this.newReclamation.title.trim(),
      description: this.newReclamation.description.trim(),
      priority: this.newReclamation.priority
    }).subscribe({
      next: () => {
        this.createSuccess = 'Réclamation créée avec succès.';
        this.newReclamation = {
          title: '',
          description: '',
          priority: ''
        };
        this.loadReclamations();
      },
      error: (err: any) => {
        console.log('Erreur création réclamation = ', err);
        this.createError = 'Impossible de créer la réclamation.';
      }
    });
  }

  editReclamation(rec: Reclamation): void {
    this.editingReclamationId = rec.id;
    this.editedReclamation = {
      title: rec.title,
      description: rec.description,
      priority: rec.priority
    };
    this.updateError = '';
    this.updateSuccess = '';
  }

  cancelEdit(): void {
    this.editingReclamationId = null;
    this.editedReclamation = {
      title: '',
      description: '',
      priority: ''
    };
    this.updateError = '';
    this.updateSuccess = '';
  }

  updateReclamation(): void {
    this.updateError = '';
    this.updateSuccess = '';

    if (this.editingReclamationId === null) {
      this.updateError = 'Aucune réclamation sélectionnée.';
      return;
    }

    if (!this.editedReclamation.title.trim()) {
      this.updateError = 'Le titre est obligatoire.';
      return;
    }

    if (!this.editedReclamation.description.trim()) {
      this.updateError = 'La description est obligatoire.';
      return;
    }

    if (!this.editedReclamation.priority) {
      this.updateError = 'La priorité est obligatoire.';
      return;
    }

    this.messagerieService.updateReclamation(this.editingReclamationId, {
      title: this.editedReclamation.title.trim(),
      description: this.editedReclamation.description.trim(),
      priority: this.editedReclamation.priority
    }).subscribe({
      next: () => {
        this.updateSuccess = 'Réclamation modifiée avec succès.';
        this.editingReclamationId = null;
        this.editedReclamation = {
          title: '',
          description: '',
          priority: ''
        };
        this.loadReclamations();
      },
      error: (err: any) => {
        console.log('Erreur update réclamation = ', err);
        this.updateError = 'Impossible de modifier la réclamation.';
      }
    });
  }

  deleteReclamation(id: number): void {
    this.error = '';
    this.deleteSuccess = '';

    const confirmed = confirm('Voulez-vous vraiment supprimer cette réclamation ?');
    if (!confirmed) {
      return;
    }

    this.messagerieService.deleteReclamation(id).subscribe({
      next: () => {
        this.deleteSuccess = 'Réclamation supprimée avec succès.';
        this.loadReclamations();
      },
      error: (err: any) => {
        console.log('Erreur suppression réclamation = ', err);
        this.error = 'Impossible de supprimer la réclamation.';
      }
    });
  }

  changeStatus(id: number, newStatus: string): void {
    if (!this.isAdmin()) {
      return;
    }

    this.error = '';
    this.statusSuccess = '';

    this.messagerieService.updateReclamationStatus(id, newStatus).subscribe({
      next: () => {
        this.statusSuccess = 'Statut mis à jour avec succès.';
        this.loadReclamations();
      },
      error: (err: any) => {
        console.log('Erreur changement statut réclamation = ', err);
        this.error = 'Impossible de changer le statut.';
      }
    });
  }
}