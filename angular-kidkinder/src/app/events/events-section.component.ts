import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { EventCardComponent } from './event-card.component';
import { EventDetailsModalComponent } from './event-details-modal.component';
import { EventParticipationModalComponent } from './event-participation-modal.component';
import { EventsTabsComponent, ParentEventsTab } from './events-tabs.component';
import { ParentChild, ParentEvent, ParentParticipation, ParentParticipationRequest } from './events.models';
import { ParentEventsService } from './parent-events.service';

@Component({
  selector: 'app-events-section',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    EventsTabsComponent,
    EventCardComponent,
    EventDetailsModalComponent,
    EventParticipationModalComponent
  ],
  template: `
    <section id="events-section" class="parent-events-section bg-white shadow-sm mt-4">
      <div class="events-hero">
        <div>
          <p class="section-title pr-5 mb-2"><span class="pr-2">Evenements</span></p>
          <h2 class="mb-2">Suivez les activites de vos enfants plus facilement</h2>
          <p class="text-muted mb-0">
            Retrouvez ici les evenements de leurs classes, votre statut de participation et les annulations encore autorisees.
          </p>
        </div>

        <div class="hero-stats" *ngIf="!loading && !error">
          <div class="hero-stat">
            <small>Evenements</small>
            <strong>{{ events.length }}</strong>
          </div>
          <div class="hero-stat">
            <small>Participations confirmees</small>
            <strong>{{ getConfirmedParticipationsCount() }}</strong>
          </div>
          <div class="hero-stat">
            <small>Annulations possibles</small>
            <strong>{{ getCancellableParticipationsCount() }}</strong>
          </div>
        </div>
      </div>

      <app-events-tabs [activeTab]="activeTab" (tabChange)="activeTab = $event"></app-events-tabs>

      <div class="alert alert-success mt-4" *ngIf="successMessage">{{ successMessage }}</div>

      <div class="event-state-card mt-4" *ngIf="loading">
        <div class="spinner-border text-primary mb-3" role="status"></div>
        <p class="mb-0">Chargement de vos evenements et participations...</p>
      </div>

      <div class="event-state-card error mt-4" *ngIf="!loading && error">
        <h4>Impossible de charger la section Evenements</h4>
        <p class="mb-0">{{ error }}</p>
      </div>

      <ng-container *ngIf="!loading && !error">
        <div class="mt-4" *ngIf="activeTab === 'events'">
          <div class="event-state-card empty" *ngIf="!children.length">
            <h4>Aucun enfant lie a ce parent</h4>
            <p class="mb-0">Les evenements apparaissent automatiquement selon les classes rattachees a vos enfants.</p>
          </div>

          <div class="event-state-card empty" *ngIf="children.length && !events.length">
            <h4>Aucun evenement disponible</h4>
            <p class="mb-0">Aucun evenement publie n'est disponible pour les classes de vos enfants pour le moment.</p>
          </div>

          <div class="events-grid" *ngIf="events.length">
            <app-event-card
              *ngFor="let event of events; trackBy: trackByEventId"
              [event]="event"
              (view)="openDetails(event)"
              (participate)="openParticipation(event)">
            </app-event-card>
          </div>
        </div>

        <div class="mt-4" *ngIf="activeTab === 'participations'">
          <div class="event-state-card empty" *ngIf="!participations.length">
            <h4>Aucune participation pour le moment</h4>
            <p class="mb-0">Les inscriptions confirmees de vos enfants apparaitront ici.</p>
          </div>

          <div class="participation-grid" *ngIf="participations.length">
            <article class="participation-card" *ngFor="let participation of participations">
              <div class="participation-card-top">
                <div>
                  <span class="participation-kicker">{{ participation.childFullName }}</span>
                  <h5>{{ participation.eventTitle }}</h5>
                </div>
                <span class="participation-status" [class.cancelled]="participation.status === 'CANCELLED'">
                  {{ formatStatus(participation.status) }}
                </span>
              </div>

              <div class="participation-meta">
                <div>
                  <small>Date de l'evenement</small>
                  <strong>{{ participation.eventStartDatetime | date:'fullDate' }} - {{ participation.eventStartDatetime | date:'HH:mm' }}</strong>
                </div>
                <div>
                  <small>Inscription</small>
                  <strong>{{ participation.registeredAt | date:'medium' }}</strong>
                </div>
              </div>

              <div class="participation-note" *ngIf="participation.cancellableByParent && participation.status === 'CONFIRMED'">
                <i class="fa fa-clock-o"></i>
                <span>Annulation possible jusqu'a 12h avant le debut de l'evenement.</span>
              </div>

              <div class="participation-actions">
                <a
                  *ngIf="participation.authorizationDocUrl"
                  class="icon-link"
                  [href]="participation.authorizationDocUrl"
                  target="_blank"
                  rel="noopener noreferrer">
                  <i class="fa fa-file-pdf-o"></i>
                  <span>PDF autorisation</span>
                </a>

                <button
                  *ngIf="participation.cancellableByParent && participation.status === 'CONFIRMED'"
                  type="button"
                  class="btn btn-outline-danger"
                  [disabled]="cancellingRegistrationId === participation.registrationId"
                  (click)="cancelParticipation(participation)">
                  <i class="fa fa-times-circle mr-2"></i>
                  {{ cancellingRegistrationId === participation.registrationId ? 'Annulation...' : 'Annuler la participation' }}
                </button>
              </div>
            </article>
          </div>
        </div>

        <div class="event-state-card empty mt-4" *ngIf="activeTab === 'menus'">
          <h4>Section Menus a venir</h4>
          <p class="mb-0">La navigation est deja prete, la partie Menus viendra ensuite.</p>
        </div>
      </ng-container>

      <app-event-details-modal
        [isOpen]="!!detailedEvent"
        [event]="detailedEvent"
        (close)="closeDetails()"
        (participate)="startParticipationFromDetails($event)">
      </app-event-details-modal>

      <app-event-participation-modal
        [isOpen]="!!selectedEvent"
        [event]="selectedEvent"
        [submitting]="submitting"
        [serverError]="participationError"
        (close)="closeParticipation()"
        (confirm)="confirmParticipation($event)">
      </app-event-participation-modal>
    </section>
  `,
  styles: [`
    .parent-events-section {
      padding: 1.6rem;
      border-radius: 34px;
    }

    .events-hero {
      display: flex;
      justify-content: space-between;
      gap: 1.5rem;
      align-items: flex-start;
      margin-bottom: 1.35rem;
    }

    .hero-stats {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.85rem;
      min-width: 420px;
    }

    .hero-stat {
      padding: 1rem 1.1rem;
      border-radius: 24px;
      background: linear-gradient(180deg, #f4fbff, #ffffff);
      text-align: center;
    }

    .hero-stat small {
      display: block;
      color: #8292a8;
      margin-bottom: 0.25rem;
    }

    .hero-stat strong {
      color: #163b64;
      font-size: 1.6rem;
    }

    .events-grid,
    .participation-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 1.5rem;
    }

    .event-state-card {
      display: grid;
      place-items: center;
      min-height: 220px;
      padding: 2rem;
      border-radius: 28px;
      text-align: center;
      background: linear-gradient(180deg, #f8fbff, #fff);
    }

    .event-state-card.error {
      background: linear-gradient(180deg, #fff5f5, #fff);
    }

    .participation-card {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 1.4rem;
      border-radius: 26px;
      background: linear-gradient(180deg, #f8fbff, #ffffff);
      box-shadow: 0 18px 38px rgba(22, 59, 100, 0.07);
    }

    .participation-card-top {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      align-items: flex-start;
    }

    .participation-kicker {
      display: inline-flex;
      padding: 0.35rem 0.7rem;
      border-radius: 999px;
      background: #ebf8ff;
      color: #1f7a8c;
      font-size: 0.76rem;
      font-weight: 700;
      text-transform: uppercase;
    }

    .participation-card h5 {
      margin: 0.65rem 0 0;
      color: #163b64;
      font-size: 1.45rem;
    }

    .participation-status {
      padding: 0.5rem 0.85rem;
      border-radius: 999px;
      background: #eaf8f0;
      color: #18704c;
      font-weight: 700;
      white-space: nowrap;
    }

    .participation-status.cancelled {
      background: #fff1f2;
      color: #c53030;
    }

    .participation-meta {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.85rem;
    }

    .participation-meta > div {
      padding: 0.95rem 1rem;
      border-radius: 20px;
      background: #fff;
      border: 1px solid rgba(22, 59, 100, 0.06);
    }

    .participation-meta small {
      display: block;
      margin-bottom: 0.2rem;
      color: #8493a8;
    }

    .participation-note {
      display: flex;
      align-items: center;
      gap: 0.7rem;
      padding: 0.95rem 1rem;
      border-radius: 18px;
      background: #fff8e6;
      color: #8b5c00;
      font-weight: 600;
    }

    .participation-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      gap: 0.9rem;
      align-items: center;
    }

    .icon-link {
      display: inline-flex;
      align-items: center;
      gap: 0.55rem;
      color: #1d73b2;
      font-weight: 700;
      text-decoration: none;
    }

    @media (max-width: 1199.98px) {
      .events-hero {
        flex-direction: column;
      }

      .hero-stats {
        min-width: 0;
        width: 100%;
      }

      .events-grid,
      .participation-grid {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 767.98px) {
      .hero-stats,
      .participation-meta {
        grid-template-columns: 1fr;
      }

      .participation-card-top,
      .participation-actions {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `]
})
export class EventsSectionComponent implements OnInit {
  protected activeTab: ParentEventsTab = 'events';
  protected loading = true;
  protected submitting = false;
  protected cancellingRegistrationId: number | null = null;
  protected error = '';
  protected participationError = '';
  protected successMessage = '';
  protected children: ParentChild[] = [];
  protected events: ParentEvent[] = [];
  protected participations: ParentParticipation[] = [];
  protected selectedEvent: ParentEvent | null = null;
  protected detailedEvent: ParentEvent | null = null;

