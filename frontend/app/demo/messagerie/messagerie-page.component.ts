import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { MessagerieService } from 'src/app/services/messagerie.service';
import { Conversation } from 'src/app/models/conversation.model';
import { User } from 'src/app/models/user.model';

@Component({
  selector: 'app-messagerie-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="messagerie-page">
      <div class="messagerie-wrapper">

        <div class="page-header">
          <h2>Gestion Messagerie interne</h2>
          <p>Créer, consulter, modifier et supprimer des conversations</p>
        </div>

        <!-- CREATE CONVERSATION -->
        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body">
            <h4 class="mb-3">Créer une conversation</h4>

            <div *ngIf="createError" class="alert alert-danger mt-2">
              {{ createError }}
            </div>

            <div *ngIf="createSuccess" class="alert alert-success mt-2">
              {{ createSuccess }}
            </div>

            <div class="mb-3">
              <label class="form-label">Sujet</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="newConversation.subject"
                name="subject"
                maxlength="100"
                placeholder="Ex: Question sur mon enfant"
                (blur)="subjectTouched = true"
              />

              <small class="text-muted">
                {{ subjectValue.length }} / {{ subjectMaxLength }} caractères
              </small>

              <div *ngIf="subjectError" class="text-danger mt-1">
                {{ subjectError }}
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label">Type de destinataire</label>
              <select
                class="form-control"
                [(ngModel)]="selectedRole"
                name="selectedRole"
                (change)="onRoleChange()"
              >
                <option value="">-- Choisir --</option>
                <option value="ADMIN">ADMIN</option>
                <option value="ANIMATRICE">ANIMATRICE</option>
              </select>
            </div>

            <div class="mb-3" *ngIf="availableUsers.length > 0">
              <label class="form-label">Destinataire</label>
              <select
                class="form-control"
                [(ngModel)]="newConversation.receiverId"
                name="receiverId"
              >
                <option [ngValue]="null">-- Choisir un destinataire --</option>
                <option *ngFor="let user of availableUsers" [ngValue]="user.id">
                  {{ user.nom }} ({{ user.email }})
                </option>
              </select>
            </div>

            <button
              class="btn btn-primary"
              (click)="createConversation()"
              [disabled]="!isSubjectValid() || !selectedRole || !newConversation.receiverId"
            >
              Créer conversation
            </button>
          </div>
        </div>

        <!-- UPDATE CONVERSATION -->
        <div class="card shadow-sm border-0 mb-4" *ngIf="editingConversationId !== null">
          <div class="card-body">
            <h4 class="mb-3">Modifier la conversation</h4>

            <div *ngIf="updateError" class="alert alert-danger">
              {{ updateError }}
            </div>

            <div *ngIf="updateSuccess" class="alert alert-success">
              {{ updateSuccess }}
            </div>

            <div class="mb-3">
              <label class="form-label">Nouveau sujet</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="editedSubject"
                name="editedSubject"
                placeholder="Modifier le sujet"
              />
            </div>

            <button class="btn btn-success me-2" (click)="updateConversation()">
              Enregistrer
            </button>

            <button class="btn btn-secondary" (click)="cancelEditConversation()">
              Annuler
            </button>
          </div>
        </div>

        <!-- LIST CONVERSATIONS -->
        <div class="card shadow-sm border-0">
          <div class="card-body">
            <h4 class="mb-3">Liste de mes conversations</h4>

            <div *ngIf="loading" class="mt-3">
              Chargement des conversations...
            </div>

            <div *ngIf="error" class="alert alert-danger mt-3">
              {{ error }}
            </div>

            <div *ngIf="deleteSuccess" class="alert alert-success mt-3">
              {{ deleteSuccess }}
            </div>

            <div *ngIf="!loading && conversations.length === 0" class="alert alert-info mt-3">
              Aucune conversation trouvée.
            </div>

            <div class="conversation-list" *ngIf="conversations.length > 0">
              <div *ngFor="let conv of conversations" class="conversation-card">
                <div class="conversation-clickable" (click)="goToConversation(conv.id)">
                  <h5 class="mb-2">{{ conv.subject }}</h5>
                  <p class="mb-1"><strong>Type :</strong> {{ conv.type }}</p>
                  <p class="mb-1"><strong>Statut :</strong> {{ conv.status }}</p>
                  <p class="mb-1"><strong>Parent :</strong> {{ conv.parent?.nom }}</p>
                  <p class="mb-1" *ngIf="conv.admin"><strong>Admin :</strong> {{ conv.admin.nom }}</p>
                  <p class="mb-1" *ngIf="conv.animatrice"><strong>Animatrice :</strong> {{ conv.animatrice.nom }}</p>
                  <p class="mb-0 text-muted">
                    Créée le : {{ conv.createdAt | date:'short' }}
                  </p>
                </div>

                <div class="conversation-actions">
                  <button
                    class="btn btn-sm btn-warning me-2"
                    (click)="editConversation(conv); $event.stopPropagation()"
                  >
                    Modifier
                  </button>

                  <button
                    class="btn btn-sm btn-danger"
                    (click)="deleteConversation(conv.id); $event.stopPropagation()"
                  >
                    Supprimer
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .messagerie-page {
      width: 100%;
      display: flex;
      justify-content: center;
      padding: 24px;
      box-sizing: border-box;
    }

    .messagerie-wrapper {
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

    .conversation-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-top: 16px;
    }

    .conversation-card {
      background: #ffffff;
      border: 1px solid #e9ecef;
      border-radius: 14px;
      padding: 16px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
    }

    .conversation-clickable {
      flex: 1;
      cursor: pointer;
    }

    .conversation-clickable h5 {
      font-weight: 700;
      color: #1f2937;
    }

    .conversation-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: flex-end;
      min-width: 180px;
    }

    @media (max-width: 768px) {
      .messagerie-page {
        padding: 12px;
      }

      .conversation-card {
        flex-direction: column;
        align-items: flex-start;
      }

      .conversation-actions {
        width: 100%;
        justify-content: flex-start;
        min-width: auto;
      }
    }
  `]
})
export class MessageriePageComponent implements OnInit {
  conversations: Conversation[] = [];
  availableUsers: User[] = [];

  loading = false;
  error = '';

  createError = '';
  createSuccess = '';

  updateError = '';
  updateSuccess = '';

  deleteSuccess = '';

  selectedRole = '';

  subjectTouched = false;
  readonly subjectMinLength = 5;
  readonly subjectMaxLength = 100;

  editingConversationId: number | null = null;
  editedSubject = '';

  newConversation = {
    subject: '',
    receiverId: null as number | null
  };

  constructor(
    private messagerieService: MessagerieService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadConversations();
  }

  get subjectValue(): string {
    return this.newConversation.subject || '';
  }

  get subjectError(): string {
    const subject = this.subjectValue.trim();

    if (!this.subjectTouched) {
      return '';
    }

    if (!subject) {
      return 'Le sujet est obligatoire.';
    }

    if (subject.length < this.subjectMinLength) {
      return `Le sujet doit contenir au moins ${this.subjectMinLength} caractères.`;
    }

    if (subject.length > this.subjectMaxLength) {
      return `Le sujet ne doit pas dépasser ${this.subjectMaxLength} caractères.`;
    }

    if (/^[0-9]+$/.test(subject)) {
      return 'Le sujet ne peut pas contenir uniquement des chiffres.';
    }

    return '';
  }

  isSubjectValid(): boolean {
    return this.subjectError === '';
  }

  loadConversations(): void {
    this.loading = true;
    this.error = '';
    this.deleteSuccess = '';

    this.messagerieService.getMyConversations().subscribe({
      next: (data) => {
        this.conversations = data;
        this.loading = false;
      },
      error: (err) => {
        console.log('Erreur conversations = ', err);

        if (err.status === 401) {
          this.error = 'Non autorisé : veuillez vous reconnecter.';
        } else if (err.status === 403) {
          this.error = 'Accès refusé.';
        } else if (err.status === 0) {
          this.error = 'Backend inaccessible ou problème CORS.';
        } else {
          this.error = 'Impossible de charger les conversations. Erreur: ' + err.status;
        }

        this.loading = false;
      }
    });
  }

  onRoleChange(): void {
    this.newConversation.receiverId = null;
    this.availableUsers = [];
    this.createError = '';
    this.createSuccess = '';

    if (this.selectedRole === 'ADMIN' && !this.newConversation.subject.trim()) {
      this.newConversation.subject = "Question à l'administration";
    }

    if (this.selectedRole === 'ANIMATRICE' && !this.newConversation.subject.trim()) {
      this.newConversation.subject = "Question à l'animatrice";
    }

    if (!this.selectedRole) {
      return;
    }

    this.messagerieService.getUsersByRole(this.selectedRole).subscribe({
      next: (users) => {
        this.availableUsers = users;
      },
      error: (err) => {
        console.log('Erreur chargement users par rôle = ', err);
        this.createError = 'Impossible de charger les destinataires.';
      }
    });
  }

  createConversation(): void {
    this.createError = '';
    this.createSuccess = '';
    this.subjectTouched = true;

    if (!this.isSubjectValid()) {
      this.createError = this.subjectError;
      return;
    }

    if (!this.selectedRole) {
      this.createError = 'Le type de destinataire est obligatoire.';
      return;
    }

    if (!this.newConversation.receiverId) {
      this.createError = 'Veuillez choisir un destinataire.';
      return;
    }

    this.messagerieService.createConversation({
      subject: this.newConversation.subject.trim(),
      receiverId: this.newConversation.receiverId,
      receiverRole: this.selectedRole
    }).subscribe({
      next: () => {
        this.createSuccess = 'Conversation créée avec succès.';
        this.newConversation = {
          subject: '',
          receiverId: null
        };
        this.selectedRole = '';
        this.availableUsers = [];
        this.subjectTouched = false;
        this.loadConversations();
      },
      error: (err) => {
        console.log('Erreur création conversation = ', err);

        if (err.status === 401) {
          this.createError = 'Non autorisé : veuillez vous reconnecter.';
        } else if (err.status === 403) {
          this.createError = 'Accès refusé.';
        } else if (err.error) {
          this.createError = typeof err.error === 'string'
            ? err.error
            : 'Erreur lors de la création.';
        } else {
          this.createError = 'Erreur lors de la création de la conversation.';
        }
      }
    });
  }

  editConversation(conv: Conversation): void {
    this.editingConversationId = conv.id;
    this.editedSubject = conv.subject;
    this.updateError = '';
    this.updateSuccess = '';
  }

  cancelEditConversation(): void {
    this.editingConversationId = null;
    this.editedSubject = '';
    this.updateError = '';
    this.updateSuccess = '';
  }

  updateConversation(): void {
    this.updateError = '';
    this.updateSuccess = '';

    if (!this.editedSubject.trim()) {
      this.updateError = 'Le sujet est obligatoire.';
      return;
    }

    if (this.editingConversationId === null) {
      this.updateError = 'Aucune conversation sélectionnée.';
      return;
    }

    this.messagerieService.updateConversation(this.editingConversationId, {
      subject: this.editedSubject.trim()
    }).subscribe({
      next: () => {
        this.updateSuccess = 'Conversation modifiée avec succès.';
        this.editingConversationId = null;
        this.editedSubject = '';
        this.loadConversations();
      },
      error: (err) => {
        console.log('Erreur update conversation = ', err);
        this.updateError = 'Impossible de modifier la conversation.';
      }
    });
  }

  deleteConversation(id: number): void {
    this.error = '';
    this.deleteSuccess = '';

    const confirmDelete = confirm('Supprimer cette conversation ?');
    if (!confirmDelete) {
      return;
    }

    this.messagerieService.deleteConversation(id).subscribe({
      next: () => {
        this.deleteSuccess = 'Conversation supprimée avec succès.';
        this.loadConversations();
      },
      error: (err) => {
        console.log('Erreur suppression conversation = ', err);
        this.error = 'Impossible de supprimer la conversation.';
      }
    });
  }

  goToConversation(id: number): void {
    this.router.navigate(['/messagerie', id]);
  }
}