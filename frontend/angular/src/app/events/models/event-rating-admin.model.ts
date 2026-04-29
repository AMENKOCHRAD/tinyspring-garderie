export interface EventRatingAdmin {
  id: number;
  eventId: number;
  childId: number;
  childFullName: string;
  stars: number;
  comment?: string | null;
  createdAt?: string;
  updatedAt?: string;
}