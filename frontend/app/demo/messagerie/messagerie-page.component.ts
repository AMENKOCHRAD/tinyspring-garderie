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
    <div class="card">
      <div class="card-body">
        <h2>Gestion Messagerie interne</h2>

        <div class="card mb-4">
          <div class="card-body">
            <h4>Créer une conversation</h4>

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

        <p>Liste de mes conversations</p>

        <div *ngIf="loading" class="mt-3">
          Chargement des conversations...
        </div>

        <div *ngIf="error" class="alert alert-danger mt-3">
          {{ error }}
        </div>

        <div *ngIf="!loading && conversations.length === 0" class="alert alert-info mt-3">
          Aucune conversation trouvée.
        </div>

        <div class="mt-3" *ngIf="conversations.length > 0">
          <div
            class="card mb-3"
            *ngFor="let conversation of conversations"
            style="cursor: pointer;"
            (click)="goToConversation(conversation.id)"
          >
            <div class="card-body">
              <h5 class="mb-2">{{ conversation.subject }}</h5>

              <p class="mb-1">
                <strong>Type :</strong> {{ conversation.type }}
              </p>

              <p class="mb-1">
                <strong>Statut :</strong> {{ conversation.status }}
              </p>

              <p class="mb-1">
                <strong>Parent :</strong> {{ conversation.parent?.nom }}
              </p>

              <p class="mb-1" *ngIf="conversation.admin">
                <strong>Admin :</strong> {{ conversation.admin.nom }}
              </p>

              <p class="mb-1" *ngIf="conversation.animatrice">
                <strong>Animatrice :</strong> {{ conversation.animatrice.nom }}
              </p>

              <p class="mb-0 text-muted">
                Créée le : {{ conversation.createdAt | date:'short' }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class MessageriePageComponent implements OnInit {
  conversations: Conversation[] = [];
  availableUsers: User[] = [];

  loading = false;
  error = '';
  createError = '';
  createSuccess = '';

  selectedRole = '';

  subjectTouched = false;
  readonly subjectMinLength = 5;
  readonly subjectMaxLength = 100;

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

  get subjectLength(): number {
    return this.subjectValue.trim().length;
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

  goToConversation(id: number): void {
    this.router.navigate(['/messagerie', id]);
  }
}