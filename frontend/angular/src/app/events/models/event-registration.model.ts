export type RegistrationStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'WAITLISTED'
  | 'ATTENDED'
  | 'ABSENT'
  | string;

export interface EventRegistration {
  id: number;
  childId: number;
  parentId: number;
  status: RegistrationStatus;
  authorizationSigned: boolean;
  authorizationDocUrl?: string | null;
  notes?: string | null;
  registeredAt: string;
}
