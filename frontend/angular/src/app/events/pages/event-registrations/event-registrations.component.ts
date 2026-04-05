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
import { Event } from '../../models/event.model';
import {
  EventRegistration,
  RegistrationStatus
} from '../../models/event-registration.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';

interface EventRegistrationRow extends EventRegistration {
  eventId: number;
  eventTitle: string;
  eventType: string;
  eventStatus: string;
}

interface RegistrationStats {
  total: number;
  confirmed: number;
  attended: number;
  absent: number;
  cancelled: number;
}

interface EventRegistrationsViewModel {
  loading: boolean;
  event: Event | null;
  registrations: EventRegistrationRow[];
  errorMessage: string;
  registrationsWarning: string;
  isGlobalView: boolean;
  stats: RegistrationStats;
}

@Component({
  selector: 'app-event-registrations',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass, AsyncPipe, EventsModuleSwitcherComponent],
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
    map((params) => params.get('id')),
    distinctUntilChanged(),
    switchMap((eventIdParam) =>
      this.refresh$.pipe(
        startWith(void 0),
        switchMap(() =>
          eventIdParam ? this.loadSingleEventRegistrations(eventIdParam) : this.loadAllRegistrations()
        )
      )
    ),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  goToDetails(eventId: number): void {
    this.router.navigate(['/events', eventId]);
  }

  goToList(): void {
    this.router.navigate(['/events']);
  }

  markAttended(registration: EventRegistrationRow): void {
    this.actionRegistrationId = registration.id;

    this.eventService
      .markRegistrationAttended(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('La présence a été marquée.');
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, "Le marquage en présent a échoué.")
          );
        }
      });
  }

  markAbsent(registration: EventRegistrationRow): void {
    this.actionRegistrationId = registration.id;

    this.eventService
      .markRegistrationAbsent(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess("L'absence a été marquée.");
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.notificationService.showError(
            this.getErrorMessage(error, "Le marquage en absent a échoué.")
          );
        }
      });
  }

  getStatusClass(status: RegistrationStatus): string {
    switch (status) {
      case 'ATTENDED':
        return 'status-attended';
      case 'ABSENT':
        return 'status-absent';
      case 'CONFIRMED':
        return 'status-confirmed';
      case 'CANCELLED':
        return 'status-cancelled';
      case 'WAITLISTED':
        return 'status-waitlisted';
      default:
        return 'status-pending';
    }
  }

  getStatusIcon(status: RegistrationStatus): string {
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

  formatStatus(status: RegistrationStatus): string {
    return status.replaceAll('_', ' ');
  }

  canMarkAttendance(registration: EventRegistrationRow, isGlobalView: boolean, event: Event | null): boolean {
    if (registration.status === 'CANCELLED' || registration.status === 'ATTENDED' || registration.status === 'ABSENT') {
      return false;
    }

    if (isGlobalView) {
      return registration.eventStatus === 'COMPLETED';
    }

    return event?.status === 'COMPLETED';
  }

  private loadSingleEventRegistrations(eventIdParam: string) {
    const eventId = Number(eventIdParam);

    if (!eventId || Number.isNaN(eventId)) {
      return of<EventRegistrationsViewModel>({
        loading: false,
        event: null,
        registrations: [],
        errorMessage: "L'identifiant de l'événement est invalide.",
        registrationsWarning: '',
        isGlobalView: false,
        stats: this.buildStats([])
      });
    }

    return forkJoin({
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
          registrations: registrationsData.map((registration) =>
            this.toRegistrationRow(registration, event)
          ),
          errorMessage: '',
          registrationsWarning,
          isGlobalView: false,
          stats: this.buildStats(registrationsData)
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
          registrationsWarning: '',
          isGlobalView: false,
          stats: this.buildStats([])
        })
      ),
      startWith<EventRegistrationsViewModel>({
        loading: true,
        event: null,
        registrations: [],
        errorMessage: '',
        registrationsWarning: '',
        isGlobalView: false,
        stats: this.buildStats([])
      })
    );
  }

  private loadAllRegistrations() {
    return this.eventService.getAllEvents().pipe(
      switchMap((events) => {
        if (!events.length) {
          return of<EventRegistrationsViewModel>({
            loading: false,
            event: null,
            registrations: [],
            errorMessage: '',
            registrationsWarning: '',
            isGlobalView: true,
            stats: this.buildStats([])
          });
        }

        return forkJoin(
          events.map((event) =>
            this.eventService.getRegistrationsByEventId(event.id).pipe(
              map((registrations) => ({
                event,
                registrations,
                warning: ''
              })),
              catchError((error: HttpErrorResponse) =>
                of({
                  event,
                  registrations: [] as EventRegistration[],
                  warning: this.getErrorMessage(
                    error,
                    `Impossible de charger les participations pour ${event.title || 'cet événement'}.`
                  )
                })
              )
            )
          )
        ).pipe(
          map((results) => {
            const warnings = results
              .map((result) => result.warning)
              .filter((warning) => warning.trim())
              .join(' ');

            const registrations = results
              .flatMap((result) =>
                result.registrations.map((registration) =>
                  this.toRegistrationRow(registration, result.event)
                )
              )
              .sort((left, right) => right.registeredAt.localeCompare(left.registeredAt));

            return {
              loading: false,
              event: null,
              registrations,
              errorMessage: '',
              registrationsWarning: warnings,
              isGlobalView: true,
              stats: this.buildStats(registrations)
            } satisfies EventRegistrationsViewModel;
          })
        );
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
          registrationsWarning: '',
          isGlobalView: true,
          stats: this.buildStats([])
        })
      ),
      startWith<EventRegistrationsViewModel>({
        loading: true,
        event: null,
        registrations: [],
        errorMessage: '',
        registrationsWarning: '',
        isGlobalView: true,
        stats: this.buildStats([])
      })
    );
  }

  private toRegistrationRow(registration: EventRegistration, event: Event): EventRegistrationRow {
    return {
      ...registration,
      eventId: event.id,
      eventTitle: event.title,
      eventType: event.type,
      eventStatus: event.status
    };
  }

  private buildStats(registrations: Pick<EventRegistration, 'status'>[]): RegistrationStats {
    return {
      total: registrations.length,
      confirmed: registrations.filter((registration) => registration.status === 'CONFIRMED').length,
      attended: registrations.filter((registration) => registration.status === 'ATTENDED').length,
      absent: registrations.filter((registration) => registration.status === 'ABSENT').length,
      cancelled: registrations.filter((registration) => registration.status === 'CANCELLED').length
    };
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
}