  constructor(private readonly parentEventsService: ParentEventsService) {}

  public ngOnInit(): void {
    this.loadAllData();
  }

  protected trackByEventId(_index: number, event: ParentEvent): number {
    return event.id;
  }

  protected openDetails(event: ParentEvent): void {
    this.successMessage = '';
    this.detailedEvent = event;
  }

  protected closeDetails(): void {
    this.detailedEvent = null;
  }

  protected openParticipation(event: ParentEvent): void {
    if (event.availableChildren.length === 0) {
      return;
    }

    this.successMessage = '';
    this.participationError = '';
    this.selectedEvent = event;
  }

  protected startParticipationFromDetails(event: ParentEvent): void {
    this.closeDetails();
    this.openParticipation(event);
  }

  protected closeParticipation(): void {
    this.selectedEvent = null;
    this.participationError = '';
  }

  protected confirmParticipation(payload: ParentParticipationRequest): void {
    if (!this.selectedEvent) {
      return;
    }

    this.submitting = true;
    this.participationError = '';

    this.parentEventsService.participateToEvent(this.selectedEvent.id, payload).subscribe({
      next: () => {
        this.submitting = false;
        this.successMessage = 'Participation confirmee avec succes.';
        this.closeParticipation();
        this.activeTab = 'participations';
        this.loadAllData();
      },
      error: (error: { error?: { message?: string } | string; message?: string }) => {
        this.submitting = false;
        if (typeof error.error === 'string') {
          this.participationError = error.error;
          return;
        }

        this.participationError =
          error.error?.message ||
          error.message ||
          'La participation n a pas pu etre enregistree.';
      }
    });
  }

