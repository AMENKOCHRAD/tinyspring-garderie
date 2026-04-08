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
  status: string;
  startDatetime: string;
  endDatetime: string;
  location: string;
  eventPrice: number | null;
  classroomId: number | null;
  classroomName: string | null;
  photoEvent: string | null;
  requiresAuthorization: boolean;
  remainingCapacity: number | null;
  maxCapacity: number | null;
  full: boolean;
  registrationOpen: boolean;
  targetedClassroomIds: number[];
  targetedClassroomNames: string[];
  eligibleChildren: ParentChild[];
  availableChildren: ParentChild[];
  activeParticipations: ParentParticipation[];
  hasParticipation: boolean;
}

export interface ParentParticipation {
  registrationId: number;
  eventId: number;
  eventTitle: string;
  childId: number;
  childFullName: string;
  status: string;
  registeredAt: string;
  eventStartDatetime: string;
  cancellableByParent: boolean;
  authorizationDocUrl?: string | null;
}

export interface ParentParticipationRequest {
  childId: number;
  notes?: string | null;
  authorizationFile?: File | null;
}
