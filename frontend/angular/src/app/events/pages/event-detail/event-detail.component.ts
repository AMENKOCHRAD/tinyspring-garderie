import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  finalize,
  map,
  shareReplay,
  startWith,
  switchMap
} from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { Event, EventStatus } from '../../models/event.model';
import { EventRegistration, RegistrationStatus } from '../../models/event-registration.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';
import { getImageUrl } from '../../utils/photo-url.util';

interface EventDetailViewModel {
  loading: boolean;
  event: Event | null;
  registrations: EventRegistration[];
  errorMessage: string;
  registrationsWarning: string;
}

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass, AsyncPipe, EventsModuleSwitcherComponent],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss']
})
export class EventDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly refresh$ = new Subject<void>();

  readonly vm$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id'))),
    distinctUntilChanged(),
    switchMap((eventId) => {
      if (!eventId || Number.isNaN(eventId)) {
        return of<EventDetailViewModel>({
          loading: false,
          event: null,
          registrations: [],
          errorMessage: "L'identifiant de l'événement est invalide.",
          registrationsWarning: ''
        });
      }

      return this.refresh$.pipe(
        startWith(void 0),
        switchMap(() =>
          forkJoin({
            event: this.eventService.getEventById(eventId),
            registrations: this.eventService.getRegistrationsByEventId(eventId).pipe(
              catchError((error: HttpErrorResponse) =>
                of({
                  registrations: [] as EventRegistration[],
                  warning: this.getErrorMessage(
                    error,
                    'Impossible de charger le résumé des participations.'
                  )
                })
              )
            )
          }).pipe(
            map(({ event, registrations }) => {
              const registrationsData = Array.isArray(registrations)
                ? registrations
                : registrations.registrations;
              const registrationsWarning = Array.isArray(registrations)
                ? ''
                : registrations.warning;

              return {
                loading: false,
                event,
                registrations: registrationsData,
                errorMessage: '',
                registrationsWarning
              } satisfies EventDetailViewModel;
            }),
            catchError((error: HttpErrorResponse) =>
              of<EventDetailViewModel>({
                loading: false,
                event: null,
                registrations: [],
                errorMessage: this.getErrorMessage(
                  error,
                  "Impossible de charger l'événement."
                ),
                registrationsWarning: ''
              })
            ),
            startWith<EventDetailViewModel>({
              loading: true,
              event: null,
              registrations: [],
              errorMessage: '',
              registrationsWarning: ''
            })
          )
        )
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  publishing = false;

  publishEvent(event: Event): void {
    if (!this.canPublish(event)) {
      return;
    }

    this.publishing = true;

    this.eventService
      .publishEvent(event.id)
      .pipe(finalize(() => (this.publishing = false)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess("L'événement a été publié avec succès.");
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, "La publication de l'événement a échoué.")
          );
        }
      });
  }

  goToRegistrations(eventId: number): void {
    this.router.navigate(['/events', eventId, 'registrations']);
  }

  goToEdit(eventId: number): void {
    this.router.navigate(['/events', eventId, 'edit']);
  }

  goToRepublish(eventId: number): void {
    this.router.navigate(['/events', eventId, 'edit']);
  }

  goToList(): void {
    this.router.navigate(['/events']);
  }

  getStatusClass(status: EventStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'status-published';
      case 'CANCELLED':
        return 'status-cancelled';
      case 'COMPLETED':
        return 'status-completed';
      default:
        return 'status-draft';
    }
  }

  formatStatus(status: EventStatus): string {
    return status.replaceAll('_', ' ');
  }

  getDisplayStatus(event: Event): EventStatus {
    return event.status;
  }

  canEdit(event: Event): boolean {
    return !this.isCancelled(event);
  }

  canPublish(event: Event): boolean {
    return !this.isCancelled(event) && this.getDisplayStatus(event) === 'DRAFT';
  }

  canRepublish(event: Event): boolean {
    return !this.isCancelled(event) && this.getDisplayStatus(event) === 'COMPLETED';
  }

  canViewParticipations(event: Event): boolean {
    return !this.isCancelled(event);
  }

  getLockedReason(event: Event, action: 'edit' | 'publish' | 'registrations'): string {
    if (this.isCancelled(event)) {
      switch (action) {
        case 'edit':
          return "Un événement annulé ne peut pas être modifié";
        case 'publish':
          return "Un événement annulé ne peut pas être publié";
        default:
          return "Les participations sont indisponibles pour un événement annulé";
      }
    }

    return '';
  }

  getStatusActionHint(event: Event): string | null {
    if (this.isCancelled(event)) {
      return "Cet événement annulé est verrouillé : modification, publication et participations indisponibles.";
    }

    if (this.canRepublish(event)) {
      return "Utilisez Republier pour rouvrir le formulaire, modifier les dates puis republier l'événement.";
    }

    return null;
  }

  getRegistrationStatusClass(status: RegistrationStatus): string {
    switch (status) {
      case 'ATTENDED':
        return 'registration-attended';
      case 'ABSENT':
        return 'registration-absent';
      case 'CONFIRMED':
        return 'registration-confirmed';
      case 'CANCELLED':
        return 'registration-cancelled';
      case 'WAITLISTED':
        return 'registration-waitlisted';
      default:
        return 'registration-pending';
    }
  }

  getRegistrationStatusIcon(status: RegistrationStatus): string {
    switch (status) {
      case 'ATTENDED':
        return 'feather icon-check-circle';
      case 'ABSENT':
        return 'feather icon-x-circle';
      case 'CONFIRMED':
        return 'feather icon-user-check';
      case 'CANCELLED':
        return 'feather icon-slash';
      case 'WAITLISTED':
        return 'feather icon-clock';
      default:
        return 'feather icon-alert-circle';
    }
  }

  formatRegistrationStatus(status: RegistrationStatus): string {
    return status.replaceAll('_', ' ');
  }

  getEventPhotoUrl(photoEvent: string | undefined): string | null {
    return getImageUrl(photoEvent);
  }

  private getErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
    if (error.status === 404) {
      return "L'événement demandé est introuvable.";
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error?.message) {
      return error.error.message;
    }

    return fallbackMessage;
  }

  private isCancelled(event: Event): boolean {
    return event.status === 'CANCELLED';
  }
}