  protected cancelParticipation(participation: ParentParticipation): void {
    this.cancellingRegistrationId = participation.registrationId;
    this.successMessage = '';
    this.error = '';

    this.parentEventsService.cancelParticipation(participation.registrationId).subscribe({
      next: () => {
        this.cancellingRegistrationId = null;
        this.successMessage = 'La participation a ete annulee avec succes.';
        this.activeTab = 'participations';
        this.loadAllData();
      },
      error: (error: { error?: { message?: string } | string; message?: string }) => {
        this.cancellingRegistrationId = null;
        if (typeof error.error === 'string') {
          this.error = error.error;
          return;
        }

        this.error =
          error.error?.message ||
          error.message ||
          'L annulation de la participation a echoue.';
      }
    });
  }

  protected formatStatus(status: string): string {
    return status.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase());
  }

  protected getConfirmedParticipationsCount(): number {
    return this.participations.filter((participation) => participation.status === 'CONFIRMED').length;
  }

  protected getCancellableParticipationsCount(): number {
    return this.participations.filter((participation) => participation.cancellableByParent).length;
  }

  private loadAllData(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      children: this.parentEventsService.getMyChildren(),
      events: this.parentEventsService.getParentEvents(),
      participations: this.parentEventsService.getParticipations()
    }).subscribe({
      next: ({ children, events, participations }) => {
        this.children = children;
        this.participations = participations;
        this.events = this.decorateEvents(events, participations);
        this.loading = false;
      },
      error: (error: { error?: { message?: string } | string; message?: string }) => {
        this.loading = false;
        if (typeof error.error === 'string') {
          this.error = error.error;
          return;
        }

        this.error =
          error.error?.message ||
          error.message ||
          'Impossible de recuperer les enfants, les evenements ou les participations du parent connecte.';
      }
    });
  }

  private decorateEvents(events: ParentEvent[], participations: ParentParticipation[]): ParentEvent[] {
    return events.map((event) => {
      const eventParticipations = participations.filter((participation) => participation.eventId === event.id);
      const registeredChildIds = new Set(eventParticipations.map((participation) => participation.childId));
      const activeParticipations = eventParticipations.filter((participation) =>
        ['CONFIRMED', 'PENDING', 'WAITLISTED', 'ATTENDED', 'ABSENT', 'CANCELLED'].includes(participation.status)
      );

      return {
        ...event,
        activeParticipations,
        hasParticipation: activeParticipations.length > 0,
        availableChildren: event.eligibleChildren.filter((child) => !registeredChildIds.has(child.id))
      };
    });
  }
}
