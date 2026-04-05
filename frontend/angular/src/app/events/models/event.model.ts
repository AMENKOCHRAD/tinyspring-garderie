export type EventType = string;

export type EventStatus = string;

export interface Event {
  id: number;
  title: string;
  description: string;
  type: EventType;
  status: EventStatus;
  startDatetime: string;
  endDatetime: string;
  location: string;
  maxCapacity: number;
  requiresAuthorization: boolean;
  classroomId: number;
  createdBy: number;
  createdAt?: string;
  updatedAt?: string;
  eventPrice: number;
  photoEvent?: string;
}
