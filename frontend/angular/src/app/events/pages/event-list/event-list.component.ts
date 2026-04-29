import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { BehaviorSubject, Subject, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, takeUntil, tap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { Event, EventStatus } from '../../models/event.model';
import { EventRequest } from '../../models/event-request.model';
import { EventNotificationService, EventToastMessage } from '../../services/event-notification.service';
import { EventService } from '../../services/event.service';
import { getSafeEventPhotoUrl } from '../../utils/photo-url.util';
import { EventRatingAdmin } from '../../models/event-rating-admin.model';

import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions, EventClickArg } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';

type EventDateFilter = 'ALL' | 'UPCOMING' | 'TODAY' | 'PAST';
type EventPriceFilter = 'ALL' | 'FREE' | 'PAID';

interface FilterOption<T extends string> {
  value: T;
  label: string;
}

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [
    SharedModule,
    RouterModule,
    DatePipe,
    NgClass,
    AsyncPipe,
    FormsModule,
    EventsModuleSwitcherComponent,
    FullCalendarModule
  ],
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
  private readonly searchSubject = new BehaviorSubject<string>('');
  private readonly statusFilterSubject = new BehaviorSubject<EventStatus | 'ALL'>('ALL');
  private readonly typeFilterSubject = new BehaviorSubject<string>('ALL');
  private readonly dateFilterSubject = new BehaviorSubject<EventDateFilter>('ALL');
  private readonly priceFilterSubject = new BehaviorSubject<EventPriceFilter>('ALL');

  private readonly currentPageSubject = new BehaviorSubject<number>(1);
  private readonly pageSizeSubject = new BehaviorSubject<number>(6);

  readonly toast$ = this.notificationService.message$;
  readonly loading$ = this.loadingSubject.asObservable();
  readonly errorMessage$ = this.errorSubject.asObservable();

  Math = Math;

  ratingsModalOpen = false;
  ratingsLoading = false;
  selectedRatingsEventTitle = '';
  selectedRatings: EventRatingAdmin[] = [];
  ratingsErrorMessage = '';

  readonly statusOptions: FilterOption<EventStatus | 'ALL'>[] = [
    { value: 'ALL', label: 'Tous' },
    { value: 'PUBLISHED', label: 'Publié' },
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'CANCELLED', label: 'Annulé' },
    { value: 'COMPLETED', label: 'Terminé' }
  ];

  readonly dateOptions: FilterOption<EventDateFilter>[] = [
    { value: 'ALL', label: 'Tous' },
    { value: 'UPCOMING', label: 'À venir' },
    { value: 'TODAY', label: "Aujourd'hui" },
    { value: 'PAST', label: 'Passés' }
  ];

  readonly priceOptions: FilterOption<EventPriceFilter>[] = [
    { value: 'ALL', label: 'Tous' },
    { value: 'FREE', label: 'Gratuit' },
    { value: 'PAID', label: 'Payant' }
  ];

  searchTerm = '';
  selectedStatus: EventStatus | 'ALL' = 'ALL';
  selectedType = 'ALL';
  selectedDateFilter: EventDateFilter = 'ALL';
  selectedPriceFilter: EventPriceFilter = 'ALL';

  currentPage = 1;
  pageSize = 6;

  actionInProgressId: number | null = null;

  calendarOptions: CalendarOptions = {
    initialView: 'dayGridMonth',
    plugins: [dayGridPlugin, interactionPlugin],
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,dayGridWeek'
    },
    locale: 'fr',
    height: 'auto',
    events: [],
    eventClick: (info: EventClickArg) => this.onCalendarEventClick(info)
  };

  readonly events$ = this.eventService.events$.pipe(
    tap(() => {
      this.loadingSubject.next(false);
      this.errorSubject.next('');
    }),
    catchError((error: HttpErrorResponse) => {
      this.loadingSubject.next(false);
      this.errorSubject.next(
        this.getErrorMessage(error, "Impossible de charger les événements.")
      );
      return of([] as Event[]);
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly typeOptions$ = this.events$.pipe(
    map((events) => {
      const types = Array.from(
        new Set(events.map((event) => event.type).filter((type) => type && type.trim()))
      ).sort((left, right) => left.localeCompare(right));

      return [
        { value: 'ALL', label: 'Tous' },
        ...types.map((type) => ({
          value: type,
          label: type.replaceAll('_', ' ')
        }))
      ];
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly vm$ = combineLatest([
    this.events$,
    this.loading$,
    this.errorMessage$,
    this.toast$.pipe(startWith(null as EventToastMessage | null)),
    this.searchSubject.asObservable(),
    this.statusFilterSubject.asObservable(),
    this.typeFilterSubject.asObservable(),
    this.dateFilterSubject.asObservable(),
    this.priceFilterSubject.asObservable(),
    this.typeOptions$,
    this.currentPageSubject.asObservable(),
    this.pageSizeSubject.asObservable()
  ]).pipe(
    map(([
      events,
      loading,
      errorMessage,
      toast,
      searchTerm,
      statusFilter,
      typeFilter,
      dateFilter,
      priceFilter,
      typeOptions,
      currentPage,
      pageSize
    ]) => {
      const filteredEvents = events.filter((event) =>
        this.matchesSearch(event, searchTerm) &&
        this.matchesStatus(event, statusFilter) &&
        this.matchesType(event, typeFilter) &&
        this.matchesDate(event, dateFilter) &&
        this.matchesPrice(event, priceFilter)
      );

      const totalFilteredEvents = filteredEvents.length;
      const totalPages = Math.max(1, Math.ceil(totalFilteredEvents / pageSize));
      const safeCurrentPage = Math.min(Math.max(currentPage, 1), totalPages);

      const startIndexRaw = (safeCurrentPage - 1) * pageSize;
      const endIndexRaw = startIndexRaw + pageSize;
      const paginatedEvents = filteredEvents.slice(startIndexRaw, endIndexRaw);

      return {
        events,
        filteredEvents,
        paginatedEvents,
        loading,
        errorMessage,
        toast,
        typeOptions,
        currentPage: safeCurrentPage,
        pageSize,
        totalPages,
        totalFilteredEvents,
        startIndex: totalFilteredEvents === 0 ? 0 : startIndexRaw + 1,
        endIndex: Math.min(endIndexRaw, totalFilteredEvents),
        hasActiveFilters:
          searchTerm.length > 0 ||
          statusFilter !== 'ALL' ||
          typeFilter !== 'ALL' ||
          dateFilter !== 'ALL' ||
          priceFilter !== 'ALL',
        totalEvents: events.length,
        publishedEvents: events.filter((event) => this.getDisplayStatus(event) === 'PUBLISHED').length,
        upcomingEvents: events.filter((event) => {
          const displayStatus = this.getDisplayStatus(event);
          return displayStatus !== 'CANCELLED' && new Date(event.startDatetime) > new Date();
        }).length
      };
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  ngOnInit(): void {
    this.eventService.refreshEvents();

    this.vm$
      .pipe(takeUntil(this.destroy$))
      .subscribe((vm) => {
        this.updateCalendarEvents(vm.filteredEvents);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  dismissToast(): void {
    this.notificationService.clear();
  }

  updateSearchTerm(value: string): void {
    this.searchTerm = value;
    this.searchSubject.next(value.trim());
    this.resetPagination();
  }

  updateStatusFilter(value: EventStatus | 'ALL'): void {
    this.selectedStatus = value;
    this.statusFilterSubject.next(value);
    this.resetPagination();
  }

  updateTypeFilter(value: string): void {
    this.selectedType = value;
    this.typeFilterSubject.next(value);
    this.resetPagination();
  }

  updateDateFilter(value: EventDateFilter): void {
    this.selectedDateFilter = value;
    this.dateFilterSubject.next(value);
    this.resetPagination();
  }

  updatePriceFilter(value: EventPriceFilter): void {
    this.selectedPriceFilter = value;
    this.priceFilterSubject.next(value);
    this.resetPagination();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = 'ALL';
    this.selectedType = 'ALL';
    this.selectedDateFilter = 'ALL';
    this.selectedPriceFilter = 'ALL';

    this.searchSubject.next('');
    this.statusFilterSubject.next('ALL');
    this.typeFilterSubject.next('ALL');
    this.dateFilterSubject.next('ALL');
    this.priceFilterSubject.next('ALL');
    this.resetPagination();
  }

  goToPage(page: number): void {
    if (page < 1) {
      return;
    }

    this.currentPage = page;
    this.currentPageSubject.next(page);
  }

  previousPage(): void {
    if (this.currentPage <= 1) {
      return;
    }

    this.goToPage(this.currentPage - 1);
  }

  nextPage(totalPages: number): void {
    if (this.currentPage >= totalPages) {
      return;
    }

    this.goToPage(this.currentPage + 1);
  }

  updatePageSize(value: number | string): void {
    const size = Number(value) || 6;

    this.pageSize = size;
    this.pageSizeSubject.next(size);
    this.resetPagination();
  }

  getPageNumbers(totalPages: number): number[] {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
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

  goToRepublish(eventId: number): void {
    this.router.navigate(['/events', eventId, 'edit']);
  }

  onCalendarEventClick(info: EventClickArg): void {
    const eventId = Number(info.event.id);

    if (!Number.isNaN(eventId)) {
      this.goToDetails(eventId);
    }
  }

  updateCalendarEvents(events: Event[]): void {
    this.calendarOptions = {
      ...this.calendarOptions,
      events: this.getCalendarEvents(events)
    };
  }

  getCalendarEvents(events: Event[]) {
    return events.map((event) => ({
      id: String(event.id),
      title: event.title || 'Événement sans titre',
      start: event.startDatetime,
      end: event.endDatetime,
      backgroundColor: this.getCalendarStatusColor(this.getDisplayStatus(event)),
      borderColor: this.getCalendarStatusColor(this.getDisplayStatus(event)),
      textColor: '#ffffff'
    }));
  }

  publishEvent(event: Event): void {
    if (!this.canPublish(event)) {
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
            this.getErrorMessage(error, "La publication de l'événement a échoué.")
          );
        }
      });
  }

  cancelEvent(event: Event): void {
    this.updateEventStatus(event, 'CANCELLED', "L'événement a été annulé avec succès.");
  }

  completeEvent(event: Event): void {
    this.updateEventStatus(event, 'COMPLETED', "L'événement a été marqué comme complété.");
  }

  deleteEvent(event: Event): void {
    const confirmed = window.confirm(
      `Supprimer l'événement "${event.title || 'sans titre'}" ?`
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
            this.getErrorMessage(error, "La suppression de l'événement a échoué.")
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
    return event.status;
  }

  canPublish(event: Event): boolean {
    return !this.isCancelled(event) && this.getDisplayStatus(event) === 'DRAFT';
  }

  canEdit(event: Event): boolean {
    return !this.isCancelled(event);
  }

  canCancel(event: Event): boolean {
    const displayStatus = this.getDisplayStatus(event);
    return displayStatus !== 'COMPLETED' && displayStatus !== 'CANCELLED';
  }

  canComplete(event: Event): boolean {
    const displayStatus = this.getDisplayStatus(event);
    return displayStatus !== 'COMPLETED' && displayStatus !== 'CANCELLED';
  }

  canViewParticipations(event: Event): boolean {
    return !this.isCancelled(event);
  }

  canRepublish(event: Event): boolean {
    return !this.isCancelled(event) && this.getDisplayStatus(event) === 'COMPLETED';
  }

  getStatusActionHint(event: Event): string | null {
    if (this.isCancelled(event)) {
      return null;
    }

    if (this.canRepublish(event)) {
      return null;
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
      targetClassroomIds: event.targetClassroomIds,
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
            this.getErrorMessage(error, "La mise à jour du statut a échoué.")
          );
        }
      });
  }

  private resetPagination(): void {
    this.currentPage = 1;
    this.currentPageSubject.next(1);
  }

  private getCalendarStatusColor(status: EventStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return '#16a34a';
      case 'CANCELLED':
        return '#dc2626';
      case 'COMPLETED':
        return '#7c3aed';
      case 'DRAFT':
      default:
        return '#2563eb';
    }
  }

  private getErrorMessage(error: HttpErrorResponse, fallbackMessage: string): string {
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

  private matchesSearch(event: Event, searchTerm: string): boolean {
    if (!searchTerm) {
      return true;
    }

    const normalizedSearch = searchTerm.toLowerCase();
    const haystack = [
      event.title,
      event.type.replaceAll('_', ' '),
      event.location,
      event.description
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    return haystack.includes(normalizedSearch);
  }

  private matchesStatus(event: Event, statusFilter: EventStatus | 'ALL'): boolean {
    return statusFilter === 'ALL' || this.getDisplayStatus(event) === statusFilter;
  }

  private matchesType(event: Event, typeFilter: string): boolean {
    return typeFilter === 'ALL' || event.type === typeFilter;
  }

  private matchesDate(event: Event, dateFilter: EventDateFilter): boolean {
    if (dateFilter === 'ALL') {
      return true;
    }

    const now = new Date();
    const start = new Date(event.startDatetime);
    const end = new Date(event.endDatetime);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return false;
    }

    if (dateFilter === 'UPCOMING') {
      return start.getTime() > now.getTime();
    }

    if (dateFilter === 'PAST') {
      return end.getTime() < now.getTime();
    }

    return start.toDateString() === now.toDateString();
  }

  private matchesPrice(event: Event, priceFilter: EventPriceFilter): boolean {
    if (priceFilter === 'ALL') {
      return true;
    }

    const price = event.eventPrice ?? 0;
    return priceFilter === 'FREE' ? price <= 0 : price > 0;
  }
}