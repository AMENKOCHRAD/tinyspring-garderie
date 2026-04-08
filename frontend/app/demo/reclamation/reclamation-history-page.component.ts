import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  MessagerieService,
  Reclamation,
  ReclamationHistory
} from 'src/app/services/messagerie.service';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-reclamation-history-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="history-page">
      <div class="history-wrapper">

        <div class="page-header">
          <h2>Historique des réclamations</h2>
          <p>
            Consultez l’évolution complète de chaque réclamation avec une vue claire,
            chronologique et professionnelle.
          </p>
        </div>

        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Recherche et filtres des réclamations</h4>

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
                <label class="form-label">Filtrer par catégorie</label>
                <select class="form-control" [(ngModel)]="filterCategory">
                  <option value="">Toutes</option>
                  <option *ngFor="let category of reclamationCategories" [value]="category">
                    {{ getCategoryLabel(category) }}
                  </option>
                </select>
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
            </div>
          </div>
        </div>

        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Réclamations disponibles</h4>

            <div *ngIf="loading">Chargement...</div>

            <div *ngIf="error" class="alert alert-danger">
              {{ error }}
            </div>

            <div *ngIf="!loading && filteredReclamations().length === 0" class="alert alert-info">
              Aucune réclamation trouvée.
            </div>

            <div class="reclamation-grid" *ngIf="filteredReclamations().length > 0">
              <div
                class="reclamation-summary-card"
                *ngFor="let rec of filteredReclamations()"
                [class.selected]="selectedReclamation?.id === rec.id"
                (click)="selectReclamation(rec)"
              >
                <div class="summary-top">
                  <h5>{{ rec.title }}</h5>
                  <span class="badge category-badge">
                    {{ getCategoryLabel(rec.category) }}
                  </span>
                </div>

                <p class="summary-description">
                  {{ rec.description }}
                </p>

                <div class="summary-meta">
                  <span class="badge"
                    [ngClass]="{
                      'bg-success': rec.status === 'RESOLVED',
                      'bg-warning text-dark': rec.status === 'IN_PROGRESS',
                      'bg-danger': rec.status === 'REJECTED',
                      'bg-primary': rec.status === 'OPEN'
                    }">
                    {{ rec.status }}
                  </span>

                  <span class="badge bg-secondary">
                    {{ rec.priority }}
                  </span>
                </div>

                <small class="text-muted">
                  Créée le : {{ rec.createdAt | date:'short' }}
                </small>
              </div>
            </div>
          </div>
        </div>

        <div class="card shadow-sm border-0" *ngIf="selectedReclamation">
          <div class="card-body">
            <div class="history-header">
              <div>
                <h4 class="mb-1">Timeline de la réclamation</h4>
                <p class="text-muted mb-0">
                  {{ selectedReclamation.title }}
                </p>
              </div>

              <div class="d-flex gap-2 flex-wrap">
                <button
                  *ngIf="isAdmin()"
                  class="btn btn-danger btn-sm"
                  (click)="exportHistoryPdf()"
                >
                  {{ exportLoading ? 'Export en cours...' : 'Exporter PDF' }}
                </button>

                <button class="btn btn-outline-secondary btn-sm" (click)="clearSelection()">
                  Fermer
                </button>
              </div>
            </div>

            <div *ngIf="exportError" class="alert alert-danger mt-3">
              {{ exportError }}
            </div>

            <div *ngIf="historyLoading" class="mt-3">Chargement de l’historique...</div>

            <div *ngIf="historyError" class="alert alert-danger mt-3">
              {{ historyError }}
            </div>

            <ng-container *ngIf="!historyLoading && !historyError">
              <div class="stats-cards mt-4" *ngIf="historyEntries.length > 0">
                <div class="stats-mini-card">
                  <div class="stats-mini-label">Total événements</div>
                  <div class="stats-mini-value">{{ filteredHistoryEntries().length }}</div>
                </div>

                <div class="stats-mini-card">
                  <div class="stats-mini-label">Actions Admin</div>
                  <div class="stats-mini-value">{{ getActorCount('ADMIN') }}</div>
                </div>

                <div class="stats-mini-card">
                  <div class="stats-mini-label">Actions Parent</div>
                  <div class="stats-mini-value">{{ getActorCount('PARENT') }}</div>
                </div>

                <div class="stats-mini-card">
                  <div class="stats-mini-label">Types visibles</div>
                  <div class="stats-mini-value">{{ getVisibleActionTypesCount() }}</div>
                </div>
              </div>

              <div class="card border-0 bg-light mt-4" *ngIf="historyEntries.length > 0">
                <div class="card-body">
                  <h5 class="mb-3">Filtres de l’historique</h5>

                  <div class="row">
                    <div class="col-md-4 mb-3">
                      <label class="form-label">Recherche dans l’historique</label>
                      <input
                        type="text"
                        class="form-control"
                        [(ngModel)]="historySearch"
                        placeholder="Chercher action, acteur, valeur..."
                      />
                    </div>

                    <div class="col-md-4 mb-3">
                      <label class="form-label">Filtrer par acteur</label>
                      <select class="form-control" [(ngModel)]="historyActorFilter">
                        <option value="">Tous</option>
                        <option value="ADMIN">ADMIN</option>
                        <option value="PARENT">PARENT</option>
                      </select>
                    </div>

                    <div class="col-md-4 mb-3">
                      <label class="form-label">Filtrer par type d’action</label>
                      <select class="form-control" [(ngModel)]="historyActionTypeFilter">
                        <option value="">Tous</option>
                        <option *ngFor="let type of historyActionTypes" [value]="type">
                          {{ getActionTypeLabel(type) }}
                        </option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>

              <div *ngIf="historyEntries.length === 0" class="alert alert-info mt-3">
                Aucun historique disponible pour cette réclamation.
              </div>

              <div *ngIf="historyEntries.length > 0 && filteredHistoryEntries().length === 0" class="alert alert-warning mt-3">
                Aucun événement ne correspond aux filtres actuels.
              </div>

              <div class="history-timeline mt-4" *ngIf="filteredHistoryEntries().length > 0">
                <div class="history-item" *ngFor="let item of filteredHistoryEntries()">
                  <div class="history-dot" [ngClass]="getHistoryColorClass(item.actionType)">
                    {{ getHistoryIcon(item.actionType) }}
                  </div>

                  <div class="history-content">
                    <div class="history-top-line">
                      <div class="history-title-group">
                        <div class="history-title">{{ item.actionLabel }}</div>
                        <span class="history-type-badge">
                          {{ getActionTypeLabel(item.actionType) }}
                        </span>
                      </div>
                      <div class="history-date">{{ item.createdAt | date:'short' }}</div>
                    </div>

                    <div class="history-meta">
                      Par <strong>{{ item.actorName }}</strong>
                      <span class="history-role">({{ item.actorRole }})</span>
                    </div>

                    <div class="history-values" *ngIf="item.oldValue || item.newValue">
                      <div *ngIf="item.oldValue" class="history-old">
                        <strong>Ancienne valeur :</strong>
                        <div class="history-value-box old-box">{{ item.oldValue }}</div>
                      </div>

                      <div *ngIf="item.newValue" class="history-new">
                        <strong>Nouvelle valeur :</strong>
                        <div class="history-value-box new-box">{{ item.newValue }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="archive-box mt-4">
                <h5 class="mb-2">Vision métier à venir</h5>
                <p class="mb-0 text-muted">
                  Cette page est maintenant prête pour accueillir ensuite :
                  réclamations archivées, historiques clôturés, filtres avancés supplémentaires,
                  et statistiques de suivi administratif.
                </p>
              </div>
            </ng-container>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .history-page {
      width: 100%;
      display: flex;
      justify-content: center;
      padding: 24px;
      box-sizing: border-box;
    }

    .history-wrapper {
      width: 100%;
      max-width: 1200px;
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
      max-width: 760px;
    }

    .reclamation-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
    }

    .reclamation-summary-card {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 16px;
      padding: 16px;
      cursor: pointer;
      transition: all 0.25s ease;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    }

    .reclamation-summary-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 24px rgba(0,0,0,0.08);
      border-color: #cbd5e1;
    }

    .reclamation-summary-card.selected {
      border-color: #3b82f6;
      background: #f8fbff;
      box-shadow: 0 10px 24px rgba(59,130,246,0.12);
    }

    .summary-top {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: flex-start;
      margin-bottom: 10px;
    }

    .summary-top h5 {
      margin: 0;
      font-weight: 700;
      color: #1f2937;
    }

    .summary-description {
      color: #4b5563;
      min-height: 48px;
      margin-bottom: 12px;
      line-height: 1.45;
    }

    .summary-meta {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 10px;
    }

    .category-badge {
      background: #e0ecff;
      color: #1d4ed8;
      font-weight: 600;
      border: 1px solid #bfd6ff;
    }

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
    }

    .stats-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 14px;
    }

    .stats-mini-card {
      background: linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
      border: 1px solid #d7e6ff;
      border-radius: 14px;
      padding: 16px;
    }

    .stats-mini-label {
      font-size: 13px;
      color: #64748b;
      margin-bottom: 6px;
    }

    .stats-mini-value {
      font-size: 26px;
      font-weight: 700;
      color: #1e3a8a;
    }

    .history-timeline {
      position: relative;
      padding-left: 18px;
      border-left: 3px solid #dbe7f5;
    }

    .history-item {
      position: relative;
      padding-left: 28px;
      margin-bottom: 22px;
    }

    .history-dot {
      position: absolute;
      left: -16px;
      top: 2px;
      width: 30px;
      height: 30px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      color: white;
      box-shadow: 0 4px 10px rgba(0,0,0,0.12);
    }

    .history-blue {
      background: #2563eb;
    }

    .history-green {
      background: #10b981;
    }

    .history-orange {
      background: #f59e0b;
    }

    .history-purple {
      background: #8b5cf6;
    }

    .history-red {
      background: #ef4444;
    }

    .history-content {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 14px;
    }

    .history-top-line {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
      margin-bottom: 6px;
    }

    .history-title-group {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    .history-title {
      font-weight: 700;
      color: #1e293b;
    }

    .history-type-badge {
      background: #eef2ff;
      color: #4338ca;
      border: 1px solid #c7d2fe;
      border-radius: 999px;
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 600;
    }

    .history-date {
      font-size: 13px;
      color: #64748b;
    }

    .history-meta {
      font-size: 14px;
      color: #475569;
      margin-bottom: 10px;
    }

    .history-role {
      color: #2563eb;
      font-weight: 600;
    }

    .history-values {
      display: flex;
      flex-direction: column;
      gap: 10px;
      font-size: 14px;
      margin-top: 10px;
    }

    .history-old,
    .history-new {
      white-space: pre-wrap;
      line-height: 1.45;
    }

    .history-value-box {
      margin-top: 6px;
      border-radius: 10px;
      padding: 10px 12px;
      border: 1px solid #e5e7eb;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .old-box {
      background: #fff7ed;
      border-color: #fed7aa;
      color: #9a3412;
    }

    .new-box {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #065f46;
    }

    .archive-box {
      background: linear-gradient(180deg, #fffdf5 0%, #fff8e7 100%);
      border: 1px solid #f5deb3;
      border-radius: 14px;
      padding: 16px;
    }

    @media (max-width: 768px) {
      .history-page {
        padding: 12px;
      }

      .summary-top,
      .history-top-line {
        flex-direction: column;
      }
    }
  `]
})
export class ReclamationHistoryPageComponent implements OnInit {
  reclamations: Reclamation[] = [];
  historyEntries: ReclamationHistory[] = [];
  selectedReclamation: Reclamation | null = null;

  reclamationCategories: string[] = [
    'REPAS',
    'TRANSPORT',
    'COMPORTEMENT',
    'HYGIENE',
    'SECURITE',
    'PERSONNEL',
    'AUTRE'
  ];

  historyActionTypes: string[] = [
    'CREATED',
    'TITLE_CHANGED',
    'DESCRIPTION_UPDATED',
    'CATEGORY_CHANGED',
    'PRIORITY_CHANGED',
    'STATUS_CHANGED',
    'ADMIN_COMMENT_ADDED',
    'ADMIN_COMMENT_UPDATED',
    'ADMIN_COMMENT_REMOVED'
  ];

  loading = false;
  historyLoading = false;
  exportLoading = false;

  error = '';
  historyError = '';
  exportError = '';

  searchTitle = '';
  filterCategory = '';
  filterStatus = '';

  historySearch = '';
  historyActorFilter = '';
  historyActionTypeFilter = '';

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

  loadReclamations(): void {
    this.loading = true;
    this.error = '';

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

  filteredReclamations(): Reclamation[] {
    return this.reclamations.filter((rec) => {
      const matchTitle =
        !this.searchTitle ||
        rec.title.toLowerCase().includes(this.searchTitle.toLowerCase());

      const matchCategory =
        !this.filterCategory || rec.category === this.filterCategory;

      const matchStatus =
        !this.filterStatus || rec.status === this.filterStatus;

      return matchTitle && matchCategory && matchStatus;
    });
  }

  filteredHistoryEntries(): ReclamationHistory[] {
    return this.historyEntries.filter((item) => {
      const search = this.historySearch.toLowerCase();

      const matchesSearch =
        !this.historySearch ||
        item.actionLabel?.toLowerCase().includes(search) ||
        item.actorName?.toLowerCase().includes(search) ||
        item.actorRole?.toLowerCase().includes(search) ||
        item.oldValue?.toLowerCase().includes(search) ||
        item.newValue?.toLowerCase().includes(search);

      const matchesActor =
        !this.historyActorFilter || item.actorRole === this.historyActorFilter;

      const matchesActionType =
        !this.historyActionTypeFilter || item.actionType === this.historyActionTypeFilter;

      return matchesSearch && matchesActor && matchesActionType;
    });
  }

  selectReclamation(rec: Reclamation): void {
    this.selectedReclamation = rec;
    this.historySearch = '';
    this.historyActorFilter = '';
    this.historyActionTypeFilter = '';
    this.exportError = '';
    this.loadHistory(rec.id);
  }

  clearSelection(): void {
    this.selectedReclamation = null;
    this.historyEntries = [];
    this.historyError = '';
    this.historyLoading = false;
    this.exportLoading = false;
    this.exportError = '';
    this.historySearch = '';
    this.historyActorFilter = '';
    this.historyActionTypeFilter = '';
  }

  loadHistory(reclamationId: number): void {
    this.historyLoading = true;
    this.historyError = '';
    this.historyEntries = [];

    this.messagerieService.getReclamationHistory(reclamationId).subscribe({
      next: (data) => {
        this.historyEntries = data;
        this.historyLoading = false;
      },
      error: (err: any) => {
        console.log('Erreur chargement historique = ', err);
        this.historyError = 'Impossible de charger l’historique.';
        this.historyLoading = false;
      }
    });
  }

  exportHistoryPdf(): void {
    if (!this.selectedReclamation || !this.isAdmin()) {
      return;
    }

    this.exportLoading = true;
    this.exportError = '';

    this.messagerieService.exportReclamationHistoryPdf(this.selectedReclamation.id).subscribe({
      next: (blob: Blob) => {
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = blobUrl;
        link.download = `historique-reclamation-${this.selectedReclamation?.id}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        window.URL.revokeObjectURL(blobUrl);
        this.exportLoading = false;
      },
      error: (err: any) => {
        console.log('Erreur export PDF historique = ', err);
        this.exportError = 'Impossible d’exporter le PDF.';
        this.exportLoading = false;
      }
    });
  }

  getActorCount(role: string): number {
    return this.filteredHistoryEntries().filter(item => item.actorRole === role).length;
  }

  getVisibleActionTypesCount(): number {
    return new Set(this.filteredHistoryEntries().map(item => item.actionType)).size;
  }

  getCategoryLabel(category?: string | null): string {
    switch (category) {
      case 'REPAS': return 'Repas';
      case 'TRANSPORT': return 'Transport';
      case 'COMPORTEMENT': return 'Comportement';
      case 'HYGIENE': return 'Hygiène';
      case 'SECURITE': return 'Sécurité';
      case 'PERSONNEL': return 'Personnel';
      case 'AUTRE': return 'Autre';
      default: return category || 'Non définie';
    }
  }

  getActionTypeLabel(actionType?: string | null): string {
    switch (actionType) {
      case 'CREATED': return 'Création';
      case 'TITLE_CHANGED': return 'Titre modifié';
      case 'DESCRIPTION_UPDATED': return 'Description mise à jour';
      case 'CATEGORY_CHANGED': return 'Catégorie changée';
      case 'PRIORITY_CHANGED': return 'Priorité changée';
      case 'STATUS_CHANGED': return 'Statut changé';
      case 'ADMIN_COMMENT_ADDED': return 'Commentaire ajouté';
      case 'ADMIN_COMMENT_UPDATED': return 'Commentaire modifié';
      case 'ADMIN_COMMENT_REMOVED': return 'Commentaire supprimé';
      default: return actionType || 'Action';
    }
  }

  getHistoryIcon(actionType: string): string {
    switch (actionType) {
      case 'CREATED': return '+';
      case 'STATUS_CHANGED': return '↻';
      case 'CATEGORY_CHANGED': return '≡';
      case 'PRIORITY_CHANGED': return '!';
      case 'ADMIN_COMMENT_ADDED':
      case 'ADMIN_COMMENT_UPDATED':
      case 'ADMIN_COMMENT_REMOVED':
        return '💬';
      case 'TITLE_CHANGED':
      case 'DESCRIPTION_UPDATED':
        return '✎';
      default:
        return '•';
    }
  }

  getHistoryColorClass(actionType: string): string {
    switch (actionType) {
      case 'CREATED':
        return 'history-blue';
      case 'STATUS_CHANGED':
        return 'history-green';
      case 'CATEGORY_CHANGED':
      case 'PRIORITY_CHANGED':
        return 'history-orange';
      case 'ADMIN_COMMENT_ADDED':
      case 'ADMIN_COMMENT_UPDATED':
      case 'ADMIN_COMMENT_REMOVED':
        return 'history-purple';
      default:
        return 'history-red';
    }
  }
}