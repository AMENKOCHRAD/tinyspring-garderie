import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, forkJoin } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event } from '../../models/event.model';
import {
  EventRegistration,
  RegistrationStatus
} from '../../models/event-registration.model';
import { EventService } from '../../services/event.service';

@Component({
  selector: 'app-event-registrations',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass],
  templateUrl: './event-registrations.component.html',
  styleUrls: ['./event-registrations.component.scss']
})
export class EventRegistrationsComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly destroy$ = new Subject<void>();

  event: Event | null = null;
  registrations: EventRegistration[] = [];
  loading = false;
  errorMessage = '';
  actionRegistrationId: number | null = null;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        const eventId = Number(params.get('id'));

        if (!eventId || Number.isNaN(eventId)) {
          this.event = null;
          this.registrations = [];
          this.errorMessage = "L'identifiant de l'événement est invalide.";
          this.loading = false;
          return;
        }

        this.loadPage(eventId);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  confirmRegistration(registration: EventRegistration): void {
    this.actionRegistrationId = registration.id;
    this.errorMessage = '';

    this.eventService
      .confirmRegistration(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          if (this.event) {
            this.loadPage(this.event.id);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            'La confirmation de la participation a échoué.'
          );
        }
      });
  }

  cancelRegistration(registration: EventRegistration): void {
    this.actionRegistrationId = registration.id;
    this.errorMessage = '';

    this.eventService
      .cancelRegistration(registration.id)
      .pipe(finalize(() => (this.actionRegistrationId = null)))
      .subscribe({
        next: () => {
          if (this.event) {
            this.loadPage(this.event.id);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            "L'annulation de la participation a échoué."
          );
        }
      });
  }

  goToDetails(): void {
    if (this.event) {
      this.router.navigate(['/events', this.event.id]);
    }
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

  private loadPage(eventId: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.event = null;
    this.registrations = [];

    forkJoin({
      event: this.eventService.getEventById(eventId),
      registrations: this.eventService.getRegistrationsByEventId(eventId)
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ event, registrations }) => {
          this.event = event;
          this.registrations = registrations;
        },
        error: (error: HttpErrorResponse) => {
          this.event = null;
          this.registrations = [];
          this.errorMessage = this.getErrorMessage(
            error,
            'Impossible de charger les participations.'
          );
        }
      });
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallbackMessage: string
  ): string {
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