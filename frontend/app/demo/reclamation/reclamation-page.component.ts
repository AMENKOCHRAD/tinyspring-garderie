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

        <div class="card shadow-sm border-0 mb-4" *ngIf="isAdmin()">
          <div class="card-body d-flex justify-content-between align-items-center flex-wrap gap-3">
            <div>
              <h4 class="mb-1">Statistiques Réclamations</h4>
              <p class="text-muted mb-0">Vue synthétique des statuts et des priorités</p>
            </div>

            <div class="d-flex gap-2 flex-wrap">
              <button class="btn btn-success" (click)="exportExcel()">
                {{ exportExcelLoading ? 'Export en cours...' : 'Exporter Excel' }}
              </button>

              <button class="btn btn-info text-white" (click)="toggleStats()">
                {{ showStats ? 'Masquer les statistiques' : 'Afficher les statistiques' }}
              </button>
            </div>
          </div>
        </div>

        <div class="card shadow-sm border-0 mb-4" *ngIf="isAdmin() && showStats">
          <div class="card-body">
            <h4 class="mb-4">Tableau de bord statistique</h4>

            <div class="stats-grid">
              <div class="stats-card">
                <h5 class="chart-title">Répartition des réclamations par statut</h5>

                <div class="pie-chart-wrapper">
                  <div class="pie-chart" [style.background]="getStatusPieGradient()"></div>

                  <div class="chart-legend">
                    <div class="legend-item">
                      <span class="legend-color status-open-color"></span>
                      <span>OPEN : {{ getStatusCount('OPEN') }}</span>
                    </div>
                    <div class="legend-item">
                      <span class="legend-color status-progress-color"></span>
                      <span>IN_PROGRESS : {{ getStatusCount('IN_PROGRESS') }}</span>
                    </div>
                    <div class="legend-item">
                      <span class="legend-color status-resolved-color"></span>
                      <span>RESOLVED : {{ getStatusCount('RESOLVED') }}</span>
                    </div>
                    <div class="legend-item">
                      <span class="legend-color status-rejected-color"></span>
                      <span>REJECTED : {{ getStatusCount('REJECTED') }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="stats-card">
                <h5 class="chart-title">Répartition des réclamations par priorité</h5>

                <div class="bar-chart-container">
                  <div class="bar-chart">
                    <div class="bar-group">
                      <div class="bar-area">
                        <div class="bar low-bar" [style.height.%]="getBarHeight(getPriorityCount('LOW'))"></div>
                      </div>
                      <div class="bar-label">LOW</div>
                      <div class="bar-value">{{ getPriorityCount('LOW') }}</div>
                    </div>

                    <div class="bar-group">
                      <div class="bar-area">
                        <div class="bar medium-bar" [style.height.%]="getBarHeight(getPriorityCount('MEDIUM'))"></div>
                      </div>
                      <div class="bar-label">MEDIUM</div>
                      <div class="bar-value">{{ getPriorityCount('MEDIUM') }}</div>
                    </div>

                    <div class="bar-group">
                      <div class="bar-area">
                        <div class="bar high-bar" [style.height.%]="getBarHeight(getPriorityCount('HIGH'))"></div>
                      </div>
                      <div class="bar-label">HIGH</div>
                      <div class="bar-value">{{ getPriorityCount('HIGH') }}</div>
                    </div>
                  </div>

                  <div class="chart-legend mt-3">
                    <div class="legend-item">
                      <span class="legend-color low-bar"></span>
                      <span>LOW</span>
                    </div>
                    <div class="legend-item">
                      <span class="legend-color medium-bar"></span>
                      <span>MEDIUM</span>
                    </div>
                    <div class="legend-item">
                      <span class="legend-color high-bar"></span>
                      <span>HIGH</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card shadow-sm border-0 mb-4" *ngIf="!isAdmin()">
          <div class="card-body">
            <h4 class="mb-3">Créer une réclamation</h4>

            <div *ngIf="createError" class="alert alert-danger">{{ createError }}</div>
            <div *ngIf="createSuccess" class="alert alert-success">{{ createSuccess }}</div>

            <div class="mb-3">
              <label class="form-label">Titre</label>
              <input type="text" class="form-control" [(ngModel)]="newReclamation.title" placeholder="Ex: Problème de repas" />
            </div>

            <div class="mb-3">
              <label class="form-label">Description</label>
              <textarea class="form-control" rows="4" [(ngModel)]="newReclamation.description" placeholder="Décrire votre réclamation..."></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label">Catégorie</label>
              <select class="form-control" [(ngModel)]="newReclamation.category">
                <option value="">-- Choisir une catégorie --</option>
                <option *ngFor="let category of reclamationCategories" [value]="category">
                  {{ getCategoryLabel(category) }}
                </option>
              </select>
            </div>

            <div class="mb-3">
              <label class="form-label">Priorité</label>
              <select class="form-control" [(ngModel)]="newReclamation.priority">
                <option value="">-- Choisir --</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div class="mb-3">
              <label class="form-label">Ajouter une image</label>
              <input type="file" class="form-control" accept="image/*" (change)="onReclamationImageSelected($event)" />
            </div>

            <div *ngIf="selectedReclamationImageName" class="mb-3">
              <small class="text-muted">
                Image sélectionnée : <strong>{{ selectedReclamationImageName }}</strong>
              </small>
            </div>

            <div class="mb-3">
              <label class="form-label">Ajouter une pièce jointe</label>
              <input type="file" class="form-control" (change)="onAttachmentSelected($event)" />
              <small class="text-muted d-block mt-1">
                Formats possibles : PDF, DOC, DOCX, image ou autre fichier.
              </small>
            </div>

            <div *ngIf="selectedAttachmentName" class="mb-3">
              <div class="attachment-preview-inline">
                <span class="attachment-icon">{{ getAttachmentIcon(selectedAttachmentName) }}</span>
                <div class="attachment-meta">
                  <div class="attachment-name">{{ selectedAttachmentName }}</div>
                  <small class="text-muted">Prête à être envoyée avec la réclamation</small>
                </div>
              </div>
            </div>

            <button class="btn btn-primary" (click)="createReclamation()">
              Créer réclamation
            </button>
          </div>
        </div>

        <div class="card shadow-sm border-0 mb-4" *ngIf="editingReclamationId !== null">
          <div class="card-body">
            <h4 class="mb-3">
              {{ isAdmin() ? 'Réponse administrative' : 'Modifier la réclamation' }}
            </h4>

            <div *ngIf="updateError" class="alert alert-danger">{{ updateError }}</div>
            <div *ngIf="updateSuccess" class="alert alert-success">{{ updateSuccess }}</div>

            <ng-container *ngIf="!isAdmin()">
              <div class="mb-3">
                <label class="form-label">Titre</label>
                <input type="text" class="form-control" [(ngModel)]="editedReclamation.title" />
              </div>

              <div class="mb-3">
                <label class="form-label">Description</label>
                <textarea class="form-control" rows="4" [(ngModel)]="editedReclamation.description"></textarea>
              </div>

              <div class="mb-3">
                <label class="form-label">Catégorie</label>
                <select class="form-control" [(ngModel)]="editedReclamation.category">
                  <option value="">-- Choisir une catégorie --</option>
                  <option *ngFor="let category of reclamationCategories" [value]="category">
                    {{ getCategoryLabel(category) }}
                  </option>
                </select>
              </div>

              <div class="mb-3">
                <label class="form-label">Priorité</label>
                <select class="form-control" [(ngModel)]="editedReclamation.priority">
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
            </ng-container>

            <ng-container *ngIf="isAdmin()">
              <div class="admin-response-box mb-3">
                <div class="admin-response-header">
                  <h5 class="mb-1">Réponse de l’administration</h5>
                  <p class="text-muted mb-0">
                    Ajoutez une réponse claire et professionnelle visible par le parent.
                  </p>
                </div>

                <div class="mb-3 mt-3">
                  <label class="form-label">Commentaire administratif</label>
                  <textarea
                    class="form-control admin-comment-textarea"
                    rows="6"
                    [(ngModel)]="editedReclamation.adminComment"
                    [placeholder]="getAdminCommentPlaceholder()"
                  ></textarea>
                </div>
              </div>
            </ng-container>

            <button class="btn btn-success me-2" (click)="updateReclamation()">Enregistrer</button>
            <button class="btn btn-secondary" (click)="cancelEdit()">Annuler</button>
          </div>
        </div>

        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Recherche et filtres</h4>

            <div class="row">
              <div class="col-md-3 mb-3">
                <label class="form-label">Recherche par titre</label>
                <input type="text" class="form-control" [(ngModel)]="searchTitle" placeholder="Chercher une réclamation..." />
              </div>

              <div class="col-md-3 mb-3">
                <label class="form-label">Filtrer par catégorie</label>
                <select class="form-control" [(ngModel)]="filterCategory">
                  <option value="">Toutes</option>
                  <option *ngFor="let category of reclamationCategories" [value]="category">
                    {{ getCategoryLabel(category) }}
                  </option>
                </select>
              </div>

              <div class="col-md-3 mb-3">
                <label class="form-label">Filtrer par statut</label>
                <select class="form-control" [(ngModel)]="filterStatus">
                  <option value="">Tous</option>
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="REJECTED">REJECTED</option>
                </select>
              </div>

              <div class="col-md-3 mb-3">
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

        <div class="card shadow-sm border-0">
          <div class="card-body">
            <h4 class="mb-3">Liste des réclamations</h4>

            <div *ngIf="loading">Chargement...</div>
            <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
            <div *ngIf="deleteSuccess" class="alert alert-success">{{ deleteSuccess }}</div>
            <div *ngIf="statusSuccess" class="alert alert-success">{{ statusSuccess }}</div>

            <div *ngIf="!loading && filteredReclamations().length === 0" class="alert alert-info">
              Aucune réclamation trouvée.
            </div>

            <div *ngIf="isAdmin() && filteredReclamations().length > 0" class="table-responsive">
              <table class="table table-hover table-bordered align-middle">
                <thead class="table-light">
                  <tr>
                    <th>ID</th>
                    <th>Titre</th>
                    <th>Description</th>
                    <th>Catégorie</th>
                    <th>Réponse admin</th>
                    <th>Image</th>
                    <th>Pièce jointe</th>
                    <th>Priorité</th>
                    <th>Statut</th>
                    <th>Date création</th>
                    <th>Changer statut</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let rec of filteredReclamations()">
                    <td>{{ rec.id }}</td>
                    <td>{{ rec.title }}</td>
                    <td>{{ rec.description }}</td>
                    <td>
                      <span class="badge category-badge">{{ getCategoryLabel(rec.category) }}</span>
                    </td>
                    <td style="min-width: 230px;">
                      <div *ngIf="rec.adminComment?.trim(); else noAdminReply" class="admin-comment-preview">
                        {{ rec.adminComment }}
                      </div>
                      <ng-template #noAdminReply>
                        <span class="text-muted">Pas encore de réponse</span>
                      </ng-template>
                    </td>
                    <td>
                      <img
                        *ngIf="rec.imagePath"
                        [src]="getFileUrl(rec.imagePath)"
                        [alt]="rec.imageName || 'image reclamation'"
                        class="reclamation-thumb"
                      />
                      <span *ngIf="!rec.imagePath" class="text-muted">Aucune image</span>
                    </td>
                    <td>
                      <div *ngIf="rec.attachmentPath; else noAttachmentAdmin" class="attachment-box attachment-admin-box">
                        <div class="attachment-top">
                          <span class="attachment-icon">{{ getAttachmentIcon(rec.attachmentName, rec.attachmentType) }}</span>
                          <div class="attachment-meta">
                            <div class="attachment-name">{{ rec.attachmentName || 'Pièce jointe' }}</div>
                            <small class="text-muted">
                              {{ getAttachmentTypeLabel(rec.attachmentName, rec.attachmentType) }}
                            </small>
                          </div>
                        </div>

                        <div class="attachment-admin-actions">
                          <button type="button" class="btn btn-sm btn-outline-primary" (click)="openAttachment(rec.attachmentPath)">
                            Ouvrir
                          </button>

                          <button type="button" class="btn btn-sm btn-outline-success" (click)="downloadAttachment(rec.attachmentPath, rec.attachmentName)">
                            Télécharger
                          </button>
                        </div>
                      </div>

                      <ng-template #noAttachmentAdmin>
                        <span class="text-muted">Aucune pièce jointe</span>
                      </ng-template>
                    </td>
                    <td>
                      <span class="badge"
                        [ngClass]="{
                          'bg-success': rec.priority === 'LOW',
                          'bg-warning text-dark': rec.priority === 'MEDIUM',
                          'bg-danger': rec.priority === 'HIGH'
                        }">
                        {{ rec.priority }}
                      </span>
                    </td>
                    <td>
                      <span class="badge"
                        [ngClass]="{
                          'bg-success': rec.status === 'RESOLVED',
                          'bg-warning text-dark': rec.status === 'IN_PROGRESS',
                          'bg-danger': rec.status === 'REJECTED',
                          'bg-primary': rec.status === 'OPEN'
                        }">
                        {{ rec.status }}
                      </span>
                    </td>
                    <td>{{ rec.createdAt | date:'short' }}</td>
                    <td style="min-width: 180px;">
                      <select class="form-control form-control-sm" [ngModel]="rec.status" (ngModelChange)="changeStatus(rec.id, $event)">
                        <option value="OPEN">OPEN</option>
                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                        <option value="RESOLVED">RESOLVED</option>
                        <option value="REJECTED">REJECTED</option>
                      </select>
                    </td>
                    <td class="text-center" style="min-width: 120px;">
                      <button class="action-btn edit-btn" [title]="isAdmin() ? 'Répondre' : 'Modifier'" (click)="editReclamation(rec)">
                        <i class="feather icon-edit"></i>
                      </button>

                      <button class="action-btn delete-btn" title="Supprimer" (click)="deleteReclamation(rec.id)">
                        <i class="feather icon-trash-2"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="reclamation-list" *ngIf="!isAdmin() && filteredReclamations().length > 0">
              <div class="reclamation-card" *ngFor="let rec of filteredReclamations()">
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
                  <h5 class="mb-0">{{ rec.title }}</h5>
                  <span class="badge category-badge">{{ getCategoryLabel(rec.category) }}</span>
                </div>

                <p class="mb-2">{{ rec.description }}</p>

                <div *ngIf="rec.imagePath" class="mb-3">
                  <img [src]="getFileUrl(rec.imagePath)" [alt]="rec.imageName || 'image reclamation'" class="reclamation-image" />
                </div>

                <div class="mb-3">
                  <label class="form-label">Pièce jointe</label>

                  <div *ngIf="rec.attachmentPath; else noAttachmentParent" class="attachment-box">
                    <div class="attachment-top">
                      <span class="attachment-icon">{{ getAttachmentIcon(rec.attachmentName, rec.attachmentType) }}</span>
                      <div class="attachment-meta">
                        <div class="attachment-name">{{ rec.attachmentName || 'Pièce jointe' }}</div>
                        <small class="text-muted">{{ getAttachmentTypeLabel(rec.attachmentName, rec.attachmentType) }}</small>
                      </div>
                    </div>

                    <div class="attachment-actions">
                      <button type="button" class="btn btn-sm btn-outline-primary" (click)="openAttachment(rec.attachmentPath)">
                        Ouvrir
                      </button>

                      <button type="button" class="btn btn-sm btn-outline-secondary" (click)="downloadAttachment(rec.attachmentPath, rec.attachmentName)">
                        Télécharger
                      </button>
                    </div>
                  </div>

                  <ng-template #noAttachmentParent>
                    <div class="text-muted">Aucune pièce jointe</div>
                  </ng-template>
                </div>

                <div class="admin-response-display mb-3">
                  <div class="admin-response-title">Réponse de l’administration</div>

                  <div *ngIf="rec.adminComment?.trim(); else noParentAdminComment" class="admin-response-content">
                    {{ rec.adminComment }}
                  </div>

                  <ng-template #noParentAdminComment>
                    <div class="admin-response-empty">Pas encore de réponse administrative.</div>
                  </ng-template>
                </div>

                <p class="mb-1">
                  <strong>Priorité :</strong>
                  <span class="badge bg-secondary">{{ rec.priority }}</span>
                </p>

                <p class="mb-2">
                  <strong>Statut actuel :</strong>
                  <span class="badge"
                    [ngClass]="{
                      'bg-success': rec.status === 'RESOLVED',
                      'bg-warning text-dark': rec.status === 'IN_PROGRESS',
                      'bg-danger': rec.status === 'REJECTED',
                      'bg-primary': rec.status === 'OPEN'
                    }">
                    {{ rec.status }}
                  </span>
                </p>

                <div class="mb-3">
                  <label class="form-label">Suivi de traitement</label>
                  <div class="form-control bg-light">{{ rec.status }}</div>
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

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 20px;
    }

    .stats-card {
      background: #f8fafc;
      border: 1px solid #e9ecef;
      border-radius: 16px;
      padding: 20px;
    }

    .chart-title {
      font-weight: 700;
      margin-bottom: 18px;
      color: #1f2937;
    }

    .pie-chart-wrapper {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 18px;
    }

    .pie-chart {
      width: 220px;
      height: 220px;
      border-radius: 50%;
      border: 8px solid #ffffff;
      box-shadow: 0 4px 14px rgba(0,0,0,0.08);
    }

    .chart-legend {
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 14px;
      font-weight: 500;
      color: #374151;
    }

    .legend-color {
      width: 16px;
      height: 16px;
      border-radius: 4px;
      display: inline-block;
    }

    .status-open-color {
      background: #0d6efd;
    }

    .status-progress-color {
      background: #f59e0b;
    }

    .status-resolved-color {
      background: #10b981;
    }

    .status-rejected-color {
      background: #ef4444;
    }

    .bar-chart-container {
      width: 100%;
    }

    .bar-chart {
      height: 280px;
      display: flex;
      align-items: flex-end;
      justify-content: space-around;
      gap: 20px;
      padding: 20px 10px 10px 10px;
      border-left: 2px solid #cbd5e1;
      border-bottom: 2px solid #cbd5e1;
      background: #ffffff;
      border-radius: 12px;
    }

    .bar-group {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 90px;
    }

    .bar-area {
      height: 200px;
      width: 56px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }

    .bar {
      width: 56px;
      border-radius: 12px 12px 0 0;
      transition: all 0.3s ease;
    }

    .low-bar {
      background: #60a5fa;
    }

    .medium-bar {
      background: #fbbf24;
    }

    .high-bar {
      background: #f87171;
    }

    .bar-label {
      margin-top: 10px;
      font-weight: 700;
      color: #374151;
    }

    .bar-value {
      font-size: 13px;
      color: #6b7280;
      margin-top: 4px;
    }

    table th {
      white-space: nowrap;
      font-weight: 700;
    }

    table td {
      vertical-align: middle;
    }

    .table-responsive {
      margin-top: 16px;
    }

    .reclamation-thumb {
      width: 90px;
      height: 70px;
      object-fit: cover;
      border-radius: 8px;
      border: 1px solid #dee2e6;
    }

    .category-badge {
      background: #e0ecff;
      color: #1d4ed8;
      font-weight: 600;
      border: 1px solid #bfd6ff;
    }

    .admin-comment-preview {
      max-height: 90px;
      overflow: auto;
      white-space: pre-wrap;
      line-height: 1.45;
      background: #f8fafc;
      border: 1px solid #dbe7f5;
      border-radius: 10px;
      padding: 10px 12px;
      color: #334155;
    }

    .admin-response-box {
      background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%);
      border: 1px solid #d8e6f5;
      border-radius: 16px;
      padding: 18px;
    }

    .admin-response-header h5 {
      color: #1e3a5f;
      font-weight: 700;
    }

    .admin-comment-textarea {
      resize: vertical;
      min-height: 150px;
    }

    .admin-response-display {
      background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%);
      border: 1px solid #d8e6f5;
      border-radius: 14px;
      padding: 14px;
    }

    .admin-response-title {
      font-weight: 700;
      color: #1e3a5f;
      margin-bottom: 8px;
    }

    .admin-response-content {
      white-space: pre-wrap;
      line-height: 1.55;
      color: #334155;
    }

    .admin-response-empty {
      color: #64748b;
      font-style: italic;
    }

    .attachment-preview-inline {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border: 1px solid #dee2e6;
      border-radius: 12px;
      background: #f8f9fa;
    }

    .attachment-box {
      border: 1px solid #dee2e6;
      border-radius: 12px;
      padding: 12px;
      background: #f8f9fa;
      min-width: 220px;
    }

    .attachment-admin-box {
      min-width: 180px;
    }

    .attachment-top {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 10px;
    }

    .attachment-icon {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #e9ecef;
      font-size: 18px;
      flex-shrink: 0;
    }

    .attachment-meta {
      min-width: 0;
      flex: 1;
    }

    .attachment-name {
      font-weight: 600;
      color: #212529;
      word-break: break-word;
      line-height: 1.3;
    }

    .attachment-actions,
    .attachment-admin-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .action-btn {
      border: none;
      padding: 8px 10px;
      border-radius: 8px;
      margin: 0 4px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-size: 14px;
      color: white;
    }

    .action-btn i {
      font-size: 16px;
    }

    .edit-btn {
      background: #fbbf24;
    }

    .edit-btn:hover {
      background: #f59e0b;
      transform: scale(1.08);
    }

    .delete-btn {
      background: #ef4444;
    }

    .delete-btn:hover {
      background: #dc2626;
      transform: scale(1.08);
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

    .reclamation-image {
      max-width: 100%;
      width: 320px;
      max-height: 240px;
      object-fit: cover;
      border-radius: 12px;
      border: 1px solid #dee2e6;
    }

    @media (max-width: 768px) {
      .reclamation-page {
        padding: 12px;
      }

      .pie-chart {
        width: 180px;
        height: 180px;
      }

      .bar-chart {
        height: 240px;
      }

      .bar-area {
        height: 160px;
      }

      .bar-group {
        width: 70px;
      }

      .bar {
        width: 42px;
      }

      .attachment-actions,
      .attachment-admin-actions {
        flex-direction: column;
      }
    }
  `]
})
export class ReclamationPageComponent implements OnInit {
  reclamations: Reclamation[] = [];

  reclamationCategories: string[] = [
    'REPAS',
    'TRANSPORT',
    'COMPORTEMENT',
    'HYGIENE',
    'SECURITE',
    'PERSONNEL',
    'AUTRE'
  ];

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
  filterCategory = '';
  filterStatus = '';
  filterPriority = '';

  showStats = false;
  exportExcelLoading = false;

  selectedReclamationImage: File | null = null;
  selectedReclamationImageName = '';

  selectedAttachment: File | null = null;
  selectedAttachmentName = '';

  newReclamation = {
    title: '',
    description: '',
    category: '',
    priority: ''
  };

  editedReclamation = {
    title: '',
    description: '',
    category: '',
    priority: '',
    adminComment: ''
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

  toggleStats(): void {
    this.showStats = !this.showStats;
  }

  exportExcel(): void {
    if (!this.isAdmin()) {
      return;
    }

    this.error = '';
    this.exportExcelLoading = true;

    this.messagerieService.exportReclamationsExcel().subscribe({
      next: (blob: Blob) => {
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = blobUrl;
        link.download = 'liste-reclamations.xlsx';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        window.URL.revokeObjectURL(blobUrl);
        this.exportExcelLoading = false;
      },
      error: (err: any) => {
        console.log('Erreur export Excel = ', err);
        this.error = 'Impossible d’exporter le fichier Excel.';
        this.exportExcelLoading = false;
      }
    });
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

  getAdminCommentPlaceholder(): string {
    const current = this.reclamations.find(r => r.id === this.editingReclamationId);

    switch (current?.status) {
      case 'OPEN':
        return 'Ex : Réclamation bien reçue, en attente d’analyse.';
      case 'IN_PROGRESS':
        return 'Ex : Le dossier est en cours de traitement par l’équipe administrative.';
      case 'RESOLVED':
        return 'Ex : Le problème a été traité et corrigé.';
      case 'REJECTED':
        return 'Ex : Après vérification, la demande n’a pas pu être retenue.';
      default:
        return 'Ajoutez une réponse administrative claire et professionnelle.';
    }
  }

  onReclamationImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedReclamationImage = input.files[0];
      this.selectedReclamationImageName = input.files[0].name;
    } else {
      this.selectedReclamationImage = null;
      this.selectedReclamationImageName = '';
    }
  }

  onAttachmentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedAttachment = input.files[0];
      this.selectedAttachmentName = input.files[0].name;
    } else {
      this.selectedAttachment = null;
      this.selectedAttachmentName = '';
    }
  }

  getFileUrl(path?: string | null): string {
    return this.messagerieService.getFullImageUrl(path);
  }

  openAttachment(path?: string | null): void {
    if (!path) return;
    window.open(this.getFileUrl(path), '_blank');
  }

  downloadAttachment(path?: string | null, fileName?: string | null): void {
    if (!path) return;

    this.error = '';

    this.messagerieService.downloadFile(path).subscribe({
      next: (blob: Blob) => {
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = blobUrl;
        link.download = fileName && fileName.trim() ? fileName : 'piece-jointe';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        window.URL.revokeObjectURL(blobUrl);
      },
      error: (err) => {
        console.error('Erreur téléchargement pièce jointe = ', err);
        this.error = 'Impossible de télécharger la pièce jointe.';
      }
    });
  }

  getAttachmentExtension(fileName?: string | null, attachmentType?: string | null): string {
    if (fileName && fileName.includes('.')) {
      return fileName.split('.').pop()!.toLowerCase();
    }

    if (attachmentType) {
      if (attachmentType.includes('pdf')) return 'pdf';
      if (attachmentType.includes('word')) return 'doc';
      if (attachmentType.includes('image')) return 'image';
      if (attachmentType.includes('sheet') || attachmentType.includes('excel')) return 'xls';
      if (attachmentType.includes('zip') || attachmentType.includes('rar')) return 'zip';
    }

    return 'file';
  }

  getAttachmentIcon(fileName?: string | null, attachmentType?: string | null): string {
    const ext = this.getAttachmentExtension(fileName, attachmentType);

    if (['pdf'].includes(ext)) return '📄';
    if (['doc', 'docx'].includes(ext)) return '📝';
    if (['xls', 'xlsx', 'csv'].includes(ext)) return '📊';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'image'].includes(ext)) return '🖼️';
    if (['zip', 'rar', '7z'].includes(ext)) return '🗜️';

    return '📎';
  }

  getAttachmentTypeLabel(fileName?: string | null, attachmentType?: string | null): string {
    const ext = this.getAttachmentExtension(fileName, attachmentType);

    if (['pdf'].includes(ext)) return 'Document PDF';
    if (['doc', 'docx'].includes(ext)) return 'Document Word';
    if (['xls', 'xlsx', 'csv'].includes(ext)) return 'Fichier tableur';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'image'].includes(ext)) return 'Image';
    if (['zip', 'rar', '7z'].includes(ext)) return 'Archive compressée';

    return attachmentType || 'Fichier joint';
  }

  filteredReclamations(): Reclamation[] {
    return this.reclamations.filter((rec) => {
      const matchTitle = !this.searchTitle || rec.title.toLowerCase().includes(this.searchTitle.toLowerCase());
      const matchCategory = !this.filterCategory || rec.category === this.filterCategory;
      const matchStatus = !this.filterStatus || rec.status === this.filterStatus;
      const matchPriority = !this.filterPriority || rec.priority === this.filterPriority;

      return matchTitle && matchCategory && matchStatus && matchPriority;
    });
  }

  getStatusCount(status: string): number {
    return this.reclamations.filter(rec => rec.status === status).length;
  }

  getPriorityCount(priority: string): number {
    return this.reclamations.filter(rec => rec.priority === priority).length;
  }

  getStatusPieGradient(): string {
    const open = this.getStatusCount('OPEN');
    const inProgress = this.getStatusCount('IN_PROGRESS');
    const resolved = this.getStatusCount('RESOLVED');
    const rejected = this.getStatusCount('REJECTED');
    const total = open + inProgress + resolved + rejected;

    if (total === 0) {
      return 'conic-gradient(#e5e7eb 0deg 360deg)';
    }

    const openDeg = (open / total) * 360;
    const progressDeg = (inProgress / total) * 360;
    const resolvedDeg = (resolved / total) * 360;
    const rejectedDeg = (rejected / total) * 360;

    const d1 = openDeg;
    const d2 = d1 + progressDeg;
    const d3 = d2 + resolvedDeg;
    const d4 = d3 + rejectedDeg;

    return `conic-gradient(
      #0d6efd 0deg ${d1}deg,
      #f59e0b ${d1}deg ${d2}deg,
      #10b981 ${d2}deg ${d3}deg,
      #ef4444 ${d3}deg ${d4}deg
    )`;
  }

  getMaxPriorityCount(): number {
    return Math.max(
      this.getPriorityCount('LOW'),
      this.getPriorityCount('MEDIUM'),
      this.getPriorityCount('HIGH'),
      1
    );
  }

  getBarHeight(value: number): number {
    return (value / this.getMaxPriorityCount()) * 100;
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

    if (!this.newReclamation.category) {
      this.createError = 'La catégorie est obligatoire.';
      return;
    }

    if (!this.newReclamation.priority) {
      this.createError = 'La priorité est obligatoire.';
      return;
    }

    this.messagerieService.createReclamation(
      this.newReclamation.title.trim(),
      this.newReclamation.description.trim(),
      this.newReclamation.priority,
      this.newReclamation.category,
      this.selectedReclamationImage,
      this.selectedAttachment
    ).subscribe({
      next: () => {
        this.createSuccess = 'Réclamation créée avec succès.';
        this.newReclamation = {
          title: '',
          description: '',
          category: '',
          priority: ''
        };
        this.selectedReclamationImage = null;
        this.selectedReclamationImageName = '';
        this.selectedAttachment = null;
        this.selectedAttachmentName = '';
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
      category: rec.category || '',
      priority: rec.priority,
      adminComment: rec.adminComment || ''
    };
    this.updateError = '';
    this.updateSuccess = '';
  }

  cancelEdit(): void {
    this.editingReclamationId = null;
    this.editedReclamation = {
      title: '',
      description: '',
      category: '',
      priority: '',
      adminComment: ''
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

    if (this.isAdmin()) {
      this.messagerieService.updateReclamation(this.editingReclamationId, {
        adminComment: this.editedReclamation.adminComment
      }).subscribe({
        next: () => {
          this.updateSuccess = 'Réponse administrative enregistrée avec succès.';
          this.cancelEdit();
          this.loadReclamations();
        },
        error: (err: any) => {
          console.log('Erreur update adminComment = ', err);
          this.updateError = 'Impossible d’enregistrer la réponse administrative.';
        }
      });
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

    if (!this.editedReclamation.category) {
      this.updateError = 'La catégorie est obligatoire.';
      return;
    }

    if (!this.editedReclamation.priority) {
      this.updateError = 'La priorité est obligatoire.';
      return;
    }

    this.messagerieService.updateReclamation(this.editingReclamationId, {
      title: this.editedReclamation.title.trim(),
      description: this.editedReclamation.description.trim(),
      category: this.editedReclamation.category,
      priority: this.editedReclamation.priority
    }).subscribe({
      next: () => {
        this.updateSuccess = 'Réclamation modifiée avec succès.';
        this.cancelEdit();
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