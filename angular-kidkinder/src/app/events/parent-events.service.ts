import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, combineLatest, map } from 'rxjs';
import {
  ParentChild,
  ParentEvent,
  ParentParticipation,
  ParentParticipationRequest
} from './events.models';
import { AuthService } from '../shared/auth.service';

interface ParentChildApiResponse {
  id: number;
  firstName?: string | null;
  lastName?: string | null;
  classroomId?: number | null;
  classroomName?: string | null;
}

interface ParentEventApiResponse {
  id: number;
  title?: string | null;
  description?: string | null;
  type?: string | null;
  status?: string | null;
  startDatetime?: string | null;
  endDatetime?: string | null;
  location?: string | null;
  eventPrice?: number | null;
  classroomId?: number | null;
  classroomName?: string | null;
  photoEvent?: string | null;
  requiresAuthorization?: boolean | null;
  remainingCapacity?: number | null;
  maxCapacity?: number | null;
  full?: boolean | null;
  registrationOpen?: boolean | null;
  targetClassroomIds?: number[] | null;
  targetedClassroomNames?: string[] | null;
}

interface ParentParticipationApiResponse {
  id?: number | null;
  eventId?: number | null;
  eventTitle?: string | null;
  childId?: number | null;
  childFullName?: string | null;
  status?: string | null;
  registeredAt?: string | null;
  eventStartDatetime?: string | null;
  cancellableByParent?: boolean | null;
  authorizationDocUrl?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ParentEventsService {
  private readonly apiBaseUrl = 'http://localhost:8081/api/parent';
  private readonly backendBaseUrl = 'http://localhost:8081';

  constructor(
    private readonly http: HttpClient,
    private readonly authService: AuthService
  ) {}

  public getMyChildren(): Observable<ParentChild[]> {
    return this.http
      .get<ParentChildApiResponse[]>(`${this.apiBaseUrl}/children`, {
        params: this.getParentParams()
      })
      .pipe(map((children) => children.map((child) => this.mapChild(child))));
  }

  public getParentEvents(): Observable<ParentEvent[]> {
    return combineLatest([this.getMyChildren(), this.getParentEventsRaw()]).pipe(
      map(([children, events]) => events.map((event) => this.attachEligibleChildren(event, children)))
    );
  }

  public getParticipations(): Observable<ParentParticipation[]> {
    return this.http
      .get<ParentParticipationApiResponse[]>(`${this.apiBaseUrl}/event-registrations`, {
        params: this.getParentParams()
      })
      .pipe(map((rows) => rows.map((row) => this.mapParticipation(row))));
  }

  public participateToEvent(eventId: number, payload: ParentParticipationRequest): Observable<ParentParticipationApiResponse> {
    const formData = new FormData();
    formData.append('parentId', String(this.getParentIdOrThrow()));
    formData.append('childId', String(payload.childId));

    if (payload.notes?.trim()) {
      formData.append('notes', payload.notes.trim());
    }

    if (payload.authorizationFile) {
      formData.append('authorizationFile', payload.authorizationFile, payload.authorizationFile.name);
    }

    return this.http.post<ParentParticipationApiResponse>(
      `${this.apiBaseUrl}/events/${eventId}/participations`,
      formData
    );
  }

  public cancelParticipation(registrationId: number): Observable<ParentParticipationApiResponse> {
    return this.http.put<ParentParticipationApiResponse>(
      `${this.apiBaseUrl}/event-registrations/${registrationId}/cancel`,
      null,
      { params: this.getParentParams() }
    );
  }

  private getParentEventsRaw(): Observable<ParentEvent[]> {
    return this.http
      .get<ParentEventApiResponse[]>(`${this.apiBaseUrl}/events`, {
        params: this.getParentParams()
      })
      .pipe(map((events) => events.map((event) => this.mapEvent(event))));
  }

  private getParentParams(): HttpParams {
    return new HttpParams().set('parentId', String(this.getParentIdOrThrow()));
  }

  private getParentIdOrThrow(): number {
    const parentId = this.authService.getCurrentUser()?.id;
    if (!parentId) {
      throw new Error('Aucun parent connecte. Merci de vous reconnecter.');
    }

    return parentId;
  }

  private mapChild(child: ParentChildApiResponse): ParentChild {
    return {
      id: child.id,
      firstName: child.firstName ?? '',
      lastName: child.lastName ?? '',
      classroomId: child.classroomId ?? 0,
      classroomName: child.classroomName ?? 'Classe non definie'
    };
  }

  private mapEvent(event: ParentEventApiResponse): ParentEvent {
    return {
      id: event.id,
      title: event.title ?? 'Evenement',
      description: event.description ?? '',
      type: event.type ?? 'Evenement',
      status: event.status ?? 'PUBLISHED',
      startDatetime: event.startDatetime ?? '',
      endDatetime: event.endDatetime ?? '',
      location: event.location ?? 'Lieu a confirmer',
      eventPrice: event.eventPrice ?? null,
      classroomId: event.classroomId ?? null,
      classroomName: event.classroomName ?? null,
      photoEvent: this.resolveFileUrl(event.photoEvent),
      requiresAuthorization: Boolean(event.requiresAuthorization),
      remainingCapacity: event.remainingCapacity ?? null,
      maxCapacity: event.maxCapacity ?? null,
      full: Boolean(event.full),
      registrationOpen: event.registrationOpen ?? !Boolean(event.full),
      targetedClassroomIds: event.targetClassroomIds ?? [],
      targetedClassroomNames: event.targetedClassroomNames ?? [],
      eligibleChildren: [],
      availableChildren: [],
      activeParticipations: [],
      hasParticipation: false
    };
  }

  private attachEligibleChildren(event: ParentEvent, children: ParentChild[]): ParentEvent {
    const classroomScope = [
      ...(event.classroomId !== null ? [event.classroomId] : []),
      ...event.targetedClassroomIds
    ].filter((value, index, array) => value !== null && array.indexOf(value) === index);

    return {
      ...event,
      eligibleChildren: children.filter((child) => classroomScope.includes(child.classroomId))
    };
  }

  private mapParticipation(row: ParentParticipationApiResponse): ParentParticipation {
    return {
      registrationId: row.id ?? 0,
      eventId: row.eventId ?? 0,
      eventTitle: row.eventTitle ?? 'Evenement',
      childId: row.childId ?? 0,
      childFullName: row.childFullName ?? 'Enfant',
      status: row.status ?? 'PENDING',
      registeredAt: row.registeredAt ?? '',
      eventStartDatetime: row.eventStartDatetime ?? '',
      cancellableByParent: Boolean(row.cancellableByParent),
      authorizationDocUrl: this.resolveFileUrl(row.authorizationDocUrl)
    };
  }

  private resolveFileUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }

    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    if (path.startsWith('/')) {
      return `${this.backendBaseUrl}${path}`;
    }

    return `${this.backendBaseUrl}/${path}`;
  }
}
