import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, combineLatest } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventFormComponent } from '../../components/event-form/event-form.component';
import { Event } from '../../models/event.model';
import { EventRequest } from '../../models/event-request.model';
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

  saveEvent(payload: EventRequest): void {
    this.submitting = true;
    this.errorMessage = null;

    const request$ =
      this.isEditMode && this.eventId
        ? this.eventService.updateEvent(this.eventId, payload)
        : this.eventService.createEvent(payload);

    request$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          if (!this.isEditMode) {
            this.notificationService.showSuccess("L'evenement a ete cree avec succes.");
            this.router.navigate(['/events']);
            return;
          }

          this.notificationService.showSuccess("L'evenement a ete mis a jour avec succes.");
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
    this.event = null;

    this.eventService
      .getEventById(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.loading = false))
      )
      .subscribe({
        next: (event) => {
          this.event = event;
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.event = null;
          this.loading = false;
          this.pageErrorMessage = this.getErrorMessage(
            error,
            "Impossible de charger l'evenement a modifier."
          );
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
