import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { environment } from 'src/environments/environment';
import { Event } from '../models/event.model';
import { EventRegistration } from '../models/event-registration.model';
import { EventRequest } from '../models/event-request.model';

interface EventApiResponse {
  id: number;
  title?: string | null;
  description?: string | null;
  type?: string | null;
  status?: string | null;
  startDatetime?: string | null;
  endDatetime?: string | null;
  location?: string | null;
  latitude?: number | null;
  longitude?: number | null;

  maxCapacity?: number | null;
  requiresAuthorization?: boolean | null;
  classroomId?: number | null;
  classroomName?: string | null;
  targetClassroomIds?: number[] | null;
  targetedClassroomNames?: string[] | null;
  createdBy?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  eventPrice?: number | null;
  photoEvent?: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api`;
  private readonly eventsRefreshSubject = new BehaviorSubject<void>(void 0);

  readonly events$ = this.eventsRefreshSubject.pipe(
    switchMap(() =>
      this.http
        .get<EventApiResponse[]>(`${this.apiUrl}/events`)
        .pipe(map((events) => events.map((event) => this.mapEventResponse(event))))
    ),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  getAllEvents(): Observable<Event[]> {
    return this.http
      .get<EventApiResponse[]>(`${this.apiUrl}/events`)
      .pipe(map((events) => events.map((event) => this.mapEventResponse(event))));
  }

  refreshEvents(): void {
    this.eventsRefreshSubject.next();
  }

  getEventById(id: number): Observable<Event> {
    return this.http
      .get<EventApiResponse>(`${this.apiUrl}/events/${id}`)
      .pipe(map((event) => this.mapEventResponse(event)));
  }

  createEvent(payload: EventRequest): Observable<Event> {
    return this.http
      .post<EventApiResponse>(`${this.apiUrl}/events`, payload)
      .pipe(map((event) => this.mapEventResponse(event)))
      .pipe(tap(() => this.refreshEvents()));
  }

  updateEvent(id: number, payload: EventRequest): Observable<Event> {
    return this.http
      .put<EventApiResponse>(`${this.apiUrl}/events/${id}`, payload)
      .pipe(map((event) => this.mapEventResponse(event)))
      .pipe(tap(() => this.refreshEvents()));
  }

  uploadEventPhoto(id: number, file: File): Observable<Event> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.http
      .post<EventApiResponse>(`${this.apiUrl}/events/${id}/photo`, formData)
      .pipe(map((event) => this.mapEventResponse(event)))
      .pipe(tap(() => this.refreshEvents()));
  }

  publishEvent(id: number): Observable<Event> {
    return this.http
      .put<EventApiResponse>(`${this.apiUrl}/events/${id}/publish`, {})
      .pipe(map((event) => this.mapEventResponse(event)))
      .pipe(tap(() => this.refreshEvents()));
  }

  deleteEvent(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/events/${id}`)
      .pipe(tap(() => this.refreshEvents()));
  }

  createRegistration(
    eventId: number,
    payload: Partial<EventRegistration>
  ): Observable<EventRegistration> {
    return this.http.post<EventRegistration>(
      `${this.apiUrl}/events/${eventId}/registrations`,
      payload
    );
  }

  getRegistrationsByEventId(eventId: number): Observable<EventRegistration[]> {
    return this.http.get<EventRegistration[]>(
      `${this.apiUrl}/events/${eventId}/registrations`
    );
  }

  confirmRegistration(id: number): Observable<EventRegistration> {
    return this.http.put<EventRegistration>(
      `${this.apiUrl}/registrations/${id}/confirm`,
      {}
    );
  }

  cancelRegistration(id: number): Observable<EventRegistration> {
    return this.http.put<EventRegistration>(
      `${this.apiUrl}/registrations/${id}/cancel`,
      {}
    );
  }

  markRegistrationAttended(id: number): Observable<EventRegistration> {
    return this.http.put<EventRegistration>(
      `${this.apiUrl}/registrations/${id}/attended`,
      {}
    );
  }

  markRegistrationAbsent(id: number): Observable<EventRegistration> {
    return this.http.put<EventRegistration>(
      `${this.apiUrl}/registrations/${id}/absent`,
      {}
    );
  }

  private mapEventResponse(event: EventApiResponse): Event {
    return {
      id: event.id,
      title: event.title ?? '',
      description: event.description ?? '',
      type: event.type ?? '',
      status: event.status ?? 'DRAFT',
      startDatetime: event.startDatetime ?? '',
      endDatetime: event.endDatetime ?? '',
      location: event.location ?? '',
      latitude: event.latitude ?? null,
      longitude: event.longitude ?? null,
      maxCapacity: event.maxCapacity ?? 0,
      requiresAuthorization: Boolean(event.requiresAuthorization),
      classroomId: event.classroomId ?? 0,
      classroomName: event.classroomName ?? undefined,
      targetClassroomIds: event.targetClassroomIds ?? [],
      targetedClassroomNames: event.targetedClassroomNames ?? [],
      createdBy: event.createdBy ?? 0,
      createdAt: event.createdAt ?? undefined,
      updatedAt: event.updatedAt ?? undefined,
      eventPrice: event.eventPrice ?? 0,
      photoEvent: event.photoEvent ?? undefined
    };
  }
getAiRecommendations(payload: any) {
  return this.http.post<any[]>(`${this.apiUrl}/events/ai/recommend`, payload);
}
}
