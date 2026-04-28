import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { AuthService } from 'src/app/services/auth.service';
import {
  AdminDashboardResponse,
  EscalationInfoResponse,
  MessagerieService,
  Reclamation,
  RecommendedAdminActionResponse
} from 'src/app/services/messagerie.service';

@Component({
  selector: 'app-reclamation-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ts-page">
      <div class="ts-shell">

        <!-- HEADER -->
        <section class="hero-card">
          <div>
            <div class="eyebrow">TinySpring • Garderie intelligente</div>
            <h1>Centre de contrôle des réclamations</h1>
            <p>
              Priorisation automatique, escalade intelligente, suivi SLA et réponse administrative assistée.
            </p>
          </div>

          <div class="hero-actions">
            <button *ngIf="isAdmin()" class="btn btn-light btn-hero" (click)="exportExcel()">
              {{ exportExcelLoading ? 'Export...' : 'Exporter Excel' }}
            </button>
            <button class="btn btn-dark btn-hero" (click)="loadReclamations()">
              Actualiser
            </button>
          </div>
        </section>

        <!-- ADMIN DASHBOARD -->
        <section *ngIf="isAdmin()" class="dashboard-section">
          <div class="kpi-grid">
            <div class="kpi-card kpi-danger">
              <div class="kpi-icon">🚨</div>
              <div>
                <span>Critiques</span>
                <strong>{{ adminDashboard?.criticalCount || 0 }}</strong>
                <small>Action immédiate</small>
              </div>
            </div>

            <div class="kpi-card kpi-warning">
              <div class="kpi-icon">⏱️</div>
              <div>
                <span>SLA dépassés</span>
                <strong>{{ adminDashboard?.slaBreachedCount || 0 }}</strong>
                <small>Retard à traiter</small>
              </div>
            </div>

            <div class="kpi-card kpi-info">
              <div class="kpi-icon">🧠</div>
              <div>
                <span>Score moyen</span>
                <strong>{{ adminDashboard?.averageSmartScore || 0 }}</strong>
                <small>Max: {{ adminDashboard?.maxSmartScore || 0 }}/100</small>
              </div>
            </div>

            <div class="kpi-card kpi-success">
              <div class="kpi-icon">🔁</div>
              <div>
                <span>Récurrentes</span>
                <strong>{{ adminDashboard?.recurringCount || 0 }}</strong>
                <small>Problèmes répétés</small>
              </div>
            </div>
          </div>

          <div class="smart-dashboard">
            <div class="dashboard-panel">
              <div class="panel-header">
                <h3>File intelligente</h3>
                <span>Tri automatique par score</span>
              </div>

              <div class="priority-stack">
                <div class="priority-row danger">
                  <span>CRITICAL</span>
                  <strong>{{ adminDashboard?.criticalCount || 0 }}</strong>
                  <div class="meter"><i [style.width.%]="getSmartLevelPercent('CRITICAL')"></i></div>
                </div>

                <div class="priority-row warning">
                  <span>HIGH</span>
                  <strong>{{ adminDashboard?.highCount || 0 }}</strong>
                  <div class="meter"><i [style.width.%]="getSmartLevelPercent('HIGH')"></i></div>
                </div>

                <div class="priority-row info">
                  <span>MEDIUM</span>
                  <strong>{{ adminDashboard?.mediumCount || 0 }}</strong>
                  <div class="meter"><i [style.width.%]="getSmartLevelPercent('MEDIUM')"></i></div>
                </div>

                <div class="priority-row success">
                  <span>LOW</span>
                  <strong>{{ adminDashboard?.lowCount || 0 }}</strong>
                  <div class="meter"><i [style.width.%]="getSmartLevelPercent('LOW')"></i></div>
                </div>
              </div>
            </div>

            <div class="dashboard-panel">
              <div class="panel-header">
                <h3>Discipline opérationnelle</h3>
                <span>Suivi du traitement</span>
              </div>

              <div class="status-grid">
                <div><small>OPEN</small><strong>{{ getStatusCount('OPEN') }}</strong></div>
                <div><small>IN PROGRESS</small><strong>{{ getStatusCount('IN_PROGRESS') }}</strong></div>
                <div><small>RESOLVED</small><strong>{{ getStatusCount('RESOLVED') }}</strong></div>
                <div><small>REJECTED</small><strong>{{ getStatusCount('REJECTED') }}</strong></div>
              </div>
            </div>

            <div class="dashboard-panel">
              <div class="panel-header">
                <h3>Décisions IA</h3>
                <span>Répartition automatique</span>
              </div>

              <div class="decision-list" *ngIf="getDecisionStats().length > 0; else noDecisions">
                <div class="decision-item" *ngFor="let item of getDecisionStats()">
                  <span [ngClass]="getDecisionBadgeClass(item.label)">
                    {{ getDecisionLabel(item.label) }}
                  </span>
                  <strong>{{ item.count }}</strong>
                </div>
              </div>

              <ng-template #noDecisions>
                <div class="empty-mini">Aucune décision IA disponible.</div>
              </ng-template>
            </div>
          </div>
        </section>

        <!-- PARENT CREATE FORM -->
        <section class="glass-card" *ngIf="!isAdmin()">
          <div class="section-title">
            <div>
              <h2>Nouvelle réclamation</h2>
              <p>Décrivez le problème. Le système analysera automatiquement la catégorie et la priorité.</p>
            </div>
          </div>

          <div *ngIf="createError" class="alert alert-danger">{{ createError }}</div>
          <div *ngIf="createSuccess" class="alert alert-success">{{ createSuccess }}</div>
          <div *ngIf="contentModeratedInfo" class="alert alert-warning">{{ contentModeratedInfo }}</div>

          <div class="form-grid">
            <div class="form-field span-2">
              <label>Titre</label>
              <input type="text" [(ngModel)]="newReclamation.title" placeholder="Ex : Retard du bus scolaire" />
            </div>

            <div class="form-field span-2">
              <label>Description</label>
              <textarea rows="5" [(ngModel)]="newReclamation.description" placeholder="Expliquez clairement la situation..."></textarea>
            </div>

            <div class="form-field">
              <label>Catégorie</label>
              <select [(ngModel)]="newReclamation.category">
                <option value="">Automatique</option>
                <option *ngFor="let category of reclamationCategories" [value]="category">
                  {{ getCategoryLabel(category) }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <label>Priorité</label>
              <select [(ngModel)]="newReclamation.priority">
                <option value="">Automatique</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div class="form-field">
              <label>Image</label>
              <input type="file" accept="image/*" (change)="onReclamationImageSelected($event)" />
              <small *ngIf="selectedReclamationImageName">{{ selectedReclamationImageName }}</small>
            </div>

            <div class="form-field">
              <label>Pièce jointe</label>
              <input type="file" (change)="onAttachmentSelected($event)" />
              <small *ngIf="selectedAttachmentName">{{ selectedAttachmentName }}</small>
            </div>
          </div>

          <button class="btn-primary-pro mt-3" (click)="createReclamation()">
            Envoyer la réclamation
          </button>
        </section>

        <!-- ADMIN/PARENT EDIT PANEL -->
        <section class="glass-card editor-card" *ngIf="editingReclamationId !== null">
          <div class="section-title">
            <div>
              <h2>{{ isAdmin() ? 'Traitement administratif intelligent' : 'Modifier la réclamation' }}</h2>
              <p>
                {{ isAdmin()
                  ? 'Analyse automatique, recommandation de service et réponse au parent.'
                  : 'Vous pouvez modifier une réclamation non clôturée.' }}
              </p>
            </div>
            <button class="btn-close-pro" (click)="cancelEdit()">Fermer</button>
          </div>

          <div *ngIf="updateError" class="alert alert-danger">{{ updateError }}</div>
          <div *ngIf="updateSuccess" class="alert alert-success">{{ updateSuccess }}</div>
          <div *ngIf="updateModeratedInfo" class="alert alert-warning">{{ updateModeratedInfo }}</div>

          <ng-container *ngIf="isAdmin(); else parentEdit">
            <div class="admin-workflow">
              <div class="workflow-main">
                <div class="escalation-card" *ngIf="selectedEscalation">
                  <div class="escalation-head">
                    <div>
                      <h3>Escalade intelligente</h3>
                      <p>Décision métier générée par le backend.</p>
                    </div>

                    <span class="escalation-pill" [class.active]="selectedEscalation.autoEscalated">
                      {{ selectedEscalation.autoEscalated ? 'ESCALADÉE' : 'SURVEILLANCE' }}
                    </span>
                  </div>

                  <div class="escalation-grid">
                    <div>
                      <small>Service</small>
                      <strong>{{ getRecommendedServiceLabel(selectedEscalation.recommendedService) }}</strong>
                    </div>

                    <div>
                      <small>Urgence</small>
                      <strong [ngClass]="getUrgencyTextClass(selectedEscalation.recommendedUrgency)">
                        {{ selectedEscalation.recommendedUrgency || '-' }}
                      </strong>
                    </div>

                    <div>
                      <small>Délai</small>
                      <strong>{{ getRecommendedDelayLabel(selectedEscalation.recommendedDelay) }}</strong>
                    </div>

                    <div>
                      <small>Score</small>
                      <strong>{{ selectedEscalation.smartPriorityScore || 0 }}/100 - {{ selectedEscalation.smartPriorityLevel || 'LOW' }}</strong>
                    </div>
                  </div>

                  <div class="reason-box">
                    <b>Raison :</b> {{ selectedEscalation.escalationReason || '-' }}
                  </div>

                  <div class="reason-box action">
                    <b>Action :</b> {{ selectedEscalation.recommendedAction || '-' }}
                  </div>
                </div>

                <div class="recommended-action-box" *ngIf="recommendedAction">
                  <h3>Action admin recommandée</h3>
                  <div class="mini-grid">
                    <span>Service : <b>{{ getRecommendedServiceLabel(recommendedAction.recommendedService) }}</b></span>
                    <span>Urgence : <b>{{ recommendedAction.recommendedUrgency }}</b></span>
                    <span>Délai : <b>{{ getRecommendedDelayLabel(recommendedAction.recommendedDelay) }}</b></span>
                  </div>
                  <p>{{ recommendedAction.recommendedAction }}</p>
                </div>

                <div class="response-editor">
                  <div class="d-flex flex-wrap gap-2 mb-3">
                    <button type="button" class="btn btn-outline-primary" (click)="generateSuggestedResponse()">
                      Suggérer une réponse
                    </button>

                    <button type="button" class="btn btn-outline-secondary" (click)="loadEscalationInfo(editingReclamationId!)">
                      Recharger l'escalade
                    </button>
                  </div>

                  <label>Commentaire administratif visible par le parent</label>
                  <textarea rows="7" [(ngModel)]="editedReclamation.adminComment" [placeholder]="getAdminCommentPlaceholder()"></textarea>
                </div>

                <div class="actions-row">
                  <button class="btn-primary-pro" (click)="updateReclamation()">Enregistrer la réponse</button>
                  <button class="btn-secondary-pro" (click)="cancelEdit()">Annuler</button>
                </div>
              </div>

              <aside class="workflow-side" *ngIf="getCurrentEditingReclamation() as current">
                <h4>Résumé réclamation</h4>
                <div class="summary-item"><span>ID</span><b>#{{ current.id }}</b></div>
                <div class="summary-item"><span>Statut</span><b>{{ current.status }}</b></div>
                <div class="summary-item"><span>Catégorie</span><b>{{ getCategoryLabel(current.category) }}</b></div>
                <div class="summary-item"><span>Priorité</span><b>{{ current.priority }}</b></div>
                <div class="summary-item"><span>Score smart</span><b>{{ current.smartPriorityScore || 0 }}/100</b></div>

                <div class="summary-block">
                  <span>Décision ML</span>
                  <b>{{ getDecisionLabel(current.decisionRecommendation) }}</b>
                </div>

                <div class="summary-block" *ngIf="current.smartPriorityReason">
                  <span>Explication score</span>
                  <p>{{ current.smartPriorityReason }}</p>
                </div>
              </aside>
            </div>
          </ng-container>

          <ng-template #parentEdit>
            <div class="form-grid">
              <div class="form-field span-2">
                <label>Titre</label>
                <input type="text" [(ngModel)]="editedReclamation.title" />
              </div>

              <div class="form-field span-2">
                <label>Description</label>
                <textarea rows="5" [(ngModel)]="editedReclamation.description"></textarea>
              </div>

              <div class="form-field">
                <label>Catégorie</label>
                <select [(ngModel)]="editedReclamation.category">
                  <option value="">Automatique</option>
                  <option *ngFor="let category of reclamationCategories" [value]="category">
                    {{ getCategoryLabel(category) }}
                  </option>
                </select>
              </div>

              <div class="form-field">
                <label>Priorité</label>
                <select [(ngModel)]="editedReclamation.priority">
                  <option value="">Automatique</option>
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                </select>
              </div>
            </div>

            <div class="actions-row">
              <button class="btn-primary-pro" (click)="updateReclamation()">Enregistrer</button>
              <button class="btn-secondary-pro" (click)="cancelEdit()">Annuler</button>
            </div>
          </ng-template>
        </section>

        <!-- FILTERS -->
        <section class="filter-card">
          <div class="filter-header">
            <div>
              <h2>Recherche & pilotage</h2>
              <p>{{ filteredReclamations().length }} résultat(s) sur {{ reclamations.length }}</p>
            </div>

            <div class="view-toggle" *ngIf="isAdmin()">
              <button [class.active]="viewMode === 'cards'" (click)="viewMode = 'cards'">Cards</button>
              <button [class.active]="viewMode === 'table'" (click)="viewMode = 'table'">Table</button>
            </div>
          </div>

          <div class="filter-grid">
            <div class="form-field">
              <label>Recherche</label>
              <input type="text" [(ngModel)]="searchTitle" (ngModelChange)="goFirstPage()" placeholder="Titre ou description..." />
            </div>

            <div class="form-field">
              <label>Catégorie</label>
              <select [(ngModel)]="filterCategory" (ngModelChange)="goFirstPage()">
                <option value="">Toutes</option>
                <option *ngFor="let category of reclamationCategories" [value]="category">
                  {{ getCategoryLabel(category) }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <label>Statut</label>
              <select [(ngModel)]="filterStatus" (ngModelChange)="goFirstPage()">
                <option value="">Tous</option>
                <option value="OPEN">OPEN</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="RESOLVED">RESOLVED</option>
                <option value="REJECTED">REJECTED</option>
              </select>
            </div>

            <div class="form-field">
              <label>Priorité</label>
              <select [(ngModel)]="filterPriority" (ngModelChange)="goFirstPage()">
                <option value="">Toutes</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </div>

            <div class="form-field" *ngIf="isAdmin()">
              <label>Niveau smart</label>
              <select [(ngModel)]="filterSmartLevel" (ngModelChange)="goFirstPage()">
                <option value="">Tous</option>
                <option value="CRITICAL">CRITICAL</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>

            <div class="form-field" *ngIf="isAdmin()">
              <label>Escalade</label>
              <select [(ngModel)]="filterEscalation" (ngModelChange)="goFirstPage()">
                <option value="">Toutes</option>
                <option value="ESCALATED">Escaladées</option>
                <option value="RECURRENT">Récurrentes</option>
                <option value="UNANSWERED">Sans réponse</option>
              </select>
            </div>
          </div>
        </section>

        <!-- LIST -->
        <section class="list-card">
          <div class="list-header">
            <div>
              <h2>Liste des réclamations</h2>
              <p *ngIf="isAdmin()">Les plus urgentes remontent automatiquement en haut.</p>
              <p *ngIf="!isAdmin()">Suivez l'état et la réponse de l'administration.</p>
            </div>

            <div class="page-size">
              <span>Par page</span>
              <select [(ngModel)]="pageSize" (ngModelChange)="goFirstPage()">
                <option [ngValue]="5">5</option>
                <option [ngValue]="10">10</option>
                <option [ngValue]="20">20</option>
              </select>
            </div>
          </div>

          <div *ngIf="loading" class="loading-box">Chargement des réclamations...</div>
          <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
          <div *ngIf="deleteSuccess" class="alert alert-success">{{ deleteSuccess }}</div>
          <div *ngIf="statusSuccess" class="alert alert-success">{{ statusSuccess }}</div>

          <div *ngIf="!loading && paginatedReclamations().length === 0" class="empty-state">
            <div>📭</div>
            <h3>Aucune réclamation trouvée</h3>
            <p>Essayez de modifier les filtres ou d’actualiser la page.</p>
          </div>

          <!-- ADMIN CARDS -->
          <div *ngIf="isAdmin() && viewMode === 'cards'" class="reclamation-grid">
            <article class="reclamation-card-pro" *ngFor="let rec of paginatedReclamations()" [class.card-critical]="rec.smartPriorityLevel === 'CRITICAL'" [class.card-overdue]="isOverdue(rec)">
              <div class="rec-top">
                <div>
                  <span class="rec-id">#{{ rec.id }}</span>
                  <h3>{{ rec.title }}</h3>
                  <p>{{ rec.description }}</p>
                </div>

                <div class="score-chip" [ngClass]="getSmartLevelSoftClass(rec.smartPriorityLevel)">
                  <strong>{{ rec.smartPriorityScore || 0 }}</strong>
                  <span>{{ rec.smartPriorityLevel || 'LOW' }}</span>
                </div>
              </div>

              <div class="chip-row">
                <span class="chip">{{ getCategoryLabel(rec.category) }}</span>
                <span class="chip" [ngClass]="getPrioritySoftClass(rec.priority)">{{ rec.priority }}</span>
                <span class="chip" [ngClass]="getStatusSoftClass(rec.status)">{{ rec.status }}</span>
                <span *ngIf="rec.autoClassified" class="chip auto">Auto ML</span>
                <span *ngIf="rec.recurring" class="chip recurring">🔁 {{ rec.recurrenceCount || 0 }}</span>
              </div>

              <div class="rec-insights">
                <div>
                  <small>Catégorie ML</small>
                  <b>{{ getCategoryLabel(rec.predictedCategory) }}</b>
                  <span>{{ getConfidencePercent(rec.classificationConfidence) }}</span>
                </div>

                <div>
                  <small>Priorité ML</small>
                  <b>{{ rec.predictedPriority || '-' }}</b>
                  <span>{{ getConfidencePercent(rec.priorityConfidence) }}</span>
                </div>

                <div>
                  <small>Décision</small>
                  <b>{{ getDecisionLabel(rec.decisionRecommendation) }}</b>
                  <span>{{ getConfidencePercent(rec.decisionConfidence) }}</span>
                </div>
              </div>

              <div class="sla-line">
                <span [ngClass]="getSlaBadgeClass(rec)">{{ getSlaLabel(rec) }}</span>
                <span *ngIf="rec.adminComment?.trim()" class="answered">Réponse ajoutée</span>
                <span *ngIf="!rec.adminComment?.trim()" class="not-answered">Sans réponse</span>
              </div>

              <div class="rec-actions">
                <select [ngModel]="rec.status" (ngModelChange)="changeStatus(rec.id, $event)">
                  <option value="OPEN">OPEN</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="RESOLVED">RESOLVED</option>
                  <option value="REJECTED">REJECTED</option>
                </select>

                <button class="btn-edit" (click)="editReclamation(rec)">Traiter</button>
                <button class="btn-delete" (click)="deleteReclamation(rec.id)">Supprimer</button>
              </div>
            </article>
          </div>

          <!-- ADMIN TABLE -->
          <div *ngIf="isAdmin() && viewMode === 'table' && paginatedReclamations().length > 0" class="table-responsive">
            <table class="table table-hover align-middle pro-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Réclamation</th>
                  <th>Score</th>
                  <th>Catégorie</th>
                  <th>ML</th>
                  <th>Décision</th>
                  <th>Statut</th>
                  <th>SLA</th>
                  <th>Réponse</th>
                  <th>Changer statut</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                <tr *ngFor="let rec of paginatedReclamations()" [class.overdue-row]="isOverdue(rec)">
                  <td>#{{ rec.id }}</td>
                  <td>
                    <b>{{ rec.title }}</b>
                    <p>{{ rec.description }}</p>
                  </td>
                  <td>
                    <span class="table-score" [ngClass]="getSmartLevelSoftClass(rec.smartPriorityLevel)">
                      {{ rec.smartPriorityScore || 0 }} • {{ rec.smartPriorityLevel || 'LOW' }}
                    </span>
                  </td>
                  <td>{{ getCategoryLabel(rec.category) }}</td>
                  <td>
                    <div>Cat: {{ getCategoryLabel(rec.predictedCategory) }}</div>
                    <div>Prio: {{ rec.predictedPriority || '-' }}</div>
                  </td>
                  <td>{{ getDecisionLabel(rec.decisionRecommendation) }}</td>
                  <td><span class="chip" [ngClass]="getStatusSoftClass(rec.status)">{{ rec.status }}</span></td>
                  <td><span [ngClass]="getSlaBadgeClass(rec)">{{ getSlaLabel(rec) }}</span></td>
                  <td>{{ rec.adminComment?.trim() ? 'Oui' : 'Non' }}</td>
                  <td>
                    <select [ngModel]="rec.status" (ngModelChange)="changeStatus(rec.id, $event)">
                      <option value="OPEN">OPEN</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="RESOLVED">RESOLVED</option>
                      <option value="REJECTED">REJECTED</option>
                    </select>
                  </td>
                  <td>
                    <button class="btn-edit mini" (click)="editReclamation(rec)">Traiter</button>
                    <button class="btn-delete mini" (click)="deleteReclamation(rec.id)">Supprimer</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- PARENT CARDS -->
          <div *ngIf="!isAdmin()" class="parent-list">
            <article class="parent-card" *ngFor="let rec of paginatedReclamations()">
              <div class="parent-card-head">
                <div>
                  <h3>{{ rec.title }}</h3>
                  <p>{{ rec.description }}</p>
                </div>

                <span class="chip" [ngClass]="getStatusSoftClass(rec.status)">
                  {{ rec.status }}
                </span>
              </div>

              <div class="chip-row">
                <span class="chip">{{ getCategoryLabel(rec.category) }}</span>
                <span class="chip" [ngClass]="getPrioritySoftClass(rec.priority)">{{ rec.priority }}</span>
                <span *ngIf="rec.predictedCategory" class="chip auto">
                  ML: {{ getCategoryLabel(rec.predictedCategory) }}
                </span>
              </div>

              <div *ngIf="rec.imagePath" class="mt-3">
                <img [src]="getFileUrl(rec.imagePath)" [alt]="rec.imageName || 'image réclamation'" class="parent-image" />
              </div>

              <div *ngIf="rec.attachmentPath" class="attachment-line">
                <span>{{ getAttachmentIcon(rec.attachmentName, rec.attachmentType) }}</span>
                <b>{{ rec.attachmentName || 'Pièce jointe' }}</b>
                <button (click)="openAttachment(rec.attachmentPath)">Ouvrir</button>
                <button (click)="downloadAttachment(rec.attachmentPath, rec.attachmentName)">Télécharger</button>
              </div>

              <div class="parent-response">
                <b>Réponse administration</b>
                <p *ngIf="rec.adminComment?.trim(); else noParentResponse">{{ rec.adminComment }}</p>
                <ng-template #noParentResponse>
                  <p class="empty-text">Pas encore de réponse administrative.</p>
                </ng-template>
              </div>

              <div class="parent-actions" *ngIf="rec.status !== 'RESOLVED' && rec.status !== 'REJECTED'">
                <button class="btn-edit" (click)="editReclamation(rec)">Modifier</button>
                <button class="btn-delete" (click)="deleteReclamation(rec.id)">Supprimer</button>
              </div>
            </article>
          </div>

          <!-- PAGINATION -->
          <div class="pagination-pro" *ngIf="filteredReclamations().length > pageSize">
            <button [disabled]="currentPage === 1" (click)="previousPage()">Précédent</button>

            <button
              *ngFor="let page of getVisiblePages()"
              [class.active]="page === currentPage"
              (click)="setPage(page)">
              {{ page }}
            </button>

            <button [disabled]="currentPage === totalPages()" (click)="nextPage()">Suivant</button>

            <span>
              Page {{ currentPage }} / {{ totalPages() }}
            </span>
          </div>
        </section>

      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      background: #eef3f8;
      min-height: 100vh;
    }

    .ts-page {
      padding: 28px;
      box-sizing: border-box;
    }

    .ts-shell {
      width: 100%;
      max-width: 1440px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 22px;
    }

    .hero-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 20px;
      padding: 28px;
      border-radius: 28px;
      background:
        radial-gradient(circle at top right, rgba(255,255,255,0.45), transparent 34%),
        linear-gradient(135deg, #0f3d5e, #105f78 48%, #22a6a8);
      color: white;
      box-shadow: 0 22px 55px rgba(15, 61, 94, 0.25);
    }

    .eyebrow {
      text-transform: uppercase;
      letter-spacing: 0.14em;
      font-size: 12px;
      opacity: 0.85;
      font-weight: 700;
      margin-bottom: 8px;
    }

    .hero-card h1 {
      margin: 0;
      font-size: clamp(28px, 4vw, 46px);
      font-weight: 900;
      letter-spacing: -0.04em;
    }

    .hero-card p {
      margin: 10px 0 0;
      color: rgba(255,255,255,0.88);
      max-width: 720px;
      font-size: 16px;
    }

    .hero-actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      justify-content: flex-end;
    }

    .btn-hero {
      border-radius: 999px;
      padding: 10px 18px;
      font-weight: 800;
    }

    .dashboard-section {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 16px;
    }

    .kpi-card {
      background: rgba(255,255,255,0.9);
      border: 1px solid rgba(255,255,255,0.85);
      border-radius: 24px;
      padding: 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 14px 40px rgba(15, 23, 42, 0.08);
    }

    .kpi-icon {
      width: 54px;
      height: 54px;
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      background: white;
      box-shadow: inset 0 0 0 1px rgba(15,23,42,0.05);
    }

    .kpi-card span {
      display: block;
      color: #64748b;
      font-size: 13px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .kpi-card strong {
      display: block;
      color: #0f172a;
      font-size: 34px;
      line-height: 1;
      margin: 5px 0;
    }

    .kpi-card small {
      color: #64748b;
      font-weight: 600;
    }

    .kpi-danger { border-left: 5px solid #ef4444; }
    .kpi-warning { border-left: 5px solid #f59e0b; }
    .kpi-info { border-left: 5px solid #06b6d4; }
    .kpi-success { border-left: 5px solid #10b981; }

    .smart-dashboard {
      display: grid;
      grid-template-columns: 1.2fr 1fr 1fr;
      gap: 16px;
    }

    .dashboard-panel,
    .glass-card,
    .filter-card,
    .list-card {
      background: rgba(255,255,255,0.92);
      border: 1px solid rgba(226,232,240,0.95);
      border-radius: 26px;
      box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
    }

    .dashboard-panel {
      padding: 20px;
    }

    .panel-header,
    .section-title,
    .filter-header,
    .list-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 18px;
    }

    .panel-header h3,
    .section-title h2,
    .filter-header h2,
    .list-header h2 {
      margin: 0;
      color: #0f172a;
      font-weight: 900;
      letter-spacing: -0.03em;
    }

    .panel-header span,
    .section-title p,
    .filter-header p,
    .list-header p {
      color: #64748b;
      margin: 4px 0 0;
      font-size: 14px;
    }

    .priority-stack {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .priority-row {
      display: grid;
      grid-template-columns: 88px 38px 1fr;
      align-items: center;
      gap: 10px;
      font-weight: 800;
      color: #334155;
    }

    .meter {
      height: 10px;
      background: #e2e8f0;
      border-radius: 999px;
      overflow: hidden;
    }

    .meter i {
      display: block;
      height: 100%;
      min-width: 4px;
      border-radius: inherit;
    }

    .priority-row.danger i { background: #ef4444; }
    .priority-row.warning i { background: #f59e0b; }
    .priority-row.info i { background: #06b6d4; }
    .priority-row.success i { background: #10b981; }

    .status-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
    }

    .status-grid div {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 18px;
      padding: 14px;
    }

    .status-grid small {
      display: block;
      color: #64748b;
      font-weight: 800;
      font-size: 12px;
    }

    .status-grid strong {
      display: block;
      margin-top: 8px;
      color: #0f172a;
      font-size: 26px;
    }

    .decision-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .decision-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 10px 12px;
      border-radius: 14px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
    }

    .decision-item span,
    .chip {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      padding: 6px 10px;
      font-size: 12px;
      font-weight: 800;
      background: #e2e8f0;
      color: #334155;
      white-space: nowrap;
    }

    .glass-card,
    .filter-card,
    .list-card {
      padding: 24px;
    }

    .form-grid,
    .filter-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
    }

    .filter-grid {
      grid-template-columns: repeat(6, minmax(150px, 1fr));
    }

    .span-2 {
      grid-column: span 2;
    }

    .form-field {
      display: flex;
      flex-direction: column;
      gap: 7px;
    }

    .form-field label,
    .response-editor label {
      color: #334155;
      font-size: 13px;
      font-weight: 900;
    }

    .form-field input,
    .form-field select,
    .form-field textarea,
    .response-editor textarea,
    .rec-actions select,
    .page-size select,
    .pro-table select {
      width: 100%;
      border: 1px solid #dbe4ef;
      background: #fff;
      color: #0f172a;
      border-radius: 14px;
      padding: 10px 12px;
      outline: none;
      transition: 0.2s ease;
    }

    .form-field input:focus,
    .form-field select:focus,
    .form-field textarea:focus,
    .response-editor textarea:focus {
      border-color: #22a6a8;
      box-shadow: 0 0 0 4px rgba(34,166,168,0.12);
    }

    .btn-primary-pro,
    .btn-secondary-pro,
    .btn-close-pro,
    .btn-edit,
    .btn-delete {
      border: none;
      border-radius: 14px;
      padding: 10px 14px;
      font-weight: 900;
      cursor: pointer;
      transition: 0.2s ease;
    }

    .btn-primary-pro {
      color: white;
      background: linear-gradient(135deg, #0ea5e9, #14b8a6);
    }

    .btn-secondary-pro,
    .btn-close-pro {
      background: #e2e8f0;
      color: #334155;
    }

    .btn-edit {
      color: white;
      background: #0ea5e9;
    }

    .btn-delete {
      color: white;
      background: #ef4444;
    }

    .mini {
      padding: 7px 9px;
      font-size: 12px;
      margin-right: 4px;
    }

    .btn-primary-pro:hover,
    .btn-edit:hover,
    .btn-delete:hover,
    .btn-secondary-pro:hover,
    .btn-close-pro:hover {
      transform: translateY(-1px);
      filter: brightness(0.98);
    }

    .admin-workflow {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 320px;
      gap: 18px;
    }

    .workflow-main {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .escalation-card {
      background: linear-gradient(135deg, #fff7ed, #ffffff);
      border: 1px solid #fdba74;
      border-radius: 24px;
      padding: 18px;
    }

    .escalation-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 14px;
      margin-bottom: 14px;
    }

    .escalation-head h3,
    .recommended-action-box h3 {
      margin: 0;
      color: #9a3412;
      font-weight: 900;
    }

    .escalation-head p {
      color: #9a3412;
      margin: 4px 0 0;
    }

    .escalation-pill {
      background: #fbbf24;
      color: #78350f;
      border-radius: 999px;
      padding: 8px 12px;
      font-weight: 900;
      font-size: 12px;
    }

    .escalation-pill.active {
      background: #ef4444;
      color: white;
    }

    .escalation-grid,
    .mini-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 12px;
    }

    .escalation-grid div,
    .recommended-action-box,
    .summary-item,
    .summary-block,
    .reason-box {
      background: rgba(255,255,255,0.75);
      border: 1px solid rgba(251,146,60,0.35);
      border-radius: 16px;
      padding: 12px;
    }

    .escalation-grid small,
    .summary-item span,
    .summary-block span {
      display: block;
      color: #64748b;
      font-size: 12px;
      font-weight: 900;
      margin-bottom: 5px;
    }

    .escalation-grid strong {
      color: #0f172a;
    }

    .reason-box {
      color: #431407;
      margin-top: 10px;
      line-height: 1.5;
    }

    .reason-box.action {
      background: #fff;
    }

    .recommended-action-box {
      border-color: #bfdbfe;
      background: #eff6ff;
    }

    .recommended-action-box h3 {
      color: #1d4ed8;
    }

    .recommended-action-box p {
      margin: 10px 0 0;
      color: #1e3a8a;
    }

    .response-editor {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 22px;
      padding: 18px;
    }

    .response-editor textarea {
      min-height: 170px;
      resize: vertical;
      margin-top: 8px;
    }

    .workflow-side {
      background: #0f172a;
      color: white;
      border-radius: 24px;
      padding: 18px;
      align-self: start;
      position: sticky;
      top: 18px;
    }

    .workflow-side h4 {
      font-weight: 900;
      margin: 0 0 14px;
    }

    .summary-item,
    .summary-block {
      background: rgba(255,255,255,0.08);
      border-color: rgba(255,255,255,0.1);
      margin-bottom: 10px;
    }

    .summary-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .summary-block p {
      margin: 5px 0 0;
      color: #cbd5e1;
      font-size: 13px;
      line-height: 1.45;
    }

    .actions-row {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-top: 16px;
    }

    .view-toggle {
      background: #e2e8f0;
      padding: 4px;
      border-radius: 999px;
      display: flex;
      gap: 4px;
    }

    .view-toggle button {
      border: none;
      border-radius: 999px;
      padding: 8px 12px;
      background: transparent;
      font-weight: 900;
      color: #475569;
    }

    .view-toggle button.active {
      background: white;
      color: #0f172a;
      box-shadow: 0 4px 16px rgba(15,23,42,0.08);
    }

    .page-size {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #64748b;
      font-weight: 800;
    }

    .reclamation-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
    }

    .reclamation-card-pro,
    .parent-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 24px;
      padding: 18px;
      box-shadow: 0 12px 30px rgba(15,23,42,0.06);
    }

    .card-critical {
      border-color: #fca5a5;
      box-shadow: 0 18px 45px rgba(239,68,68,0.12);
    }

    .card-overdue {
      background: #fff7f7;
    }

    .rec-top,
    .parent-card-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 14px;
      margin-bottom: 12px;
    }

    .rec-id {
      display: inline-block;
      color: #64748b;
      font-weight: 900;
      font-size: 12px;
      margin-bottom: 4px;
    }

    .rec-top h3,
    .parent-card h3 {
      margin: 0;
      color: #0f172a;
      font-weight: 900;
      font-size: 18px;
      line-height: 1.2;
    }

    .rec-top p,
    .parent-card p {
      margin: 6px 0 0;
      color: #64748b;
      line-height: 1.45;
    }

    .score-chip {
      width: 82px;
      height: 82px;
      border-radius: 22px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      flex: 0 0 auto;
    }

    .score-chip strong {
      font-size: 24px;
      color: #0f172a;
      line-height: 1;
    }

    .score-chip span {
      font-size: 11px;
      font-weight: 900;
      color: #334155;
      margin-top: 4px;
    }

    .chip-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 12px 0;
    }

    .rec-insights {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
      margin: 14px 0;
    }

    .rec-insights div {
      background: #f8fafc;
      border-radius: 16px;
      padding: 11px;
      border: 1px solid #e2e8f0;
      min-width: 0;
    }

    .rec-insights small {
      display: block;
      color: #64748b;
      font-weight: 900;
      font-size: 11px;
    }

    .rec-insights b {
      display: block;
      color: #0f172a;
      margin: 4px 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .rec-insights span {
      color: #64748b;
      font-size: 12px;
      font-weight: 700;
    }

    .sla-line {
      display: flex;
      justify-content: space-between;
      gap: 10px;
      flex-wrap: wrap;
      align-items: center;
      margin-top: 12px;
    }

    .sla-line > span,
    .answered,
    .not-answered {
      border-radius: 999px;
      padding: 6px 10px;
      font-weight: 900;
      font-size: 12px;
    }

    .answered {
      color: #065f46;
      background: #d1fae5;
    }

    .not-answered {
      color: #92400e;
      background: #fef3c7;
    }

    .rec-actions,
    .parent-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      margin-top: 14px;
    }

    .rec-actions select {
      flex: 1;
      min-width: 145px;
    }

    .parent-list {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
    }

    .parent-image {
      width: 100%;
      max-height: 280px;
      object-fit: cover;
      border-radius: 18px;
      border: 1px solid #e2e8f0;
    }

    .attachment-line {
      margin-top: 12px;
      padding: 12px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .attachment-line button {
      border: none;
      border-radius: 999px;
      background: #e0f2fe;
      color: #0369a1;
      font-weight: 800;
      padding: 6px 10px;
    }

    .parent-response {
      margin-top: 14px;
      background: #f0f9ff;
      border: 1px solid #bae6fd;
      border-radius: 18px;
      padding: 14px;
    }

    .parent-response b {
      color: #075985;
    }

    .empty-text,
    .empty-mini {
      color: #64748b;
      font-style: italic;
    }

    .pro-table {
      background: white;
      border-radius: 18px;
      overflow: hidden;
      border-collapse: separate;
      border-spacing: 0;
    }

    .pro-table thead th {
      background: #0f172a;
      color: white;
      font-weight: 900;
      white-space: nowrap;
      border: none;
      padding: 14px;
    }

    .pro-table td {
      padding: 13px;
      border-bottom: 1px solid #e2e8f0;
    }

    .pro-table td p {
      margin: 4px 0 0;
      color: #64748b;
      max-width: 300px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .table-score {
      border-radius: 999px;
      padding: 7px 10px;
      font-weight: 900;
      white-space: nowrap;
    }

    .pagination-pro {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 20px;
      flex-wrap: wrap;
    }

    .pagination-pro button {
      border: none;
      border-radius: 12px;
      padding: 8px 12px;
      background: #e2e8f0;
      color: #334155;
      font-weight: 900;
      cursor: pointer;
    }

    .pagination-pro button.active {
      background: #0ea5e9;
      color: white;
    }

    .pagination-pro button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .pagination-pro span {
      color: #64748b;
      font-weight: 800;
      margin-left: 8px;
    }

    .loading-box,
    .empty-state {
      text-align: center;
      padding: 38px;
      background: #f8fafc;
      border: 1px dashed #cbd5e1;
      border-radius: 22px;
      color: #64748b;
    }

    .empty-state div {
      font-size: 42px;
      margin-bottom: 8px;
    }

    .empty-state h3 {
      color: #0f172a;
      font-weight: 900;
      margin: 0;
    }

    .empty-state p {
      margin: 6px 0 0;
    }

    .soft-critical,
    .soft-danger {
      background: #fee2e2;
      color: #991b1b;
    }

    .soft-high,
    .soft-warning {
      background: #fef3c7;
      color: #92400e;
    }

    .soft-medium,
    .soft-info {
      background: #cffafe;
      color: #155e75;
    }

    .soft-low,
    .soft-success {
      background: #d1fae5;
      color: #065f46;
    }

    .chip.auto {
      background: #ede9fe;
      color: #6d28d9;
    }

    .chip.recurring {
      background: #ffedd5;
      color: #c2410c;
    }

    .decision-repair {
      background: #dbeafe;
      color: #1d4ed8;
    }

    .decision-supervision {
      background: #fee2e2;
      color: #b91c1c;
    }

    .decision-training {
      background: #ede9fe;
      color: #6d28d9;
    }

    .decision-process {
      background: #fef3c7;
      color: #92400e;
    }

    .decision-admin {
      background: #e0f2fe;
      color: #0369a1;
    }

    .decision-medical {
      background: #ffe4e6;
      color: #be123c;
    }

    .decision-transport {
      background: #dcfce7;
      color: #166534;
    }

    .decision-followup {
      background: #f3f4f6;
      color: #374151;
    }

    .text-urgent {
      color: #dc2626 !important;
    }

    .text-high {
      color: #d97706 !important;
    }

    .text-normal {
      color: #059669 !important;
    }

    .bg-danger-soft {
      background: #fee2e2;
      color: #991b1b;
    }

    .bg-warning-soft {
      background: #fef3c7;
      color: #92400e;
    }

    .bg-success-soft {
      background: #d1fae5;
      color: #065f46;
    }

    .bg-info-soft {
      background: #cffafe;
      color: #155e75;
    }

    .overdue-row {
      background: #fff1f2;
    }

    @media (max-width: 1200px) {
      .kpi-grid,
      .smart-dashboard,
      .filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }

      .admin-workflow {
        grid-template-columns: 1fr;
      }

      .workflow-side {
        position: static;
      }
    }

    @media (max-width: 820px) {
      .ts-page {
        padding: 14px;
      }

      .hero-card,
      .panel-header,
      .section-title,
      .filter-header,
      .list-header,
      .rec-top,
      .parent-card-head {
        flex-direction: column;
        align-items: stretch;
      }

      .kpi-grid,
      .smart-dashboard,
      .form-grid,
      .filter-grid,
      .reclamation-grid,
      .parent-list,
      .rec-insights,
      .escalation-grid,
      .mini-grid {
        grid-template-columns: 1fr;
      }

      .span-2 {
        grid-column: span 1;
      }

      .hero-actions {
        justify-content: stretch;
      }

      .hero-actions button {
        width: 100%;
      }
    }
  `]
})
export class ReclamationPageComponent implements OnInit {
  reclamations: Reclamation[] = [];
  adminDashboard: AdminDashboardResponse | null = null;

  reclamationCategories: string[] = [
    'REPAS',
    'TRANSPORT',
    'COMPORTEMENT',
    'HYGIENE',
    'SECURITE',
    'PERSONNEL',
    'FINANCIER',
    'PEDAGOGIQUE',
    'ADMINISTRATIF',
    'AUTRE'
  ];

  loading = false;
  error = '';
  createError = '';
  createSuccess = '';
  updateError = '';
  updateSuccess = '';
  contentModeratedInfo = '';
  updateModeratedInfo = '';
  deleteSuccess = '';
  statusSuccess = '';
  exportExcelLoading = false;

  searchTitle = '';
  filterCategory = '';
  filterStatus = '';
  filterPriority = '';
  filterSmartLevel = '';
  filterEscalation = '';

  viewMode: 'cards' | 'table' = 'cards';
  currentPage = 1;
  pageSize = 5;

  editingReclamationId: number | null = null;
  selectedReclamationImage: File | null = null;
  selectedReclamationImageName = '';
  selectedAttachment: File | null = null;
  selectedAttachmentName = '';

  recommendedAction: RecommendedAdminActionResponse | null = null;
  selectedEscalation: EscalationInfoResponse | null = null;

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

  loadReclamations(): void {
    this.loading = true;
    this.error = '';
    this.deleteSuccess = '';
    this.statusSuccess = '';

    if (this.isAdmin()) {
      this.messagerieService.getAdminDashboard().subscribe({
        next: (data) => {
          this.adminDashboard = data;
          this.reclamations = data.prioritizedReclamations || [];
          this.loading = false;
          this.normalizePage();
        },
        error: (err: any) => {
          console.error('Erreur chargement dashboard = ', err);
          this.error = 'Impossible de charger le dashboard intelligent.';
          this.loading = false;
        }
      });
      return;
    }

    this.messagerieService.getMyReclamations().subscribe({
      next: (data) => {
        this.reclamations = data || [];
        this.loading = false;
        this.normalizePage();
      },
      error: (err: any) => {
        console.error('Erreur chargement réclamations = ', err);
        this.error = 'Impossible de charger les réclamations.';
        this.loading = false;
      }
    });
  }

  createReclamation(): void {
    this.createError = '';
    this.createSuccess = '';
    this.contentModeratedInfo = '';

    if (!this.newReclamation.title.trim()) {
      this.createError = 'Le titre est obligatoire.';
      return;
    }

    if (!this.newReclamation.description.trim()) {
      this.createError = 'La description est obligatoire.';
      return;
    }

    const originalTitle = this.newReclamation.title.trim();
    const originalDescription = this.newReclamation.description.trim();

    this.messagerieService.createReclamation(
      originalTitle,
      originalDescription,
      this.newReclamation.priority,
      this.newReclamation.category,
      this.selectedReclamationImage,
      this.selectedAttachment
    ).subscribe({
      next: (savedReclamation) => {
        this.createSuccess = 'Réclamation créée avec succès. Elle sera analysée automatiquement.';

        if (this.wasContentModerated(originalTitle, originalDescription, savedReclamation)) {
          this.contentModeratedInfo = 'Certains mots inappropriés ont été filtrés automatiquement.';
        }

        this.newReclamation = { title: '', description: '', category: '', priority: '' };
        this.selectedReclamationImage = null;
        this.selectedReclamationImageName = '';
        this.selectedAttachment = null;
        this.selectedAttachmentName = '';
        this.loadReclamations();
      },
      error: (err: any) => {
        console.error('Erreur création réclamation = ', err);
        this.createError = 'Impossible de créer la réclamation.';
      }
    });
  }

  editReclamation(rec: Reclamation): void {
    this.editingReclamationId = rec.id;
    this.recommendedAction = null;
    this.selectedEscalation = null;

    this.editedReclamation = {
      title: rec.title || '',
      description: rec.description || '',
      category: rec.category || '',
      priority: rec.priority || '',
      adminComment: rec.adminComment || ''
    };

    this.updateError = '';
    this.updateSuccess = '';
    this.updateModeratedInfo = '';

    if (this.isAdmin()) {
      this.loadEscalationInfo(rec.id);
      this.loadRecommendedAction(rec.id);
    }

    setTimeout(() => {
      document.querySelector('.editor-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  }

  cancelEdit(): void {
    this.editingReclamationId = null;
    this.recommendedAction = null;
    this.selectedEscalation = null;
    this.editedReclamation = {
      title: '',
      description: '',
      category: '',
      priority: '',
      adminComment: ''
    };
    this.updateError = '';
    this.updateSuccess = '';
    this.updateModeratedInfo = '';
  }

  updateReclamation(): void {
    this.updateError = '';
    this.updateSuccess = '';
    this.updateModeratedInfo = '';

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
          console.error('Erreur update adminComment = ', err);
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

    const originalTitle = this.editedReclamation.title.trim();
    const originalDescription = this.editedReclamation.description.trim();

    this.messagerieService.updateReclamation(this.editingReclamationId, {
      title: originalTitle,
      description: originalDescription,
      category: this.editedReclamation.category,
      priority: this.editedReclamation.priority
    }).subscribe({
      next: (savedReclamation) => {
        this.updateSuccess = 'Réclamation modifiée avec succès.';

        if (this.wasContentModerated(originalTitle, originalDescription, savedReclamation)) {
          this.updateModeratedInfo = 'Certains mots inappropriés ont été filtrés automatiquement.';
        }

        this.cancelEdit();
        this.loadReclamations();
      },
      error: (err: any) => {
        console.error('Erreur modification réclamation = ', err);
        this.updateError = 'Impossible de modifier la réclamation.';
      }
    });
  }

  deleteReclamation(id: number): void {
    if (!confirm('Voulez-vous vraiment supprimer cette réclamation ?')) {
      return;
    }

    this.error = '';
    this.deleteSuccess = '';

    this.messagerieService.deleteReclamation(id).subscribe({
      next: () => {
        this.deleteSuccess = 'Réclamation supprimée avec succès.';
        if (this.editingReclamationId === id) {
          this.cancelEdit();
        }
        this.loadReclamations();
      },
      error: (err: any) => {
        console.error('Erreur suppression réclamation = ', err);
        this.error = 'Impossible de supprimer la réclamation.';
      }
    });
  }

  changeStatus(id: number, status: string): void {
    if (!this.isAdmin()) {
      return;
    }

    this.statusSuccess = '';
    this.error = '';

    this.messagerieService.updateReclamationStatus(id, status).subscribe({
      next: () => {
        this.statusSuccess = 'Statut mis à jour avec succès.';
        this.loadReclamations();
      },
      error: (err: any) => {
        console.error('Erreur changement statut = ', err);
        this.error = 'Impossible de modifier le statut.';
      }
    });
  }

  generateSuggestedResponse(): void {
    if (this.editingReclamationId === null) {
      return;
    }

    this.updateError = '';
    this.updateSuccess = '';

    this.messagerieService.getSuggestedResponse(this.editingReclamationId).subscribe({
      next: (res) => {
        this.editedReclamation.adminComment = res.suggestedResponse || '';
        this.updateSuccess = 'Réponse suggérée générée automatiquement.';
      },
      error: (err: any) => {
        console.error('Erreur génération réponse = ', err);
        this.updateError = 'Impossible de générer une réponse suggérée.';
      }
    });

    this.loadRecommendedAction(this.editingReclamationId);
    this.loadEscalationInfo(this.editingReclamationId);
  }

  loadRecommendedAction(id: number): void {
    this.messagerieService.getRecommendedAdminAction(id).subscribe({
      next: (res) => {
        this.recommendedAction = res;
      },
      error: (err: any) => {
        console.error('Erreur recommandation ML = ', err);
      }
    });
  }

  loadEscalationInfo(id: number): void {
    this.messagerieService.getEscalationInfo(id).subscribe({
      next: (res) => {
        this.selectedEscalation = res;
      },
      error: (err: any) => {
        console.error('Erreur escalade intelligente = ', err);
      }
    });
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
        console.error('Erreur export Excel = ', err);
        this.error = 'Impossible d’exporter le fichier Excel.';
        this.exportExcelLoading = false;
      }
    });
  }

  filteredReclamations(): Reclamation[] {
    const q = this.normalize(this.searchTitle);

    return this.reclamations.filter((rec) => {
      const text = this.normalize(`${rec.title || ''} ${rec.description || ''}`);
      const matchSearch = !q || text.includes(q);
      const matchCategory = !this.filterCategory || rec.category === this.filterCategory;
      const matchStatus = !this.filterStatus || rec.status === this.filterStatus;
      const matchPriority = !this.filterPriority || rec.priority === this.filterPriority;
      const matchSmart = !this.filterSmartLevel || rec.smartPriorityLevel === this.filterSmartLevel;

      let matchEscalation = true;
      if (this.filterEscalation === 'ESCALATED') {
        matchEscalation = rec.autoEscalated === true;
      } else if (this.filterEscalation === 'RECURRENT') {
        matchEscalation = rec.recurring === true;
      } else if (this.filterEscalation === 'UNANSWERED') {
        matchEscalation = !rec.adminComment || !rec.adminComment.trim();
      }

      return matchSearch && matchCategory && matchStatus && matchPriority && matchSmart && matchEscalation;
    });
  }

  paginatedReclamations(): Reclamation[] {
    const start = (this.currentPage - 1) * Number(this.pageSize);
    return this.filteredReclamations().slice(start, start + Number(this.pageSize));
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredReclamations().length / Number(this.pageSize)));
  }

  getVisiblePages(): number[] {
    const total = this.totalPages();
    const pages: number[] = [];
    const start = Math.max(1, this.currentPage - 2);
    const end = Math.min(total, this.currentPage + 2);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  setPage(page: number): void {
    this.currentPage = Math.min(Math.max(1, page), this.totalPages());
  }

  previousPage(): void {
    this.setPage(this.currentPage - 1);
  }

  nextPage(): void {
    this.setPage(this.currentPage + 1);
  }

  goFirstPage(): void {
    this.currentPage = 1;
  }

  normalizePage(): void {
    if (this.currentPage > this.totalPages()) {
      this.currentPage = this.totalPages();
    }
  }

  getSmartLevelPercent(level: string): number {
    const total = this.adminDashboard?.totalActive || 0;
    if (total === 0) {
      return 0;
    }

    const count = level === 'CRITICAL'
      ? this.adminDashboard?.criticalCount || 0
      : level === 'HIGH'
        ? this.adminDashboard?.highCount || 0
        : level === 'MEDIUM'
          ? this.adminDashboard?.mediumCount || 0
          : this.adminDashboard?.lowCount || 0;

    return Math.round((count / total) * 100);
  }

  getDecisionStats(): { label: string; count: number }[] {
    const map = new Map<string, number>();

    this.reclamations.forEach((rec) => {
      if (rec.decisionRecommendation) {
        map.set(rec.decisionRecommendation, (map.get(rec.decisionRecommendation) || 0) + 1);
      }
    });

    return Array.from(map.entries()).map(([label, count]) => ({ label, count }));
  }

  getStatusCount(status: string): number {
    if (this.adminDashboard) {
      if (status === 'OPEN') return this.adminDashboard.openCount || 0;
      if (status === 'IN_PROGRESS') return this.adminDashboard.inProgressCount || 0;
      if (status === 'RESOLVED') return this.adminDashboard.resolvedCount || 0;
      if (status === 'REJECTED') return this.adminDashboard.rejectedCount || 0;
    }

    return this.reclamations.filter((rec) => rec.status === status).length;
  }

  getCurrentEditingReclamation(): Reclamation | undefined {
    return this.reclamations.find((rec) => rec.id === this.editingReclamationId);
  }

  getCategoryLabel(category?: string | null): string {
    switch (category) {
      case 'REPAS': return 'Repas';
      case 'TRANSPORT': return 'Transport';
      case 'COMPORTEMENT': return 'Comportement';
      case 'HYGIENE': return 'Hygiène';
      case 'SECURITE': return 'Sécurité';
      case 'PERSONNEL': return 'Personnel';
      case 'FINANCIER': return 'Financier';
      case 'PEDAGOGIQUE': return 'Pédagogique';
      case 'ADMINISTRATIF': return 'Administratif';
      case 'AUTRE': return 'Autre';
      default: return category || '-';
    }
  }

  getDecisionLabel(decision?: string | null): string {
    switch (decision) {
      case 'REPAIR_NEEDED': return 'Réparation nécessaire';
      case 'INCREASE_SUPERVISION': return 'Surveillance renforcée';
      case 'STAFF_TRAINING': return 'Formation du personnel';
      case 'PROCESS_IMPROVEMENT': return 'Amélioration processus';
      case 'ADMINISTRATIVE_CORRECTION': return 'Correction administrative';
      case 'MEDICAL_ATTENTION': return 'Attention médicale';
      case 'TRANSPORT_ESCALATION': return 'Escalade transport';
      case 'PARENT_FOLLOWUP': return 'Suivi parent';
      default: return decision || '-';
    }
  }

  getDecisionBadgeClass(decision?: string | null): string {
    switch (decision) {
      case 'REPAIR_NEEDED': return 'decision-repair';
      case 'INCREASE_SUPERVISION': return 'decision-supervision';
      case 'STAFF_TRAINING': return 'decision-training';
      case 'PROCESS_IMPROVEMENT': return 'decision-process';
      case 'ADMINISTRATIVE_CORRECTION': return 'decision-admin';
      case 'MEDICAL_ATTENTION': return 'decision-medical';
      case 'TRANSPORT_ESCALATION': return 'decision-transport';
      case 'PARENT_FOLLOWUP': return 'decision-followup';
      default: return 'chip';
    }
  }

  getRecommendedServiceLabel(service?: string | null): string {
    switch (service) {
      case 'SERVICE_MEDICAL': return 'Service médical';
      case 'MAINTENANCE': return 'Maintenance';
      case 'SERVICE_PEDAGOGIQUE': return 'Service pédagogique';
      case 'RESSOURCES_HUMAINES': return 'Ressources humaines';
      case 'ADMINISTRATION': return 'Administration';
      case 'SERVICE_ADMINISTRATIF': return 'Service administratif';
      case 'SERVICE_TRANSPORT': return 'Service transport';
      case 'SERVICE_HYGIENE': return 'Service hygiène';
      case 'DIRECTION': return 'Direction';
      case 'RELATION_PARENT': return 'Relation parent';
      case 'ANALYSE_ADMINISTRATIVE': return 'Analyse administrative';
      default: return service || '-';
    }
  }

  getRecommendedDelayLabel(delay?: string | null): string {
    switch (delay) {
      case 'IMMEDIATE': return 'Immédiat';
      case '24H': return 'Sous 24h';
      case '48H': return 'Sous 48h';
      case '72H': return 'Sous 72h';
      case 'NONE': return 'Aucun délai critique';
      default: return delay || '-';
    }
  }

  getConfidencePercent(value?: number | null): string {
    if (value === null || value === undefined) {
      return '-';
    }

    return (value * 100).toFixed(1) + '%';
  }

  getSmartLevelSoftClass(level?: string | null): string {
    switch (level) {
      case 'CRITICAL': return 'soft-critical';
      case 'HIGH': return 'soft-high';
      case 'MEDIUM': return 'soft-medium';
      case 'LOW': return 'soft-low';
      default: return 'soft-low';
    }
  }

  getPrioritySoftClass(priority?: string | null): string {
    switch (priority) {
      case 'HIGH': return 'soft-danger';
      case 'MEDIUM': return 'soft-warning';
      case 'LOW': return 'soft-success';
      default: return '';
    }
  }

  getStatusSoftClass(status?: string | null): string {
    switch (status) {
      case 'OPEN': return 'soft-info';
      case 'IN_PROGRESS': return 'soft-warning';
      case 'RESOLVED': return 'soft-success';
      case 'REJECTED': return 'soft-danger';
      default: return '';
    }
  }

  getUrgencyTextClass(urgency?: string | null): string {
    switch (urgency) {
      case 'CRITICAL':
      case 'HIGH':
        return 'text-urgent';
      case 'MEDIUM':
        return 'text-high';
      default:
        return 'text-normal';
    }
  }

  getSlaBadgeClass(rec: Reclamation): string {
    if (rec.status === 'RESOLVED' || rec.status === 'REJECTED') {
      return 'bg-success-soft';
    }

    if (this.isOverdue(rec)) {
      return 'bg-danger-soft';
    }

    if ((rec.smartPriorityScore || 0) >= 50) {
      return 'bg-warning-soft';
    }

    return 'bg-info-soft';
  }

  getSlaLabel(rec: Reclamation): string {
    if (rec.status === 'RESOLVED' || rec.status === 'REJECTED') {
      return 'Clôturée';
    }

    if (this.isOverdue(rec)) {
      return 'SLA dépassé';
    }

    if ((rec.smartPriorityScore || 0) >= 75) {
      return 'SLA critique';
    }

    if ((rec.smartPriorityScore || 0) >= 50) {
      return 'À surveiller';
    }

    return 'SLA OK';
  }

  isOverdue(rec: Reclamation): boolean {
    if (rec.status === 'RESOLVED' || rec.status === 'REJECTED') {
      return false;
    }

    if (rec.smartPriorityLevel === 'CRITICAL') {
      return true;
    }

    return (rec.smartPriorityReason || '').toUpperCase().includes('SLA DÉPASSÉ')
      || (rec.smartPriorityReason || '').toUpperCase().includes('SLA DEPASSE');
  }

  getAdminCommentPlaceholder(): string {
    const current = this.getCurrentEditingReclamation();

    switch (current?.status) {
      case 'OPEN':
        return 'Ex : Réclamation bien reçue. Une analyse est en cours par le service concerné.';
      case 'IN_PROGRESS':
        return 'Ex : Le dossier est en cours de traitement. Nous reviendrons vers vous rapidement.';
      case 'RESOLVED':
        return 'Ex : Le problème signalé a été traité et les mesures nécessaires ont été prises.';
      case 'REJECTED':
        return 'Ex : Après vérification, la demande ne peut pas être retenue en l’état.';
      default:
        return 'Ajoutez une réponse claire, professionnelle et rassurante.';
    }
  }

  onReclamationImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      this.selectedReclamationImage = input.files[0];
      this.selectedReclamationImageName = input.files[0].name;
      return;
    }

    this.selectedReclamationImage = null;
    this.selectedReclamationImageName = '';
  }

  onAttachmentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      this.selectedAttachment = input.files[0];
      this.selectedAttachmentName = input.files[0].name;
      return;
    }

    this.selectedAttachment = null;
    this.selectedAttachmentName = '';
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
      error: (err: any) => {
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

  wasContentModerated(originalTitle: string, originalDescription: string, savedReclamation: Reclamation): boolean {
    return originalTitle !== savedReclamation.title || originalDescription !== savedReclamation.description;
  }

  normalize(value?: string | null): string {
    return (value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }
}
