import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ParentChild, ParentEvent, ParentParticipationRequest } from './events.models';

@Component({
  selector: 'app-event-participation-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="participation-modal-backdrop" *ngIf="isOpen" (click)="close.emit()">
      <div class="participation-modal" *ngIf="event" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <div class="header-content">
            <span class="modal-kicker">Participation</span>
            <h3>{{ event.title }}</h3>
            <p class="subtitle">
              Sélectionnez l'enfant concerné puis confirmez l'inscription.
            </p>
          </div>

          <button type="button" class="close-button" (click)="close.emit()" aria-label="Fermer">
            &times;
          </button>
        </div>

        <div class="modal-body">
          <div class="event-summary">
            <div class="summary-item">
              <span class="summary-icon">🏫</span>
              <div>
                <small>Classe</small>
                <strong>
                  {{ event.classroomName || event.targetedClassroomNames.join(', ') || 'Classe non précisée' }}
                </strong>
              </div>
            </div>

            <div class="summary-item">
              <span class="summary-icon">📅</span>
              <div>
                <small>Date</small>
                <strong>{{ event.startDatetime | date:'dd/MM/yyyy HH:mm' }}</strong>
              </div>
            </div>

            <div class="summary-item">
              <span class="summary-icon">📍</span>
              <div>
                <small>Lieu</small>
                <strong>{{ event.location || 'Lieu non précisé' }}</strong>
              </div>
            </div>
          </div>

          <div class="warning-box" *ngIf="event.requiresAuthorization">
            <span class="warning-icon">⚠️</span>
            <div>
              Cet événement nécessite une autorisation parentale en PDF.
              Le document sera téléchargeable par l'administration.
            </div>
          </div>

          <div class="feedback error" *ngIf="validationError">
            {{ validationError }}
          </div>

          <div class="feedback error" *ngIf="serverError">
            {{ serverError }}
          </div>

          <div class="form-section">
            <label class="section-label">Enfant concerné</label>

            <div class="child-cards" *ngIf="eligibleChildren.length > 0; else noChildAvailable">
              <button
                type="button"
                class="child-card"
                *ngFor="let child of eligibleChildren"
                [class.selected]="child.id === selectedChildId"
                (click)="selectedChildId = child.id"
              >
                <strong>{{ child.firstName }} {{ child.lastName }}</strong>
                <small>{{ child.classroomName }}</small>
              </button>
            </div>

            <ng-template #noChildAvailable>
              <div class="empty-box">
                Aucun enfant éligible pour cet événement.
              </div>
            </ng-template>
          </div>

          <div class="form-section" *ngIf="event.requiresAuthorization">
            <label class="section-label">Autorisation parentale (PDF)</label>

            <label class="file-upload">
              <input
                type="file"
                accept="application/pdf"
                (change)="onFileSelected($event)"
              />

              <div class="file-upload-content">
                <span class="file-upload-icon">📄</span>

                <div class="file-upload-text">
                  <strong *ngIf="selectedFile; else emptyFileState">
                    {{ selectedFile.name }}
                  </strong>

                  <ng-template #emptyFileState>
                    <strong>Choisir un fichier PDF</strong>
                  </ng-template>

                  <small>Seuls les fichiers PDF sont acceptés.</small>
                </div>
              </div>
            </label>
          </div>

          <div class="form-section">
            <label class="section-label">Notes pour l'équipe</label>
            <textarea
              class="notes-field"
              rows="4"
              [(ngModel)]="notes"
              placeholder="Informations utiles sur la participation de votre enfant"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn-cancel" (click)="close.emit()">
            Annuler
          </button>

          <button
            type="button"
            class="btn-confirm"
            [disabled]="submitting || eligibleChildren.length === 0"
            (click)="submit()"
          >
            {{ submitting ? 'Enregistrement...' : 'Confirmer la participation' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .participation-modal-backdrop {
      position: fixed;
      inset: 0;
      z-index: 1050;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 1rem;
      background: rgba(15, 23, 42, 0.55);
      backdrop-filter: blur(6px);
    }

    .participation-modal {
      width: min(820px, 100%);
      max-height: 92vh;
      overflow: auto;
      border-radius: 28px;
      background: #ffffff;
      box-shadow: 0 30px 90px rgba(0, 0, 0, 0.24);
    }

    .modal-header,
    .modal-footer {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1rem;
      padding: 1.5rem 1.6rem;
    }

    .modal-header {
      border-bottom: 1px solid #edf2f7;
    }

    .modal-footer {
      border-top: 1px solid #edf2f7;
      align-items: center;
    }

    .modal-body {
      padding: 1.5rem;
    }

    .header-content h3 {
      margin: 0 0 0.35rem;
      color: #0f3b57;
      font-size: 2rem;
      font-weight: 800;
      line-height: 1.1;
    }

    .subtitle {
      margin: 0;
      color: #6b7280;
      font-size: 0.96rem;
    }

    .modal-kicker {
      display: inline-flex;
      align-items: center;
      margin-bottom: 0.7rem;
      padding: 0.42rem 0.82rem;
      border-radius: 999px;
      background: #e6f7fb;
      color: #1d8ca3;
      font-size: 0.78rem;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.03em;
    }

    .close-button {
      width: 44px;
      height: 44px;
      border: 0;
      border-radius: 50%;
      background: #f8fafc;
      color: #111827;
      font-size: 1.8rem;
      line-height: 1;
      cursor: pointer;
      transition: background 0.2s ease, transform 0.2s ease;
      flex-shrink: 0;
    }

    .close-button:hover {
      background: #eef2f7;
      transform: scale(1.04);
    }

    .event-summary {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 1rem;
      margin-bottom: 1.2rem;
    }

    .summary-item {
      display: flex;
      gap: 0.85rem;
      align-items: flex-start;
      padding: 1rem;
      border-radius: 18px;
      background: linear-gradient(180deg, #f9fbff 0%, #f4f8fc 100%);
      border: 1px solid #ebf1f6;
      min-height: 96px;
    }

    .summary-icon {
      font-size: 1.2rem;
      line-height: 1;
      margin-top: 0.15rem;
    }

    .summary-item small {
      display: block;
      margin-bottom: 0.25rem;
      color: #7b8794;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .summary-item strong {
      color: #374151;
      font-size: 1rem;
      font-weight: 700;
      line-height: 1.5;
      word-break: break-word;
    }

    .warning-box {
      display: flex;
      gap: 0.7rem;
      align-items: flex-start;
      margin-bottom: 1rem;
      padding: 1rem 1.1rem;
      border-radius: 14px;
      background: #fff8dc;
      border: 1px solid #f5deb3;
      color: #a16207;
      font-weight: 500;
    }

    .warning-icon {
      flex-shrink: 0;
      font-size: 1.1rem;
      margin-top: 0.05rem;
    }

    .feedback {
      margin-bottom: 1rem;
      padding: 0.9rem 1rem;
      border-radius: 12px;
      font-size: 0.95rem;
      font-weight: 500;
    }

    .feedback.error {
      background: #fff1f2;
      border: 1px solid #fecdd3;
      color: #be123c;
    }

    .form-section {
      margin-bottom: 1.3rem;
    }

    .section-label {
      display: inline-block;
      margin-bottom: 0.7rem;
      color: #5b6573;
      font-size: 0.98rem;
      font-weight: 700;
    }

    .child-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 0.8rem;
    }

    .child-card {
      border: 1px solid #e5e7eb;
      border-radius: 16px;
      background: #f8fafc;
      padding: 0.95rem 1rem;
      text-align: left;
      cursor: pointer;
      transition: all 0.2s ease;
      color: #1f2937;
    }

    .child-card:hover {
      background: #eef6ff;
      border-color: #bfdbfe;
      transform: translateY(-1px);
    }

    .child-card.selected {
      background: #e0f2fe;
      border: 2px solid #38bdf8;
      box-shadow: 0 8px 20px rgba(56, 189, 248, 0.16);
    }

    .child-card strong {
      display: block;
      font-size: 0.98rem;
      margin-bottom: 0.25rem;
    }

    .child-card small {
      color: #6b7280;
      font-size: 0.84rem;
    }

    .empty-box {
      padding: 1rem;
      border-radius: 14px;
      background: #f9fafb;
      border: 1px dashed #d1d5db;
      color: #6b7280;
      text-align: center;
    }

    .file-upload {
      display: block;
      cursor: pointer;
      margin: 0;
    }

    .file-upload input[type="file"] {
      display: none;
    }

    .file-upload-content {
      display: flex;
      gap: 0.9rem;
      align-items: center;
      padding: 1rem 1.1rem;
      border: 2px dashed #d6dde6;
      border-radius: 16px;
      background: #fbfdff;
      transition: border-color 0.2s ease, background 0.2s ease;
    }

    .file-upload:hover .file-upload-content {
      background: #f4faff;
      border-color: #8ecae6;
    }

    .file-upload-icon {
      font-size: 1.4rem;
      flex-shrink: 0;
    }

    .file-upload-text {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }

    .file-upload-text strong {
      color: #1f2937;
      font-size: 0.96rem;
    }

    .file-upload-text small {
      color: #6b7280;
      font-size: 0.82rem;
    }

    .notes-field {
      width: 100%;
      padding: 0.95rem 1rem;
      border-radius: 16px;
      border: 1px solid #dbe3ec;
      background: #fcfdff;
      color: #334155;
      resize: vertical;
      outline: none;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }

    .notes-field:focus {
      border-color: #67c6d8;
      box-shadow: 0 0 0 4px rgba(103, 198, 216, 0.15);
    }

    .btn-cancel,
    .btn-confirm {
      min-width: 150px;
      padding: 0.72rem 1.3rem;
      border-radius: 999px;
      font-size: 0.95rem;
      font-weight: 700;
      transition: all 0.2s ease;
    }

    .btn-cancel {
      border: 1px solid #0f5f82;
      background: #ffffff;
      color: #0f5f82;
    }

    .btn-cancel:hover {
      background: #f0f9ff;
    }

    .btn-confirm {
      border: none;
      background: linear-gradient(135deg, #16b5c9 0%, #1f9ed1 100%);
      color: #ffffff;
      box-shadow: 0 10px 24px rgba(31, 158, 209, 0.24);
    }

    .btn-confirm:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 14px 28px rgba(31, 158, 209, 0.28);
    }

    .btn-confirm:disabled {
      opacity: 0.7;
      cursor: not-allowed;
      box-shadow: none;
    }

    @media (max-width: 767.98px) {
      .participation-modal {
        border-radius: 22px;
      }

      .modal-header,
      .modal-body,
      .modal-footer {
        padding-left: 1rem;
        padding-right: 1rem;
      }

      .header-content h3 {
        font-size: 1.5rem;
      }

      .event-summary {
        grid-template-columns: 1fr;
      }

      .modal-footer {
        flex-direction: column-reverse;
        align-items: stretch;
      }

      .btn-cancel,
      .btn-confirm {
        width: 100%;
      }
    }
  `]
})
export class EventParticipationModalComponent implements OnChanges {
  @Input() public isOpen = false;
  @Input() public event: ParentEvent | null = null;
  @Input() public submitting = false;
  @Input() public serverError = '';

  @Output() public close = new EventEmitter<void>();
  @Output() public confirm = new EventEmitter<ParentParticipationRequest>();

  protected selectedChildId: number | null = null;
  protected selectedFile: File | null = null;
  protected notes = '';
  protected validationError = '';

  public ngOnChanges(changes: SimpleChanges): void {
    if (changes['event'] || changes['isOpen']) {
      this.validationError = '';
      this.notes = '';
      this.selectedFile = null;
      this.selectedChildId = this.eligibleChildren.length === 1 ? this.eligibleChildren[0].id : null;
    }
  }

  protected get eligibleChildren(): ParentChild[] {
    return this.event?.availableChildren ?? [];
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (file && file.type !== 'application/pdf') {
      this.validationError = 'Merci de joindre un fichier PDF uniquement.';
      this.selectedFile = null;
      input.value = '';
      return;
    }

    this.validationError = '';
    this.selectedFile = file;
  }

  protected submit(): void {
    if (!this.event) {
      return;
    }

    this.validationError = '';

    const isEligible = this.eligibleChildren.some((child) => child.id === this.selectedChildId);

    if (!isEligible || this.selectedChildId === null) {
      this.validationError = 'Veuillez sélectionner un enfant lié à la classe de cet événement.';
      return;
    }

    if (this.event.requiresAuthorization && !this.selectedFile) {
      this.validationError = 'Une autorisation parentale en PDF est obligatoire pour cet événement.';
      return;
    }

    this.confirm.emit({
      childId: this.selectedChildId,
      notes: this.notes.trim() || null,
      authorizationFile: this.selectedFile
    });
  }
}