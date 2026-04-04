import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, combineLatest, of } from 'rxjs';
import { catchError, finalize, switchMap, takeUntil, timeout } from 'rxjs/operators';


import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventFormComponent } from '../../components/event-form/event-form.component';
import { Event } from '../../models/event.model';
import { EventFormSubmission } from '../../models/event-form-submission.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';

@Component({
  selector: 'app-event-create',
  standalone: true,
  imports: [SharedModule, RouterModule, EventFormComponent],
  templateUrl: './event-create.component.html',
  styleUrls: ['./event-create.component.scss']
})
export class EventCreateComponent implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();
  private readonly cdr = inject(ChangeDetectorRef);

  event: Event | null = null;
  isEditMode = false;
  eventId: number | null = null;
  loading = false;
  submitting = false;
  errorMessage: string | null = null;
  pageErrorMessage: string | null = null;

  ngOnInit(): void {
    combineLatest([this.route.data, this.route.paramMap])
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ([data, params]) => {
          this.isEditMode = data['mode'] === 'edit';
          this.errorMessage = null;
          this.pageErrorMessage = null;

          const idParam = params.get('id');
          this.eventId = idParam ? Number(idParam) : null;

          if (!this.isEditMode) {
            this.event = null;
            this.loading = false;
            return;
          }

          if (!this.eventId || Number.isNaN(this.eventId)) {
            this.event = null;
            this.loading = false;
            this.pageErrorMessage = "L'identifiant de l'evenement est invalide.";
            return;
          }

          this.loadEvent(this.eventId);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

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
        }),
        takeUntil(this.destroy$)
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

  private loadEvent(id: number): void {
    this.loading = true;
  this.pageErrorMessage = null;
  this.errorMessage = null;

  this.eventService
    .getEventById(id)
    .pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    }))
    .subscribe({
      next: (event) => {
        this.event = event;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.event = null;
        this.pageErrorMessage = this.getErrorMessage(
          error,
          "Impossible de charger l'événement à modifier."
        );
        this.cdr.detectChanges();
      }
    });
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallbackMessage: string
  ): string {
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
