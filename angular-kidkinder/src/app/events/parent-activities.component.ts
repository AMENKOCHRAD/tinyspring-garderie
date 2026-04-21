import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ParentActivitiesService } from './parent-activities.service';
import {
  ActivityFilter,
  DecoratedParentEvent,
  ParentActivitiesData,
  ParentChild,
  ParentParticipation
} from './parent-activities.models';

@Component({
  selector: 'app-parent-activities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-activities.component.html',
  styleUrl: './parent-activities.component.css'
})
export class ParentActivitiesComponent {
  private readonly activitiesService = inject(ParentActivitiesService);
  private readonly destroyRef = inject(DestroyRef);

  protected hoveredStars: Record<number, number> = {};
  protected ratingLoadingEventId: number | null = null;

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly selectedFilter = signal<ActivityFilter>('TOUS');
  protected readonly selectedEventId = signal<number | null>(null);
  protected readonly selectedChildId = signal<number | null>(null);
  protected readonly parentNotes = signal('');
  protected readonly selectedAuthorizationFile = signal<File | null>(null);
  protected readonly isSubmitting = signal(false);
  protected readonly cancellingParticipationId = signal<number | null>(null);
  protected readonly activitiesData = signal<ParentActivitiesData | null>(null);

  protected readonly filters: Array<{ key: ActivityFilter; label: string }> = [
    { key: 'TOUS', label: 'Tous' },
    { key: 'SORTIES', label: 'Sorties' },
    { key: 'FETES', label: 'Fetes' },
    { key: 'ATELIERS', label: 'Ateliers' },
    { key: 'REUNIONS', label: 'Reunions' }
  ];

  protected readonly filteredEvents = computed(() => {
    const data = this.activitiesData();
    if (!data) {
      return [];
    }

    return data.events.filter((event) => this.matchesFilter(event, this.selectedFilter()));
  });

  protected readonly selectedEvent = computed(() => {
    const selectedId = this.selectedEventId();
    if (selectedId == null) {
      return null;
    }

    return this.filteredEvents().find((event) => event.id === selectedId) ?? null;
  });

  protected readonly classSummary = computed(() => this.buildClassSummary(this.activitiesData()?.children ?? []));

  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly nextSevenDays = computed(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);

