import { User } from './user.model';

export interface Conversation {
  id: number;
  subject: string;
  type: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  createdBy: User;
  parent: User;
  admin?: User | null;
  animatrice?: User | null;
}