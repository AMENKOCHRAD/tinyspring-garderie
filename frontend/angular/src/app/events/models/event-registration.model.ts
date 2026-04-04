export type RegistrationStatus = string;

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
