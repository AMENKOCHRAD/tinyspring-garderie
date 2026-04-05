import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  filter,
  finalize,
  map,
  shareReplay,
  startWith,
  switchMap
} from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event, EventStatus } from '../../models/event.model';
import { EventRegistration } from '../../models/event-registration.model';
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
  imports: [SharedModule, RouterModule, DatePipe, NgClass, AsyncPipe],
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
          errorMessage: "L'identifiant de l'evenement est invalide.",
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
                    'Impossible de charger le resume des participations.'
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
                  "Impossible de charger l'evenement."
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
    if (event.status === 'PUBLISHED') {
      return;
    }

    this.publishing = true;

    this.eventService
      .publishEvent(event.id)
      .pipe(finalize(() => (this.publishing = false)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess("L'evenement a ete publie avec succes.");
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, "La publication de l'evenement a echoue.")
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

  getEventPhotoUrl(photoEvent: string | undefined): string | null {
    return getImageUrl(photoEvent);
  }

  private getErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
    if (error.status === 404) {
      return "L'evenement demande est introuvable.";
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error?.message) {
      return error.error.message;
    }

    return fallbackMessage;
  }
}
