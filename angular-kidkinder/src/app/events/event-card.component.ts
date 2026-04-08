import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ParentEvent } from './events.models';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <article class="event-card shadow-sm" [class.event-card--participating]="event.hasParticipation">
      <div class="event-card-media">
        <img
          *ngIf="event.photoEvent; else placeholder"
          [src]="event.photoEvent"
          [alt]="event.title">

        <ng-template #placeholder>
          <div class="event-card-placeholder">
            <span>Evenement TinySpring</span>
          </div>
        </ng-template>

        <div class="event-card-topline">
          <span class="status-chip type">{{ formatType(event.type) }}</span>
          <span class="status-chip neutral" *ngIf="!event.full && event.registrationOpen">Inscriptions ouvertes</span>
          <span class="status-chip warning" *ngIf="event.requiresAuthorization">Autorisation PDF</span>
          <span class="status-chip dark" *ngIf="event.full">Complet</span>
        </div>
      </div>

      <div class="event-card-body">
        <div class="event-card-header">
          <div>
            <h3>{{ event.title }}</h3>
            <p class="event-card-description">
              {{ event.description.length > 110 ? (event.description | slice:0:110) + '...' : event.description }}
            </p>
          </div>
          <div class="capacity-pill" *ngIf="event.remainingCapacity !== null">
            <strong>{{ event.remainingCapacity }}</strong>
            <span>places</span>
          </div>
        </div>

        <div class="event-card-meta">
          <div>
            <small>Classe</small>
            <strong>{{ event.classroomName || event.targetedClassroomNames.join(', ') || 'Classe non precisee' }}</strong>
          </div>
          <div>
            <small>Lieu</small>
            <strong>{{ event.location || 'Lieu a confirmer' }}</strong>
          </div>
          <div>
            <small>Debut</small>
            <strong>{{ event.startDatetime | date:'dd/MM/yyyy HH:mm' }}</strong>
          </div>
          <div>
            <small>Fin</small>
            <strong>{{ event.endDatetime | date:'dd/MM/yyyy HH:mm' }}</strong>
          </div>
        </div>

        <div class="participation-banner" *ngIf="event.hasParticipation">
          <i class="fa fa-check-circle"></i>
          <span>
            Deja inscrit :
            <strong>{{ getParticipationLabel() }}</strong>
          </span>
        </div>

        <div class="event-card-footnote">
          <span *ngIf="event.eventPrice !== null">Tarif : {{ event.eventPrice }} DT</span>
          <span>{{ event.availableChildren.length }} enfant(s) encore eligible(s)</span>
        </div>

        <div class="event-card-actions">
          <button type="button" class="icon-btn secondary" (click)="view.emit(event)" title="Voir les details">
            <i class="fa fa-eye"></i>
            <span>Voir</span>
          </button>

          <button
            type="button"
            class="icon-btn primary"
            [disabled]="isParticipateDisabled()"
            (click)="participate.emit(event)"
            [title]="getParticipateLabel()">
            <i class="fa" [ngClass]="getParticipateIcon()"></i>
            <span>{{ getParticipateLabel() }}</span>
          </button>
        </div>
      </div>
    </article>
  `,
  styles: [`
    .event-card {
      overflow: hidden;
      height: 100%;
      border: 1px solid rgba(22, 59, 100, 0.06);
      border-radius: 32px;
      background: #fff;
      box-shadow: 0 18px 40px rgba(22, 59, 100, 0.08);
    }

    .event-card--participating {
      box-shadow: 0 20px 46px rgba(22, 183, 212, 0.14);
      border-color: rgba(22, 183, 212, 0.18);
    }

    .event-card-media {
      position: relative;
      height: 248px;
      overflow: hidden;
      background: #eaf5ff;
    }

    .event-card-media img,
    .event-card-placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .event-card-placeholder {
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, #18b9d4, #163b64);
      color: #fff;
      font-size: 1.05rem;
      font-weight: 700;
    }

    .event-card-topline {
      position: absolute;
      inset: 1rem 1rem auto 1rem;
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .status-chip {
      padding: 0.45rem 0.78rem;
      border-radius: 999px;
      font-size: 0.74rem;
      font-weight: 700;
      box-shadow: 0 10px 26px rgba(0, 0, 0, 0.15);
    }

    .status-chip.type { background: rgba(22, 59, 100, 0.92); color: #fff; }
    .status-chip.neutral { background: rgba(255, 255, 255, 0.94); color: #22577a; }
    .status-chip.warning { background: rgba(255, 244, 205, 0.96); color: #8e6500; }
    .status-chip.dark { background: rgba(31, 41, 55, 0.94); color: #fff; }

    .event-card-body {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 1.4rem;
    }

    .event-card-header {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: flex-start;
    }

    .event-card-body h3 {
      margin: 0;
      color: #163b64;
      font-size: 1.8rem;
      line-height: 1.05;
    }

    .event-card-description {
      margin: 0.7rem 0 0;
      color: #5f6c84;
      line-height: 1.65;
    }

    .capacity-pill {
      min-width: 88px;
      padding: 0.85rem 0.9rem;
      border-radius: 22px;
      background: linear-gradient(180deg, #f2fbff, #ffffff);
      text-align: center;
    }

    .capacity-pill strong {
      display: block;
      color: #16a5c7;
      font-size: 1.4rem;
      line-height: 1;
    }

    .capacity-pill span {
      color: #74839a;
      font-size: 0.85rem;
    }

    .event-card-meta {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.85rem;
      padding: 1rem;
      border-radius: 22px;
      background: linear-gradient(180deg, #f7fbff, #ffffff);
    }

    .event-card-meta small {
      display: block;
      margin-bottom: 0.15rem;
      color: #8593a9;
    }

    .event-card-meta strong {
      color: #294669;
      font-size: 0.98rem;
    }

    .participation-banner {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.95rem 1rem;
      border-radius: 18px;
      background: linear-gradient(135deg, #effcf7, #f8fffc);
      color: #18603d;
      font-weight: 600;
    }

    .participation-banner i {
      font-size: 1.1rem;
    }

    .event-card-footnote {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem 1rem;
      color: #50627a;
      font-size: 0.95rem;
    }

    .event-card-actions {
      display: flex;
      gap: 0.8rem;
      margin-top: auto;
    }

    .icon-btn {
      display: inline-flex;
      justify-content: center;
      align-items: center;
      gap: 0.6rem;
      flex: 1;
      padding: 0.95rem 1rem;
      border-radius: 18px;
      border: 0;
      font-weight: 700;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .icon-btn:not(:disabled):hover {
      transform: translateY(-1px);
    }

    .icon-btn.secondary {
      background: #eef6ff;
      color: #204d8d;
    }

    .icon-btn.primary {
      background: linear-gradient(135deg, #17b6d2, #1276b8);
      color: #fff;
      box-shadow: 0 14px 28px rgba(18, 118, 184, 0.24);
    }

    .icon-btn:disabled {
      background: #edf2f7;
      color: #95a2b6;
      box-shadow: none;
      cursor: not-allowed;
    }

    @media (max-width: 575.98px) {
      .event-card-header,
      .event-card-actions {
        flex-direction: column;
      }

      .event-card-meta {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class EventCardComponent {
  @Input({ required: true }) public event!: ParentEvent;
  @Output() public view = new EventEmitter<ParentEvent>();
  @Output() public participate = new EventEmitter<ParentEvent>();

  protected formatType(type: string): string {
    return type.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
  }

  protected getParticipationLabel(): string {
    return this.event.activeParticipations
      .map((participation) => `${participation.childFullName} (${this.formatType(participation.status)})`)
      .join(', ');
  }

  protected isParticipateDisabled(): boolean {
    return this.event.full || !this.event.registrationOpen || this.event.availableChildren.length === 0;
  }

  protected getParticipateLabel(): string {
    if (this.event.full) {
      return 'Complet';
    }

    if (!this.event.registrationOpen) {
      return 'Ferme';
    }

    if (this.event.availableChildren.length === 0 && this.event.hasParticipation) {
      return 'Participe';
    }

    return 'Participer';
  }

  protected getParticipateIcon(): string {
    if (this.event.availableChildren.length === 0 && this.event.hasParticipation) {
      return 'fa-check-circle';
    }

    if (this.event.full) {
      return 'fa-ban';
    }

    return 'fa-user-plus';
  }
}
