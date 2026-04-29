import { EventStatus, EventType } from './event.model';

export interface EventRequest {
  title: string;
  description: string;
  type: EventType;
  status?: EventStatus;
  startDatetime: string;
  endDatetime: string;
  location: string;
  latitude?: number | null;
  longitude?: number | null;
  maxCapacity: number;
  requiresAuthorization: boolean;
  classroomId: number;
  targetClassroomIds?: number[];
  createdBy: number;
  eventPrice: number;
  photoEvent?: string;
}
