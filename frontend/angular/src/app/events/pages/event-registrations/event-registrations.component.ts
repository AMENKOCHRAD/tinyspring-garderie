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
import { Event } from '../../models/event.model';
import {
  EventRegistration,
  RegistrationStatus
} from '../../models/event-registration.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';

interface EventRegistrationsViewModel {
  loading: boolean;
  event: Event | null;
  registrations: EventRegistration[];
  errorMessage: string;
  registrationsWarning: string;
}

@Component({
  selector: 'app-event-registrations',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass, AsyncPipe],
  templateUrl: './event-registrations.component.html',
  styleUrls: ['./event-registrations.component.scss']
})
export class EventRegistrationsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly refresh$ = new Subject<void>();

  actionRegistrationId: number | null = null;

  readonly vm$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id'))),
    distinctUntilChanged(),
    switchMap((eventId) => {
      if (!eventId || Number.isNaN(eventId)) {
        return of<EventRegistrationsViewModel>({
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
                    'Impossible de charger les participations pour le moment.'
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
              } satisfies EventRegistrationsViewModel;
            }),
            catchError((error: HttpErrorResponse) =>
              of<EventRegistrationsViewModel>({
                loading: false,
                event: null,
                registrations: [],
                errorMessage: this.getErrorMessage(
                  error,
                  'Impossible de charger les participations.'
                ),
                registrationsWarning: ''
              })
            ),
            startWith<EventRegistrationsViewModel>({
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

  confirmRegistration(registration: EventRegistration): void {
    this.actionRegistrationId = registration.id;

    this.eventService
      .confirmRegistration(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('La participation a ete confirmee.');
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, 'La confirmation de la participation a echoue.')
          );
        }
      });
  }

  cancelRegistration(registration: EventRegistration): void {
    this.actionRegistrationId = registration.id;

    this.eventService
      .cancelRegistration(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('La participation a ete annulee.');
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, "L'annulation de la participation a echoue.")
          );
        }
      });
  }

  goToDetails(eventId: number): void {
    this.router.navigate(['/events', eventId]);
  }

  goToList(): void {
    this.router.navigate(['/events']);
  }

  getStatusClass(status: RegistrationStatus): string {
    switch (status) {
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-pending';
    }
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
