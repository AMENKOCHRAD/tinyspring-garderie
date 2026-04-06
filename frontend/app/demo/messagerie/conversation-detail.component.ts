import { Component, OnInit, AfterViewChecked, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AuthService } from 'src/app/services/auth.service';
import {
  MessagerieService,
  Message
} from 'src/app/services/messagerie.service';
import { Conversation } from 'src/app/models/conversation.model';

@Component({
  selector: 'app-conversation-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="chat-page">
      <div class="chat-wrapper">
        <div class="chat-header">
          <div>
            <h2>{{ conversation?.subject || 'Conversation' }}</h2>
            <p>Messagerie interne</p>
          </div>

          <div class="conversation-status-box" *ngIf="conversation">
            <span
              class="status-badge"
              [class.status-open]="conversation.status === 'OPEN'"
              [class.status-closed]="conversation.status === 'CLOSED'"
              [class.status-archived]="conversation.status === 'ARCHIVED'"
            >
              {{ conversation.status }}
            </span>

            <button
              class="btn btn-sm btn-outline-danger mt-2"
              *ngIf="conversation.status === 'OPEN'"
              (click)="closeConversation()"
            >
              Fermer la conversation
            </button>
          </div>
        </div>

        <div *ngIf="error" class="alert alert-danger mx-3 mt-3">
          {{ error }}
        </div>

        <div *ngIf="successMessage" class="alert alert-success mx-3 mt-3">
          {{ successMessage }}
        </div>

        <div *ngIf="conversation?.status === 'CLOSED'" class="alert alert-warning mx-3 mt-3">
          Cette conversation est fermée. Vous ne pouvez plus envoyer de nouveaux messages.
        </div>

        <div class="chat-body" #scrollContainer>
          <div *ngIf="loading" class="system-message">
            Chargement des messages...
          </div>

          <div *ngIf="!loading && messages.length === 0" class="system-message">
            Aucun message dans cette conversation.
          </div>

          <div
            *ngFor="let msg of messages"
            class="message-line"
            [class.mine-line]="isCurrentUserSender(msg)"
            [class.other-line]="!isCurrentUserSender(msg)"
          >
            <div class="message-group">
              <div
                class="message-meta"
                [class.text-end]="isCurrentUserSender(msg)"
                [class.text-start]="!isCurrentUserSender(msg)"
              >
                {{ msg.sender?.nom }}
              </div>

              <div
                class="message-bubble"
                [class.mine]="isCurrentUserSender(msg)"
                [class.other]="!isCurrentUserSender(msg)"
              >
                <div *ngIf="editingMessageId !== msg.id">
                  <div class="message-text" *ngIf="msg.content">
                    {{ msg.content }}
                  </div>

                  <div *ngIf="msg.imagePath" class="message-image-wrapper">
                    <img
                      [src]="getImageUrl(msg.imagePath)"
                      [alt]="msg.imageName || 'image message'"
                      class="message-image"
                    />
                  </div>
                </div>

                <div *ngIf="editingMessageId === msg.id" class="edit-area">
                  <textarea
                    class="form-control"
                    rows="3"
                    [(ngModel)]="editedContent"
                  ></textarea>
                </div>

                <div class="message-time">
                  {{ msg.sentAt | date:'shortTime' }}
                  <span
                    class="read-badge ms-2"
                    [class.read-true]="msg.isRead"
                    [class.read-false]="!msg.isRead"
                  >
                    {{ msg.isRead ? 'Lu' : 'Non lu' }}
                  </span>
                </div>
              </div>

              <div class="message-actions" *ngIf="isCurrentUserSender(msg)">
                <ng-container *ngIf="editingMessageId !== msg.id">
                  <button
                    class="btn btn-sm btn-light border me-2"
                    (click)="startEdit(msg); $event.stopPropagation()"
                  >
                    Modifier
                  </button>

                  <button
                    class="btn btn-sm btn-danger"
                    (click)="deleteMessage(msg.id); $event.stopPropagation()"
                  >
                    Supprimer
                  </button>
                </ng-container>

                <ng-container *ngIf="editingMessageId === msg.id">
                  <button
                    class="btn btn-sm btn-success me-2"
                    (click)="updateMessage(msg.id); $event.stopPropagation()"
                  >
                    Enregistrer
                  </button>

                  <button
                    class="btn btn-sm btn-secondary"
                    (click)="cancelEdit(); $event.stopPropagation()"
                  >
                    Annuler
                  </button>
                </ng-container>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <div class="selected-file" *ngIf="selectedImageName">
            Image sélectionnée : <strong>{{ selectedImageName }}</strong>
          </div>

          <div class="composer">
            <textarea
              class="composer-input"
              rows="2"
              [(ngModel)]="newMessage"
              placeholder="Écrire un message..."
              [disabled]="conversation?.status === 'CLOSED'"
            ></textarea>

            <div class="composer-actions">
              <label class="file-btn" [class.disabled-btn]="conversation?.status === 'CLOSED'">
                📷
                <input
                  type="file"
                  accept="image/*"
                  (change)="onFileSelected($event)"
                  hidden
                  [disabled]="conversation?.status === 'CLOSED'"
                />
              </label>

              <button
                class="send-btn"
                (click)="sendMessage()"
                [disabled]="conversation?.status === 'CLOSED'"
              >
                Envoyer
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chat-page {
      width: 100%;
      display: flex;
      justify-content: center;
      padding: 20px;
      box-sizing: border-box;
    }

    .chat-wrapper {
      width: 100%;
      max-width: 950px;
      height: calc(100vh - 120px);
      background: #fff;
      border-radius: 20px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(0,0,0,0.08);
      display: flex;
      flex-direction: column;
      border: 1px solid #e9ecef;
    }

    .chat-header {
      padding: 18px 22px;
      border-bottom: 1px solid #e9ecef;
      background: #ffffff;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 20px;
    }

    .chat-header h2 {
      margin: 0;
      font-size: 28px;
      font-weight: 700;
      color: #212529;
    }

    .chat-header p {
      margin: 4px 0 0 0;
      color: #6c757d;
      font-size: 15px;
    }

    .conversation-status-box {
      text-align: right;
    }

    .status-badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 14px;
      font-size: 12px;
      font-weight: 700;
    }

    .status-open {
      background: #d1fae5;
      color: #065f46;
    }

    .status-closed {
      background: #fee2e2;
      color: #991b1b;
    }

    .status-archived {
      background: #e5e7eb;
      color: #374151;
    }

    .chat-body {
      flex: 1;
      overflow-y: auto;
      padding: 22px;
      background:
        linear-gradient(rgba(248,249,251,0.96), rgba(248,249,251,0.96)),
        repeating-linear-gradient(
          45deg,
          #f3f5f8,
          #f3f5f8 10px,
          #eef1f5 10px,
          #eef1f5 20px
        );
    }

    .system-message {
      text-align: center;
      color: #6c757d;
      padding: 16px;
      font-size: 15px;
    }

    .message-line {
      display: flex;
      width: 100%;
      margin-bottom: 14px;
    }

    .mine-line {
      justify-content: flex-end;
    }

    .other-line {
      justify-content: flex-start;
    }

    .message-group {
      max-width: 72%;
      display: flex;
      flex-direction: column;
    }

    .message-meta {
      font-size: 12px;
      color: #6c757d;
      margin-bottom: 4px;
      padding: 0 6px;
      font-weight: 600;
    }

    .message-bubble {
      padding: 10px 12px 8px 12px;
      border-radius: 18px;
      position: relative;
      box-shadow: 0 2px 6px rgba(0,0,0,0.08);
      overflow: hidden;
    }

    .message-bubble.mine {
      background: #d9fdd3;
      color: #1f1f1f;
      border-bottom-right-radius: 6px;
    }

    .message-bubble.other {
      background: #ffffff;
      color: #1f1f1f;
      border: 1px solid #e5e7eb;
      border-bottom-left-radius: 6px;
    }

    .message-text {
      font-size: 15px;
      line-height: 1.45;
      white-space: pre-wrap;
      word-break: break-word;
      margin-bottom: 6px;
    }

    .message-image-wrapper {
      margin-top: 6px;
    }

    .message-image {
      display: block;
      max-width: 320px;
      width: 100%;
      max-height: 260px;
      object-fit: cover;
      border-radius: 14px;
      border: 1px solid rgba(0,0,0,0.08);
      background: #fff;
    }

    .message-time {
      text-align: right;
      font-size: 11px;
      color: #667085;
      margin-top: 4px;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;
    }

    .read-badge {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 600;
    }

    .read-true {
      background: #d1fae5;
      color: #065f46;
    }

    .read-false {
      background: #fee2e2;
      color: #991b1b;
    }

    .message-actions {
      margin-top: 6px;
      display: flex;
      justify-content: flex-end;
      padding: 0 4px;
    }

    .edit-area {
      margin-bottom: 6px;
    }

    .chat-footer {
      border-top: 1px solid #e9ecef;
      background: #fff;
      padding: 14px 18px;
    }

    .selected-file {
      font-size: 13px;
      color: #6c757d;
      margin-bottom: 10px;
    }

    .composer {
      display: flex;
      gap: 12px;
      align-items: flex-end;
    }

    .composer-input {
      flex: 1;
      min-height: 52px;
      max-height: 140px;
      border: 1px solid #d0d5dd;
      border-radius: 14px;
      padding: 12px 14px;
      font-size: 15px;
      resize: none;
      outline: none;
    }

    .composer-input:focus {
      border-color: #86b7fe;
      box-shadow: 0 0 0 3px rgba(13,110,253,0.12);
    }

    .composer-actions {
      display: flex;
      gap: 10px;
      align-items: center;
    }

    .file-btn {
      width: 46px;
      height: 46px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f1f5f9;
      cursor: pointer;
      font-size: 22px;
      border: 1px solid #dbe2ea;
      margin: 0;
    }

    .disabled-btn {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .send-btn {
      border: none;
      background: #0d6efd;
      color: white;
      border-radius: 24px;
      padding: 11px 18px;
      font-weight: 600;
      min-width: 100px;
    }

    .send-btn:hover {
      background: #0b5ed7;
    }

    .send-btn:disabled {
      background: #94a3b8;
      cursor: not-allowed;
    }

    @media (max-width: 768px) {
      .chat-page {
        padding: 10px;
      }

      .chat-wrapper {
        height: calc(100vh - 90px);
        max-width: 100%;
      }

      .chat-header {
        flex-direction: column;
        align-items: flex-start;
      }

      .chat-body {
        padding: 14px;
      }

      .message-group {
        max-width: 90%;
      }

      .message-image {
        max-width: 100%;
      }

      .composer {
        flex-direction: column;
        align-items: stretch;
      }

      .composer-actions {
        justify-content: space-between;
      }
    }
  `]
})
export class ConversationDetailComponent implements OnInit, AfterViewChecked {
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  conversationId!: number;
  conversation: Conversation | null = null;

  messages: Message[] = [];
  newMessage = '';

  selectedImage: File | null = null;
  selectedImageName = '';

  editingMessageId: number | null = null;
  editedContent = '';

  loading = false;
  error = '';
  successMessage = '';

  currentUserEmail = '';

  constructor(
    private route: ActivatedRoute,
    private messagerieService: MessagerieService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.conversationId = Number(this.route.snapshot.paramMap.get('id'));
    this.currentUserEmail = this.authService.getEmail() || '';
    this.loadConversation();
    this.loadMessages();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      if (this.scrollContainer) {
        const el = this.scrollContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    } catch {}
  }

  loadConversation(): void {
    this.messagerieService.getConversationById(this.conversationId).subscribe({
      next: (data) => {
        this.conversation = data;
      },
      error: (err) => {
        console.log('Erreur chargement conversation = ', err);
      }
    });
  }

  loadMessages(): void {
    this.loading = true;
    this.error = '';

    this.messagerieService.getMessagesByConversation(this.conversationId).subscribe({
      next: (data) => {
        this.messages = data;
        this.loading = false;

        this.messagerieService.markConversationMessagesAsRead(this.conversationId).subscribe({
          next: () => {
            this.messages = this.messages.map(msg => {
              if (!this.isCurrentUserSender(msg)) {
                return { ...msg, isRead: true };
              }
              return msg;
            });
          },
          error: (err) => {
            console.log('Erreur marquage lu = ', err);
          }
        });
      },
      error: (err) => {
        console.log('Erreur chargement messages = ', err);
        this.error = 'Impossible de charger les messages.';
        this.loading = false;
      }
    });
  }

  closeConversation(): void {
    if (!this.conversation) {
      return;
    }

    const confirmed = confirm('Voulez-vous vraiment fermer cette conversation ?');
    if (!confirmed) {
      return;
    }

    this.messagerieService.updateConversationStatus(this.conversation.id, 'CLOSED').subscribe({
      next: (updatedConversation) => {
        this.conversation = updatedConversation;
        this.successMessage = 'Conversation fermée avec succès.';
      },
      error: (err) => {
        console.log('Erreur fermeture conversation = ', err);
        this.error = 'Impossible de fermer la conversation.';
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      this.selectedImage = input.files[0];
      this.selectedImageName = input.files[0].name;
    } else {
      this.selectedImage = null;
      this.selectedImageName = '';
    }
  }

  sendMessage(): void {
    this.error = '';
    this.successMessage = '';

    if (this.conversation?.status === 'CLOSED') {
      this.error = 'Impossible d’envoyer un message dans une conversation fermée.';
      return;
    }

    const hasText = !!this.newMessage.trim();
    const hasImage = !!this.selectedImage;

    if (!hasText && !hasImage) {
      this.error = 'Le message doit contenir un texte ou une image.';
      return;
    }

    this.messagerieService.sendMessage(
      this.conversationId,
      this.newMessage,
      this.selectedImage
    ).subscribe({
      next: () => {
        this.successMessage = 'Message envoyé avec succès.';
        this.newMessage = '';
        this.selectedImage = null;
        this.selectedImageName = '';
        this.loadMessages();
      },
      error: (err) => {
        console.log('Erreur envoi message = ', err);
        this.error = 'Impossible d’envoyer le message.';
      }
    });
  }

  getImageUrl(imagePath?: string | null): string {
    return this.messagerieService.getFullImageUrl(imagePath);
  }

  isCurrentUserSender(msg: Message): boolean {
    return msg.sender?.email === this.currentUserEmail;
  }

  startEdit(msg: Message): void {
    this.editingMessageId = msg.id;
    this.editedContent = msg.content || '';
    this.error = '';
    this.successMessage = '';
  }

  cancelEdit(): void {
    this.editingMessageId = null;
    this.editedContent = '';
  }

  updateMessage(messageId: number): void {
    this.error = '';
    this.successMessage = '';

    if (!this.editedContent.trim()) {
      this.error = 'Le contenu du message est obligatoire.';
      return;
    }

    this.messagerieService.updateMessage(messageId, {
      content: this.editedContent.trim()
    }).subscribe({
      next: () => {
        this.successMessage = 'Message modifié avec succès.';
        this.editingMessageId = null;
        this.editedContent = '';
        this.loadMessages();
      },
      error: (err) => {
        console.log('Erreur modification message = ', err);
        this.error = 'Impossible de modifier le message.';
      }
    });
  }

  deleteMessage(messageId: number): void {
    this.error = '';
    this.successMessage = '';

    const confirmed = confirm('Voulez-vous vraiment supprimer ce message ?');
    if (!confirmed) {
      return;
    }

    this.messagerieService.deleteMessage(messageId).subscribe({
      next: () => {
        this.successMessage = 'Message supprimé avec succès.';
        this.loadMessages();
      },
      error: (err) => {
        console.log('Erreur suppression message = ', err);
        this.error = 'Impossible de supprimer le message.';
      }
    });
  }
}