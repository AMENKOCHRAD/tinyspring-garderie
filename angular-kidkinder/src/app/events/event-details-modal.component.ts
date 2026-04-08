import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ParentEvent } from './events.models';

@Component({
  selector: 'app-event-details-modal',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="event-details-backdrop" *ngIf="isOpen && event" (click)="close.emit()">
      <div class="event-details-modal" (click)="$event.stopPropagation()">
        <button type="button" class="close-button" (click)="close.emit()">&times;</button>

        <div class="event-details-media">
          <img *ngIf="event.photoEvent; else placeholder" [src]="event.photoEvent" [alt]="event.title">
          <ng-template #placeholder>
            <div class="event-details-placeholder">Evenement TinySpring</div>
          </ng-template>
        </div>

        <div class="event-details-body">
          <div class="event-details-header">
            <div>
              <span class="kicker">Evenement</span>
              <h3>{{ event.title }}</h3>
            </div>
            <div class="badges">
              <span class="badge info">{{ event.type }}</span>
              <span class="badge warning" *ngIf="event.requiresAuthorization">Autorisation parentale</span>
              <span class="badge dark" *ngIf="event.full">Complet</span>
            </div>
          </div>

          <p class="description">{{ event.description || 'Aucune description disponible pour le moment.' }}</p>

          <div class="details-grid">
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
              <strong>{{ event.startDatetime | date:'fullDate' }} - {{ event.startDatetime | date:'HH:mm' }}</strong>
            </div>
            <div>
              <small>Fin</small>
              <strong>{{ event.endDatetime | date:'fullDate' }} - {{ event.endDatetime | date:'HH:mm' }}</strong>
            </div>
            <div *ngIf="event.eventPrice !== null">
              <small>Tarif</small>
              <strong>{{ event.eventPrice }} DT</strong>
            </div>
            <div *ngIf="event.remainingCapacity !== null">
              <small>Places restantes</small>
              <strong>{{ event.remainingCapacity }}</strong>
            </div>
          </div>

          <div class="details-footer">
            <button type="button" class="btn btn-outline-secondary" (click)="close.emit()">Fermer</button>
            <button
              type="button"
              class="btn btn-primary"
              [disabled]="event.full || !event.registrationOpen || !event.availableChildren.length"
              (click)="participate.emit(event)">
              <i class="fa mr-2" [ngClass]="event.availableChildren.length ? 'fa-user-plus' : 'fa-check-circle'"></i>
              {{ event.availableChildren.length ? 'Participer' : 'Participe' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .event-details-backdrop {
      position: fixed;
      inset: 0;
      z-index: 1045;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 1rem;
      background: rgba(18, 33, 56, 0.62);
      backdrop-filter: blur(5px);
    }

    .event-details-modal {
      position: relative;
      width: min(960px, 100%);
      max-height: 92vh;
      overflow: auto;
      border-radius: 34px;
      background: #fff;
      box-shadow: 0 28px 80px rgba(0, 0, 0, 0.28);
    }

    .close-button {
      position: absolute;
      top: 1rem;
      right: 1rem;
      z-index: 2;
      width: 44px;
      height: 44px;
      border: 0;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.92);
      font-size: 1.8rem;
      line-height: 1;
    }

    .event-details-media {
      height: 320px;
      background: #e9f7ff;
    }

    .event-details-media img,
    .event-details-placeholder {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .event-details-placeholder {
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, #1ab3cf, #163b64);
      color: #fff;
      font-size: 1.1rem;
      font-weight: 700;
    }

    .event-details-body {
      padding: 1.75rem;
    }

    .event-details-header {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: flex-start;
    }

    .kicker {
      display: inline-flex;
      padding: 0.35rem 0.7rem;
      border-radius: 999px;
      background: #ebf8ff;
      color: #1f7a8c;
      font-size: 0.78rem;
      font-weight: 700;
      text-transform: uppercase;
    }

    .event-details-header h3 {
      margin: 0.7rem 0 0;
      color: #163b64;
      font-size: 2rem;
    }

    .badges {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      justify-content: flex-end;
    }

    .badge {
      padding: 0.45rem 0.8rem;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
    }

    .badge.info { background: #eef5ff; color: #2453a6; }
    .badge.warning { background: #fff5cf; color: #946200; }
    .badge.dark { background: #153b63; color: #fff; }

    .description {
      margin: 1.2rem 0 0;
      color: #566983;
      line-height: 1.75;
    }

    .details-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 1rem;
      margin-top: 1.5rem;
    }

    .details-grid > div {
      padding: 1rem 1.1rem;
      border-radius: 22px;
      background: linear-gradient(180deg, #f6fbff, #ffffff);
    }

    .details-grid small {
      display: block;
      margin-bottom: 0.25rem;
      color: #8593a9;
    }

    .details-grid strong {
      color: #294669;
    }

    .details-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }

    @media (max-width: 767.98px) {
      .event-details-media {
        height: 220px;
      }

      .event-details-header,
      .details-footer {
        flex-direction: column;
      }

      .details-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class EventDetailsModalComponent {
  @Input() public isOpen = false;
  @Input() public event: ParentEvent | null = null;
  @Output() public close = new EventEmitter<void>();
  @Output() public participate = new EventEmitter<ParentEvent>();
}
