import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { BehaviorSubject, Subject, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, takeUntil, tap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event, EventStatus } from '../../models/event.model';
import { EventRequest } from '../../models/event-request.model';
import { EventNotificationService, EventToastMessage } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';
import { getSafeEventPhotoUrl } from '../../utils/photo-url.util';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [SharedModule, RouterModule, DatePipe, NgClass, AsyncPipe],
  templateUrl: './event-list.component.html',
  styleUrls: ['./event-list.component.scss']
})
export class EventListComponent implements OnInit, OnDestroy {
  private readonly eventService = inject(EventService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();
  private readonly loadingSubject = new BehaviorSubject<boolean>(true);
  private readonly errorSubject = new BehaviorSubject<string>('');

  readonly toast$ = this.notificationService.message$;
  readonly loading$ = this.loadingSubject.asObservable();
  readonly errorMessage$ = this.errorSubject.asObservable();

  readonly events$ = this.eventService.events$.pipe(
    tap(() => {
      this.loadingSubject.next(false);
      this.errorSubject.next('');
    }),
    catchError((error: HttpErrorResponse) => {
      this.loadingSubject.next(false);
      this.errorSubject.next(
        this.getErrorMessage(error, "Impossible de charger les evenements.")
      );
      return of([] as Event[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly vm$ = combineLatest([
    this.events$,
    this.loading$,
    this.errorMessage$,
    this.toast$.pipe(startWith(null as EventToastMessage | null))
  ]).pipe(
    map(([events, loading, errorMessage, toast]) => ({
      events,
      loading,
      errorMessage,
      toast,
      totalEvents: events.length,
      publishedEvents: events.filter((event) => this.getDisplayStatus(event) === 'PUBLISHED').length,
      upcomingEvents: events.filter((event) => {
        const displayStatus = this.getDisplayStatus(event);
        return displayStatus !== 'CANCELLED' && new Date(event.startDatetime) > new Date();
      }).length
    }))
  );

  actionInProgressId: number | null = null;

  ngOnInit(): void {
    this.eventService.refreshEvents();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  dismissToast(): void {
    this.notificationService.clear();
  }

  goToNewEvent(): void {
    this.router.navigate(['/events/new']);
  }

  goToDetails(eventId: number): void {
    this.router.navigate(['/events', eventId]);
  }

  goToRegistrations(eventId: number): void {
    this.router.navigate(['/events', eventId, 'registrations']);
  }

  publishEvent(event: Event): void {
    const displayStatus = this.getDisplayStatus(event);

    if (displayStatus !== 'DRAFT') {
      return;
    }

    this.actionInProgressId = event.id;
    this.errorSubject.next('');

    this.eventService
      .publishEvent(event.id)
      .pipe(
        finalize(() => (this.actionInProgressId = null)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        error: (error: HttpErrorResponse) => {
          this.errorSubject.next(
            this.getErrorMessage(error, "La publication de l'evenement a echoue.")
          );
        }
      });
  }

  cancelEvent(event: Event): void {
    this.updateEventStatus(event, 'CANCELLED', "L'evenement a ete annule avec succes.");
  }

  completeEvent(event: Event): void {
    this.updateEventStatus(event, 'COMPLETED', "L'evenement a ete marque comme complete.");
  }

  deleteEvent(event: Event): void {
    const confirmed = window.confirm(
      `Supprimer l'evenement "${event.title || 'sans titre'}" ?`
    );

    if (!confirmed) {
      return;
    }

    this.actionInProgressId = event.id;
    this.errorSubject.next('');

    this.eventService
      .deleteEvent(event.id)
      .pipe(
        finalize(() => (this.actionInProgressId = null)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        error: (error: HttpErrorResponse) => {
          this.errorSubject.next(
            this.getErrorMessage(error, "La suppression de l'evenement a echoue.")
          );
        }
      });
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
    return status.replace(/_/g, ' ');
  }

  getDisplayStatus(event: Event): EventStatus {
    if (event.status === 'CANCELLED') {
      return 'CANCELLED';
    }

    if (event.status === 'COMPLETED') {
      return 'COMPLETED';
    }

    const endDate = new Date(event.endDatetime);

    if (!Number.isNaN(endDate.getTime()) && endDate.getTime() < Date.now()) {
      return 'COMPLETED';
    }

    return event.status;
  }

  canPublish(event: Event): boolean {
    if (event.status === 'PUBLISHED') {
      return false;
    }

    if (event.status === 'CANCELLED') {
      return true;
    }

    if (event.status === 'COMPLETED') {
      return this.hasFutureEndDate(event);
    }

    return this.getDisplayStatus(event) === 'DRAFT';
  }

  canEdit(event: Event): boolean {
    return true;
  }

  canCancel(event: Event): boolean {
    const displayStatus = this.getDisplayStatus(event);
    return displayStatus !== 'CANCELLED' && displayStatus !== 'COMPLETED';
  }

  canComplete(event: Event): boolean {
    const displayStatus = this.getDisplayStatus(event);
    return displayStatus !== 'COMPLETED' && displayStatus !== 'CANCELLED';
  }

  getStatusActionHint(event: Event): string | null {
    if (event.status === 'COMPLETED' && !this.hasFutureEndDate(event)) {
      return 'Modifiez la date de fin pour pouvoir republier cet événement.';
    }

    if (event.status === 'CANCELLED') {
      return "Cet événement annulé peut être modifié ou republié.";
    }

    return null;
  }

  getEventPhotoUrl(photoEvent: string | undefined): string | null {
    return getSafeEventPhotoUrl(photoEvent);
  }

  private updateEventStatus(
    event: Event,
    status: EventStatus,
    successMessage: string
  ): void {
    this.actionInProgressId = event.id;
    this.errorSubject.next('');

    const payload: EventRequest = {
      title: event.title,
      description: event.description,
      type: event.type,
      status,
      startDatetime: event.startDatetime,
      endDatetime: event.endDatetime,
      location: event.location,
      maxCapacity: event.maxCapacity,
      requiresAuthorization: event.requiresAuthorization,
      classroomId: event.classroomId,
      createdBy: event.createdBy,
      eventPrice: event.eventPrice,
      photoEvent: event.photoEvent
    };

    this.eventService
      .updateEvent(event.id, payload)
      .pipe(
        finalize(() => (this.actionInProgressId = null)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: () => {
          this.notificationService.showSuccess(successMessage);
        },
        error: (error: HttpErrorResponse) => {
          this.errorSubject.next(
            this.getErrorMessage(error, "La mise a jour du statut a echoue.")
          );
        }
      });
  }

  private getErrorMessage(
    error: HttpErrorResponse,
    fallbackMessage: string
  ): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error?.message) {
      return error.error.message;
    }

    return fallbackMessage;
  }

  private hasFutureEndDate(event: Event): boolean {
    const endDate = new Date(event.endDatetime);
    return !Number.isNaN(endDate.getTime()) && endDate.getTime() > Date.now();
  }
}
