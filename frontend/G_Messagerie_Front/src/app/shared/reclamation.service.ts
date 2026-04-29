import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

export type ReclamationStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type ReclamationPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type ReclamationCategory =
  | 'REPAS'
  | 'TRANSPORT'
  | 'COMPORTEMENT'
  | 'HYGIENE'
  | 'SECURITE'
  | 'PERSONNEL'
  | 'FINANCIER'
  | 'PEDAGOGIQUE'
  | 'ADMINISTRATIF'
  | 'AUTRE';

export interface Reclamation {
  id: number;
  title: string;
  description: string;
  status: ReclamationStatus;
  priority: ReclamationPriority;
  category: ReclamationCategory;
  predictedCategory?: string | null;
  classificationConfidence?: number | null;
  autoClassified?: boolean | null;
  predictedPriority?: string | null;
  priorityConfidence?: number | null;
  decisionRecommendation?: string | null;
  decisionConfidence?: number | null;
  recurring?: boolean | null;
  recurrenceCount?: number | null;
  recurrenceReason?: string | null;
  smartPriorityScore?: number | null;
  smartPriorityLevel?: string | null;
  smartPriorityReason?: string | null;
  autoEscalated?: boolean | null;
  escalatedAt?: string | null;
  escalationReason?: string | null;
  recommendedService?: string | null;
  recommendedDelay?: string | null;
  recommendedAction?: string | null;
  adminComment?: string | null;
  imageName?: string | null;
  imagePath?: string | null;
  attachmentName?: string | null;
  attachmentPath?: string | null;
  attachmentType?: string | null;
  conversation?: Conversation | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface AppUser {
  id: number;
  nom?: string | null;
  email: string;
}

export interface Conversation {
  id: number;
  subject: string;
}

export interface Message {
  id: number;
  content: string;
  sentAt: string;
}

export interface CreateReclamationPayload {
  title: string;
  description: string;
  priority?: ReclamationPriority | '';
  category?: ReclamationCategory | '';
  image?: File | null;
  attachment?: File | null;
}

export interface UpdateReclamationPayload {
  title?: string;
  description?: string;
  priority?: ReclamationPriority | '';
  category?: ReclamationCategory | '';
}

@Injectable({
  providedIn: 'root'
})
export class ReclamationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/reclamations';

  getMine(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(`${this.apiUrl}/my`).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 400 || error.status === 404) {
          return this.http.get<Reclamation[]>(this.apiUrl);
        }

        return throwError(() => error);
      })
    );
  }

  getById(id: number): Observable<Reclamation> {
    return this.http.get<Reclamation>(`${this.apiUrl}/${id}`);
  }

  create(payload: CreateReclamationPayload): Observable<Reclamation> {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('description', payload.description);

    if (payload.priority) {
      formData.append('priority', payload.priority);
    }

    if (payload.category) {
      formData.append('category', payload.category);
    }

    if (payload.image) {
      formData.append('image', payload.image);
    }

    if (payload.attachment) {
      formData.append('attachment', payload.attachment);
    }

    return this.http.post<Reclamation>(this.apiUrl, formData);
  }

  update(id: number, payload: UpdateReclamationPayload): Observable<Reclamation> {
    return this.http.put<Reclamation>(`${this.apiUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getUsersByRole(roleName: 'ADMIN' | 'ANIMATRICE'): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(`/api/users/by-role/${roleName}`);
  }

  createConversation(subject: string, receiverId: number, receiverRole: 'ADMIN' | 'ANIMATRICE'): Observable<Conversation> {
    return this.http.post<Conversation>('/api/conversations', {
      subject,
      receiverId,
      receiverRole
    });
  }

  sendMessage(conversationId: number, content: string): Observable<Message> {
    const formData = new FormData();
    formData.append('content', content);

    return this.http.post<Message>(`/api/conversations/${conversationId}/messages`, formData);
  }

  downloadFile(path: string): Observable<Blob> {
    return this.http.get(path, { responseType: 'blob' });
  }

  getFileUrl(path?: string | null): string | null {
    return path ? path : null;
  }
}
