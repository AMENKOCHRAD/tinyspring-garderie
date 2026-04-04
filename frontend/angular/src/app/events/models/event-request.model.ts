import { EventStatus, EventType } from './event.model';

export interface EventRequest {
  title: string;
  description: string;
  type: EventType;
  status?: EventStatus;
  startDatetime: string;
  endDatetime: string;
  location: string;
  maxCapacity: number;
  requiresAuthorization: boolean;
  classroomId: number;
  createdBy: number;
  eventPrice: number;
  photoPrice: number;
  photoEvent?: string;
}
