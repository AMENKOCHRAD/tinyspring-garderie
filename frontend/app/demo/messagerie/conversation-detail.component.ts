import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MessagerieService, Message } from 'src/app/services/messagerie.service';

@Component({
  selector: 'app-conversation-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-body">
        <h3>Détail de la conversation</h3>

        <div *ngIf="loading" class="mt-3">
          Chargement des messages...
        </div>

        <div *ngIf="error" class="alert alert-danger mt-3">
          {{ error }}
        </div>

        <div *ngIf="successMessage" class="alert alert-success mt-3">
          {{ successMessage }}
        </div>

        <div *ngIf="!loading && messages.length === 0" class="alert alert-info mt-3">
          Aucun message dans cette conversation.
        </div>

        <div class="mt-3" *ngIf="messages.length > 0">
          <div class="card mb-2" *ngFor="let msg of messages">
            <div class="card-body">
              <p class="mb-1">
                <strong>{{ msg.sender?.nom }}</strong>
              </p>

              <p class="mb-1">
                {{ msg.content }}
              </p>

              <small class="text-muted">
                {{ msg.sentAt | date:'short' }}
              </small>
            </div>
          </div>
        </div>

        <hr class="my-4" />

        <h5>Envoyer un message</h5>

        <div class="mb-3">
          <textarea
            class="form-control"
            rows="4"
            [(ngModel)]="newMessage"
            placeholder="Tapez votre message..."
          ></textarea>
        </div>

        <button class="btn btn-primary" (click)="sendMessage()">
          Envoyer
        </button>
      </div>
    </div>
  `
})
export class ConversationDetailComponent implements OnInit {
  conversationId!: number;
  messages: Message[] = [];
  newMessage = '';

  loading = false;
  error = '';
  successMessage = '';

  constructor(
    private route: ActivatedRoute,
    private messagerieService: MessagerieService
  ) {}

  ngOnInit(): void {
    this.conversationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadMessages();
  }

  loadMessages(): void {
    this.loading = true;
    this.error = '';

    this.messagerieService.getMessagesByConversation(this.conversationId).subscribe({
      next: (data) => {
        this.messages = data;
        this.loading = false;
      },
      error: (err) => {
        console.log('Erreur chargement messages = ', err);
        this.error = 'Impossible de charger les messages.';
        this.loading = false;
      }
    });
  }

  sendMessage(): void {
    this.error = '';
    this.successMessage = '';

    if (!this.newMessage.trim()) {
      this.error = 'Le message est obligatoire.';
      return;
    }

    this.messagerieService.sendMessage(this.conversationId, {
      content: this.newMessage.trim()
    }).subscribe({
      next: () => {
        this.successMessage = 'Message envoyé avec succès.';
        this.newMessage = '';
        this.loadMessages();
      },
      error: (err) => {
        console.log('Erreur envoi message = ', err);
        this.error = 'Impossible d’envoyer le message.';
      }
    });
  }
}