    return Array.from({ length: 7 }, (_, index) => {
      const day = new Date(start);
      day.setDate(start.getDate() + index);

      return {
        label: new Intl.DateTimeFormat('fr-FR', { day: 'numeric' }).format(day),
        weekday: new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(day),
        selected: this.isSameDay(day, this.selectedEvent()?.startDatetime ?? null),
        hasEvent: (this.activitiesData()?.events ?? []).some((event) => this.isSameDay(day, event.startDatetime))
      };
    });
  });

  public constructor() {
    this.activitiesService.activities$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.activitiesData.set(data);
          this.loading.set(false);
          this.errorMessage.set('');

          const currentSelectedId = this.selectedEventId();
          const nextSelectedId =
            currentSelectedId != null && data.events.some((event) => event.id === currentSelectedId)
              ? currentSelectedId
              : null;

          this.selectedEventId.set(nextSelectedId);
          this.syncSelectedChild();
        },
        error: (error: HttpErrorResponse | Error) => {
          this.loading.set(false);
          this.errorMessage.set(this.getErrorMessage(error, "Impossible de charger les activites du parent."));
        }
      });
  }

  protected selectFilter(filter: ActivityFilter): void {
    this.selectedFilter.set(filter);
    this.selectedEventId.set(null);
    this.resetParticipationDraft();
  }

  protected showDetails(event: DecoratedParentEvent): void {
    this.selectedEventId.set(event.id);
    this.successMessage.set('');
    this.errorMessage.set('');
    this.syncSelectedChild();
  }

  protected triggerPrimaryAction(event: DecoratedParentEvent): void {
    if (event.cardState === 'registered') {
      this.showDetails(event);
      return;
    }

    if (!event.canParticipate) {
      this.showDetails(event);
      return;
    }

    if (event.availableChildren.length === 1 && !event.requiresAuthorization) {
      this.selectedEventId.set(event.id);
      this.selectedChildId.set(event.availableChildren[0].id);
      this.participate(event);
      return;
    }

    this.showDetails(event);
  }

  protected resetSelection(): void {
    this.selectedEventId.set(null);
    this.resetParticipationDraft();
  }

  protected onChildSelectionChange(value: string | number): void {
    const childId = Number(value);
    this.selectedChildId.set(Number.isNaN(childId) ? null : childId);
  }

  protected onNotesChange(value: string): void {
    this.parentNotes.set(value);
  }

  protected onAuthorizationFileChange(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    this.selectedAuthorizationFile.set(input?.files?.item(0) ?? null);
  }

  protected participate(event: DecoratedParentEvent): void {
    const childId = this.selectedChildId();

    if (childId == null) {
      this.errorMessage.set("Veuillez selectionner l'enfant concerne.");
      return;
    }

    if (event.requiresAuthorization && !this.selectedAuthorizationFile()) {
      this.errorMessage.set("Une autorisation parentale au format PDF est requise pour cet evenement.");
      return;
    }

    this.isSubmitting.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.activitiesService
      .participate(event.id, childId, this.parentNotes(), this.selectedAuthorizationFile())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.successMessage.set('La participation a ete enregistree avec succes.');
          this.resetParticipationDraft();
        },
        error: (error: HttpErrorResponse | Error) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(this.getErrorMessage(error, "La participation n'a pas pu etre enregistree."));
        }
      });
  }

  protected cancelParticipation(participation: ParentParticipation): void {
    this.cancellingParticipationId.set(participation.id);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.activitiesService
      .cancelParticipation(participation.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.cancellingParticipationId.set(null);
          this.successMessage.set('La participation a ete annulee.');
        },
        error: (error: HttpErrorResponse | Error) => {
          this.cancellingParticipationId.set(null);
          this.errorMessage.set(this.getErrorMessage(error, "L'annulation n'a pas pu etre effectuee."));
        }
      });
  }

  protected submitRating(event: DecoratedParentEvent, stars: number): void {
    if (!event.canRate || !event.ratingChildId) {
      return;
    }

    this.ratingLoadingEventId = event.id;
    this.errorMessage.set('');
    this.successMessage.set('');

    this.activitiesService
      .rateEvent(event.id, event.ratingChildId, stars)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
       next: () => {
  //  mettre à jour la note locale
  event.myRating = stars;

  // mise à jour moyenne + compteur (UX instantanée)
  if (!event.ratingCount || event.ratingCount === 0) {
    event.ratingCount = 1;
    event.averageRating = stars;
  } else {
    event.averageRating = stars; // simple refresh visuel
  }

  this.ratingLoadingEventId = null;
  this.successMessage.set('Votre note a ete enregistree.');
},
        error: (error: HttpErrorResponse | Error) => {
          this.ratingLoadingEventId = null;
          this.errorMessage.set(this.getErrorMessage(error, "La note n'a pas pu etre enregistree."));
        }
      });
  }

  protected isCancelling(participationId: number): boolean {
    return this.cancellingParticipationId() === participationId;
  }

  protected isStarFilled(event: DecoratedParentEvent, star: number): boolean {
    const hovered = this.hoveredStars[event.id];
    const current = hovered ?? event.myRating ?? 0;
    return star <= current;
  }

  protected setHoveredStars(eventId: number, stars: number): void {
    this.hoveredStars[eventId] = stars;
  }

  protected clearHoveredStars(eventId: number): void {
    delete this.hoveredStars[eventId];
  }

  protected openDirections(event: DecoratedParentEvent): void {
    const destination =
      event.latitude != null && event.longitude != null
        ? `${event.latitude},${event.longitude}`
        : encodeURIComponent(event.location || event.title);

    window.open(`https://www.google.com/maps/dir/?api=1&destination=${destination}`, '_blank', 'noopener');
  }

  protected getPhotoUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }

    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    return path.startsWith('/') ? `http://localhost:8081${path}` : `http://localhost:8081/${path}`;
  }

  protected getTypeLabel(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'SORTIE':
        return 'Sortie scolaire';
      case 'FETE':
        return 'Fete';
      case 'ATELIER':
        return 'Atelier';
      case 'REUNION_PARENTS':
        return 'Reunion';
      case 'ACTIVITE':
        return 'Activite';
      default:
        return 'Activite';
    }
  }

  protected getCardStateBadge(event: DecoratedParentEvent): string {
    if (event.cardState === 'registered') {
      return 'Deja inscrit';
    }

    if (event.cardState === 'full') {
      if (event.status === 'CANCELLED') {
        return 'Annule';
      }

      if (event.hasStarted || event.status === 'COMPLETED') {
        return 'Cloture';
      }

      return 'Complet';
    }

    if (event.remainingCapacity == null) {
      return 'Places dispo.';
    }

    return `${event.remainingCapacity} places`;
  }

  protected getCardStateClass(event: DecoratedParentEvent): string {
    return `card-state--${event.cardState}`;
  }

  protected getPrimaryActionIcon(event: DecoratedParentEvent): string {
    return event.cardState === 'registered' ? '✓' : '+';
  }

  protected getDescriptionPreview(description: string): string {
    return description || 'Description a venir pour cet evenement.';
  }

  protected getHeroInitials(): string {
    const children = this.activitiesData()?.children ?? [];
    if (!children.length) {
      return 'PT';
    }

    return children
      .slice(0, 2)
      .map((child) => child.firstName.charAt(0).toUpperCase())
      .join('');
  }

  protected getSelectedParticipations(): ParentParticipation[] {
    return this.selectedEvent()?.activeParticipations ?? [];
  }

  protected getRegisteredChildrenMessage(): string {
    const names = this.getSelectedParticipations().map((participation) => participation.childFullName);

    if (!names.length) {
      return '';
    }

    if (names.length === 1) {
      return `${names[0]} est inscrit a cet evenement.`;
    }

    return `${names.join(', ')} sont inscrits a cet evenement.`;
  }

  protected hasSelectedParticipations(): boolean {
    return this.getSelectedParticipations().length > 0;
  }

  protected getSelectedChildOptions(): ParentChild[] {
    return this.selectedEvent()?.availableChildren ?? [];
  }

  protected hasAvailableChildrenToRegister(): boolean {
    return this.getSelectedChildOptions().length > 0;
  }

  protected areAllParticipationsLocked(): boolean {
    const participations = this.getSelectedParticipations();
    return participations.length > 0 && participations.every((participation) => !participation.cancellableByParent);
  }

  protected getSelectedFileName(): string {
    return this.selectedAuthorizationFile()?.name ?? '';
  }

  protected formatDateRange(event: DecoratedParentEvent): string {
    if (!event.startDatetime) {
      return 'Date a confirmer';
    }

    const start = new Date(event.startDatetime);
    const end = event.endDatetime ? new Date(event.endDatetime) : null;
    const day = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' }).format(start);
    const startTime = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(start);
    const endTime = end ? new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(end) : '';

    return endTime ? `${day} - ${startTime}-${endTime}` : `${day} - ${startTime}`;
  }

  protected formatLongDate(value: string | null): string {
    if (!value) {
      return 'Date a confirmer';
    }

    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(new Date(value));
  }

  protected formatTimeRange(event: DecoratedParentEvent): string {
    if (!event.startDatetime) {
      return 'Horaire a confirmer';
    }

    const start = new Date(event.startDatetime);
    const end = event.endDatetime ? new Date(event.endDatetime) : null;
    const startTime = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(start);
    const endTime = end ? new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit' }).format(end) : '';

    return endTime ? `${startTime} a ${endTime}` : startTime;
  }

  protected getUnavailableMessage(event: DecoratedParentEvent): string {
    if (event.status === 'CANCELLED') {
      return 'Cet evenement est annule.';
    }

    if (event.hasStarted || event.status === 'COMPLETED') {
      return 'La date de debut est passee.';
    }

    if (event.remainingCapacity === 0 || event.full) {
      return "Il n'y a plus de places disponibles.";
    }

    if (!event.availableChildren.length) {
      return 'Tous les enfants eligibles sont deja inscrits.';
    }

    return "L'inscription n'est pas disponible pour cet evenement.";
  }

  protected isCompletedEvent(event: DecoratedParentEvent): boolean {
    return event.status === 'COMPLETED';
  }

  protected hasAttendedParticipation(event: DecoratedParentEvent): boolean {
    return event.activeParticipations.some((participation) => participation.status === 'ATTENDED');
  }

  protected shouldShowCompletedBadge(event: DecoratedParentEvent): boolean {
    return this.isCompletedEvent(event);
  }

  protected shouldShowAttendedBadge(event: DecoratedParentEvent): boolean {
    return this.isCompletedEvent(event) && this.hasAttendedParticipation(event);
  }

  private matchesFilter(event: DecoratedParentEvent, filter: ActivityFilter): boolean {
    const type = (event.type || '').toUpperCase();

    switch (filter) {
      case 'SORTIES':
        return type === 'SORTIE';
      case 'FETES':
        return type === 'FETE';
      case 'ATELIERS':
        return type === 'ATELIER' || type === 'ACTIVITE';
      case 'REUNIONS':
        return type === 'REUNION_PARENTS';
      default:
        return true;
    }
  }

  private syncSelectedChild(): void {
    const availableChildren = this.getSelectedChildOptions();
    const current = this.selectedChildId();

    if (current != null && availableChildren.some((child) => child.id === current)) {
      return;
    }

    this.selectedChildId.set(availableChildren[0]?.id ?? null);
  }

  private resetParticipationDraft(): void {
    this.parentNotes.set('');
    this.selectedAuthorizationFile.set(null);
    this.syncSelectedChild();
  }

  private buildClassSummary(children: ParentChild[]): string {
    if (!children.length) {
      return 'Aucun enfant relie a ce compte';
    }

    const byClassroom = new Map<string, string[]>();

    for (const child of children) {
      const label = child.classroomName || 'Classe';
      const current = byClassroom.get(label) ?? [];
      current.push(child.firstName);
      byClassroom.set(label, current);
    }

    return Array.from(byClassroom.entries())
      .map(([classroom, names]) => `${classroom} - ${names.join(', ')}`)
      .join(' | ');
  }

  private isSameDay(day: Date, value: string | null): boolean {
    if (!value) {
      return false;
    }

    const date = new Date(value);

    return (
      day.getFullYear() === date.getFullYear() &&
      day.getMonth() === date.getMonth() &&
      day.getDate() === date.getDate()
    );
  }

  private getErrorMessage(error: HttpErrorResponse | Error, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim()) {
        return error.error;
      }

      if (error.error?.message) {
        return error.error.message;
      }
    }

    return error.message || fallback;
  }
}