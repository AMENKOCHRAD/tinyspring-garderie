import { AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { combineLatest, of } from 'rxjs';
import {
  catchError,
  distinctUntilChanged,
  finalize,
  map,
  shareReplay,
  startWith,
  switchMap,
  timeout
} from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventFormComponent } from '../../components/event-form/event-form.component';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { Event } from '../../models/event.model';
import { EventFormSubmission } from '../../models/event-form-submission.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';

interface EventCreateViewModel {
  loading: boolean;
  event: Event | null;
  pageErrorMessage: string | null;
}

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [SharedModule, RouterModule, EventFormComponent, AsyncPipe, EventsModuleSwitcherComponent],
  templateUrl: './event-create.component.html',
  styleUrls: ['./event-create.component.scss']
})
export class EventCreateComponent {
  private readonly eventService = inject(EventService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  isEditMode = false;
  eventId: number | null = null;
  submitting = false;
  errorMessage: string | null = null;

  readonly vm$ = combineLatest([this.route.data, this.route.paramMap]).pipe(
    map(([data, params]) => ({
      isEditMode: data['mode'] === 'edit',
      eventId: Number(params.get('id'))
    })),
    distinctUntilChanged(
      (previous, current) =>
        previous.isEditMode === current.isEditMode && previous.eventId === current.eventId
    ),
    switchMap(({ isEditMode, eventId }) => {
      this.isEditMode = isEditMode;
      this.eventId = Number.isNaN(eventId) ? null : eventId;
      this.errorMessage = null;

      if (!isEditMode) {
        return of<EventCreateViewModel>({
          loading: false,
          event: null,
          pageErrorMessage: null
        });
      }

      if (!eventId || Number.isNaN(eventId)) {
        return of<EventCreateViewModel>({
          loading: false,
          event: null,
          pageErrorMessage: "L'identifiant de l'evenement est invalide."
        });
      }

      return this.eventService.getEventById(eventId).pipe(
        timeout(10000),
        map((event) => ({
          loading: false,
          event,
          pageErrorMessage: null
        })),
        catchError((error: HttpErrorResponse) =>
          of<EventCreateViewModel>({
            loading: false,
            event: null,
            pageErrorMessage: this.getErrorMessage(
              error,
              "Impossible de charger l'evenement a modifier."
            )
          })
        ),
        startWith<EventCreateViewModel>({
          loading: true,
          event: null,
          pageErrorMessage: null
        })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  saveEvent(submission: EventFormSubmission): void {
    this.submitting = true;
    this.errorMessage = null;

    const saveRequest$ =
      this.isEditMode && this.eventId
        ? this.eventService.updateEvent(this.eventId, submission.payload)
        : this.eventService.createEvent(submission.payload);

    saveRequest$
      .pipe(
        switchMap((event) => {
          if (!submission.photoFile) {
            return of(event);
          }

          return this.eventService.uploadEventPhoto(event.id, submission.photoFile).pipe(
            timeout(10000),
            catchError(() => {
              this.notificationService.showError(
                "L'evenement a ete enregistre, mais l'image n'a pas pu etre envoyee."
              );
              return of(event);
            })
          );
        }),
        finalize(() => {
          this.submitting = false;
        })
      )
      .subscribe({
        next: () => {
          this.notificationService.showSuccess(
            this.isEditMode
              ? "L'evenement a ete mis a jour avec succes."
              : "L'evenement a ete cree avec succes."
          );
          this.router.navigate(['/events']);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            this.isEditMode
              ? "La mise a jour de l'evenement a echoue."
              : "La creation de l'evenement a echoue."
          );
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/events']);
  }

  get pageTitle(): string {
    return this.isEditMode ? 'Modifier un evenement' : 'Ajouter un evenement';
  }

  get pageDescription(): string {
    return this.isEditMode
      ? "Mettez a jour les informations de l'evenement."
      : "Renseignez les informations du nouvel evenement.";
  }

  get submitLabel(): string {
    return this.isEditMode ? 'Enregistrer les modifications' : "Creer l'evenement";
  }

  private getErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
    if (error.status === 404) {
      return "L'evenement demande est introuvable.";
    }

    if (error.error?.message) {
      return error.error.message;
    }

    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    return fallbackMessage;
  }
}
