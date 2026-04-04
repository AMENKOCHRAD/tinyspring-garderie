import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subject, forkJoin, of } from 'rxjs';
import { catchError, finalize, takeUntil } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event, EventStatus } from '../../models/event.model';
import { EventRegistration } from '../../models/event-registration.model';
import { EventService } from '../../services/event.service';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass],
  templateUrl: './event-detail.component.html',
  styleUrls: ['./event-detail.component.scss']
})
export class EventDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly destroy$ = new Subject<void>();

  event: Event | null = null;
  registrations: EventRegistration[] = [];
  loading = false;
  publishing = false;
  errorMessage = '';

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe({
      next: (params) => {
        const id = Number(params.get('id'));

        if (!id || Number.isNaN(id)) {
          this.errorMessage = "L'identifiant de l'événement est invalide.";
          this.event = null;
          this.registrations = [];
          this.loading = false;
          return;
        }

        this.loadEventDetails(id);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  publishEvent(): void {
    if (!this.event || this.event.status === 'PUBLISHED') {
      return;
    }

    this.publishing = true;
    this.errorMessage = '';

    this.eventService
      .publishEvent(this.event.id)
      .pipe(finalize(() => (this.publishing = false)))
      .subscribe({
        next: (event) => {
          this.event = event;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getErrorMessage(
            error,
            "La publication de l'événement a échoué."
          );
        }
      });
  }

  goToRegistrations(): void {
    if (this.event) {
      this.router.navigate(['/events', this.event.id, 'registrations']);
    }
  }

  goToEdit(): void {
    if (this.event) {
      this.router.navigate(['/events', this.event.id, 'edit']);
    }
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

  private loadEventDetails(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.event = null;
    this.registrations = [];

    forkJoin({
      event: this.eventService.getEventById(id),
      registrations: this.eventService
        .getRegistrationsByEventId(id)
        .pipe(catchError(() => of([])))
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
            "Impossible de charger l'événement."
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