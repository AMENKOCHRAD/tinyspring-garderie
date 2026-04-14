export type ParentEventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED' | string;
export type ParentRegistrationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'WAITLISTED'
  | 'CANCELLED'
  | 'ATTENDED'
  | 'ABSENT'
  | string;

export interface ParentChild {
  id: number;
  firstName: string;
  lastName: string;
  classroomId: number;
  classroomName: string;
}

export interface ParentEvent {
  id: number;
  title: string;
  description: string;
  type: string;
  status: ParentEventStatus;
  startDatetime: string | null;
  endDatetime: string | null;
  location: string;
  latitude: number | null;
  longitude: number | null;
  maxCapacity: number | null;
  requiresAuthorization: boolean;
  classroomId: number | null;
  classroomName?: string | null;
  targetClassroomIds: number[];
  targetedClassroomNames: string[];
  eventPrice: number | null;
  photoEvent?: string | null;
  confirmedRegistrations: number;
  waitlistedRegistrations: number;
  remainingCapacity: number | null;
  full: boolean;
  registrationOpen: boolean;
}

export interface ParentParticipation {
  id: number;
  eventId: number;
  eventTitle: string;
  childId: number;
  childFullName: string;
  parentId: number;
  status: ParentRegistrationStatus;
  authorizationSigned: boolean;
  authorizationDocUrl: string | null;
  notes: string | null;
  registeredAt: string | null;
  eventStartDatetime: string | null;
  cancellableByParent: boolean;
}

export type ActivityFilter = 'TOUS' | 'SORTIES' | 'FETES' | 'ATELIERS' | 'REUNIONS';
export type ActivityCardState = 'available' | 'registered' | 'full';

export interface DecoratedParentEvent extends ParentEvent {
  eligibleChildren: ParentChild[];
  activeParticipations: ParentParticipation[];
  availableChildren: ParentChild[];
  cardState: ActivityCardState;
  canParticipate: boolean;
  hasStarted: boolean;
}

export interface ParentActivitiesData {
  parentId: number;
  children: ParentChild[];
  events: DecoratedParentEvent[];
  participations: ParentParticipation[];
}
