import { EventRequest } from './event-request.model';

export interface EventFormSubmission {
  payload: EventRequest;
  photoFile: File | null;
}
