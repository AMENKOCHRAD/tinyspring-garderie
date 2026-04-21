import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin } from 'rxjs';
import { map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../shared/auth.service';
import {
  DecoratedParentEvent,
  ParentActivitiesData,
  ParentChild,
  ParentEvent,
  ParentParticipation
} from './parent-activities.models';

@Injectable({
  providedIn: 'root'
})
export class ParentActivitiesService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = '/api/parent';
  private readonly refreshSubject = new BehaviorSubject<void>(void 0);

  readonly activities$ = this.refreshSubject.pipe(
    switchMap(() => this.loadActivities()),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  refresh(): void {
    this.refreshSubject.next();
  }

  participate(eventId: number, childId: number, notes?: string, authorizationFile?: File | null): Observable<ParentParticipation> {
    const parentId = this.getParentId();
    const formData = new FormData();
    formData.append('parentId', String(parentId));
    formData.append('childId', String(childId));

    if (notes?.trim()) {
      formData.append('notes', notes.trim());
    }

    if (authorizationFile) {
      formData.append('authorizationFile', authorizationFile, authorizationFile.name);
    }

    return this.http
      .post<ParentParticipation>(`${this.apiUrl}/events/${eventId}/participations`, formData)
      .pipe(tap(() => this.refresh()));
  }

  cancelParticipation(registrationId: number): Observable<ParentParticipation> {
    const parentId = this.getParentId();
    const params = new HttpParams().set('parentId', String(parentId));

    return this.http
      .put<ParentParticipation>(`${this.apiUrl}/event-registrations/${registrationId}/cancel`, null, { params })
      .pipe(tap(() => this.refresh()));
  }

  private loadActivities(): Observable<ParentActivitiesData> {
    const parentId = this.getParentId();
    const params = new HttpParams().set('parentId', String(parentId));

    return forkJoin({
      children: this.http.get<ParentChild[]>(`${this.apiUrl}/children`, { params }),
      events: this.http.get<ParentEvent[]>(`${this.apiUrl}/events`, { params }),
      participations: this.http.get<ParentParticipation[]>(`${this.apiUrl}/event-registrations`, { params })
    }).pipe(
      map(({ children, events, participations }) => ({
        parentId,
        children,
        participations,
        events: events
          .map((event) => this.decorateEvent(event, children, participations))
          .sort((left, right) => this.toTime(left.startDatetime) - this.toTime(right.startDatetime))
      }))
    );
  }
rateEvent(eventId: number, childId: number, stars: number, comment?: string) {
  const parentId = this.getParentId();

  return this.http.post(
    `${this.apiUrl}/events/${eventId}/rating?parentId=${parentId}`,
    {
      childId,
      stars,
      comment
    }
  );
}


  private decorateEvent(
    event: ParentEvent,
    children: ParentChild[],
    participations: ParentParticipation[]
  ): DecoratedParentEvent {
    const classroomScope = new Set<number>();

    if (typeof event.classroomId === 'number') {
      classroomScope.add(event.classroomId);
    }
    

    for (const classroomId of event.targetClassroomIds ?? []) {
      classroomScope.add(classroomId);
    }

    const eligibleChildren = children.filter((child) => classroomScope.has(child.classroomId));
    const eligibleChildIds = new Set(eligibleChildren.map((child) => child.id));
    const activeParticipations = participations.filter(
      (participation) =>
        participation.eventId === event.id &&
        participation.status !== 'CANCELLED' &&
        eligibleChildIds.has(participation.childId)
    );
    const registeredChildIds = new Set(activeParticipations.map((participation) => participation.childId));
    const availableChildren = eligibleChildren.filter((child) => !registeredChildIds.has(child.id));
    const hasStarted = event.startDatetime ? new Date(event.startDatetime).getTime() <= Date.now() : false;

    const isClosed =
      hasStarted ||
      event.status === 'CANCELLED' ||
      event.status === 'COMPLETED' ||
      !event.registrationOpen ||
      event.full ||
      event.remainingCapacity === 0 ||
      availableChildren.length === 0;

    let cardState: DecoratedParentEvent['cardState'] = 'available';
    if (activeParticipations.length > 0) {
      cardState = 'registered';
    } else if (isClosed) {
      cardState = 'full';
    }
const attendedParticipations = activeParticipations.filter(
  (participation) => participation.status === 'ATTENDED'
);

const ratingChildId = attendedParticipations.length > 0
  ? attendedParticipations[0].childId
  : null;

    return {
 ...event,
  eligibleChildren,
  activeParticipations,
  availableChildren,
  hasStarted,
  canParticipate:
    !hasStarted &&
    event.status !== 'CANCELLED' &&
    event.status !== 'COMPLETED' &&
    event.registrationOpen &&
    !event.full &&
    (event.remainingCapacity === null || event.remainingCapacity > 0) &&
    availableChildren.length > 0,

  canRate: event.status === 'COMPLETED' && ratingChildId !== null,
  ratingChildId,

  cardState
    };
  }

  private getParentId(): number {
    const rawId = this.authService.getCurrentUser()?.id;
    const parentId = Number(rawId);

    if (!rawId || Number.isNaN(parentId)) {
      throw new Error("Impossible d'identifier le parent connecte.");
    }

    return parentId;
  }

  private toTime(value: string | null): number {
    if (!value) {
      return Number.MAX_SAFE_INTEGER;
    }

    const timestamp = new Date(value).getTime();
    return Number.isNaN(timestamp) ? Number.MAX_SAFE_INTEGER : timestamp;
  }
}
