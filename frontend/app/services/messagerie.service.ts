import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { Conversation } from '../models/conversation.model';
import { User } from '../models/user.model';

export interface CreateConversationRequest {
  subject: string;
  receiverId: number;
  receiverRole: string;
}

export interface UpdateConversationRequest {
  subject: string;
}

export interface Message {
  id: number;
  content: string;
  sentAt: string;
  isRead: boolean;
  imageName?: string | null;
  imagePath?: string | null;
  sender: User;
}

export interface UpdateMessageRequest {
  content: string;
}

export interface Reclamation {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
  category: string;

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

  adminComment?: string | null;
  createdAt: string;
  updatedAt: string;
  imageName?: string | null;
  imagePath?: string | null;
  attachmentName?: string | null;
  attachmentPath?: string | null;
  attachmentType?: string | null;
  conversation?: any;
}

export interface ReclamationHistory {
  id: number;
  actionType: string;
  actionLabel: string;
  oldValue?: string | null;
  newValue?: string | null;
  actorName: string;
  actorRole: string;
  createdAt: string;
}
export interface RecommendedAdminActionResponse {
  recommendedService: string;
  recommendedUrgency: string;
  recommendedAction: string;
  recommendedDelay: string;
}

@Injectable({
  providedIn: 'root'
})
export class MessagerieService {
  private apiUrl = 'http://localhost:8081/api';
  private backendBaseUrl = 'http://localhost:8081';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getMyConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(
      `${this.apiUrl}/conversations`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  createConversation(data: CreateConversationRequest): Observable<Conversation> {
    return this.http.post<Conversation>(
      `${this.apiUrl}/conversations`,
      data,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  updateConversation(id: number, data: UpdateConversationRequest): Observable<Conversation> {
    return this.http.put<Conversation>(
      `${this.apiUrl}/conversations/${id}`,
      data,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  deleteConversation(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/conversations/${id}`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getUsersByRole(roleName: string): Observable<User[]> {
    return this.http.get<User[]>(
      `${this.apiUrl}/users/by-role/${roleName}`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getMessagesByConversation(conversationId: number): Observable<Message[]> {
    return this.http.get<Message[]>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }
  getRecommendedAdminAction(id: number): Observable<RecommendedAdminActionResponse> {
  return this.http.get<RecommendedAdminActionResponse>(
    `${this.apiUrl}/reclamations/${id}/recommended-admin-action`,
    { headers: this.authService.getBasicAuthHeaders() }
  );
}

  sendMessage(conversationId: number, content: string, image?: File | null): Observable<Message> {
    const formData = new FormData();

    if (content && content.trim()) {
      formData.append('content', content.trim());
    }

    if (image) {
      formData.append('image', image);
    }

    return this.http.post<Message>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      formData,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  updateMessage(messageId: number, data: UpdateMessageRequest): Observable<Message> {
    return this.http.put<Message>(
      `${this.apiUrl}/messages/${messageId}`,
      data,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/messages/${messageId}`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  markConversationMessagesAsRead(conversationId: number): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl}/conversations/${conversationId}/messages/read`,
      {},
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getConversationById(id: number): Observable<Conversation> {
    return this.http.get<Conversation>(
      `${this.apiUrl}/conversations/${id}`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  updateConversationStatus(id: number, status: string): Observable<Conversation> {
    return this.http.put<Conversation>(
      `${this.apiUrl}/conversations/${id}/status`,
      { status },
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getFullImageUrl(path?: string | null): string {
    if (!path) {
      return '';
    }
    return `${this.backendBaseUrl}${path}`;
  }

  downloadFile(path: string): Observable<Blob> {
    const headers: HttpHeaders = this.authService.getBasicAuthHeaders();

    return this.http.get(this.getFullImageUrl(path), {
      headers,
      responseType: 'blob'
    });
  }

  getMyReclamations(): Observable<Reclamation[]> {
    return this.http.get<Reclamation[]>(
      `${this.apiUrl}/reclamations`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getReclamationHistory(reclamationId: number): Observable<ReclamationHistory[]> {
    return this.http.get<ReclamationHistory[]>(
      `${this.apiUrl}/reclamations/${reclamationId}/history`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  exportReclamationHistoryPdf(reclamationId: number): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/reclamations/${reclamationId}/history/export-pdf`,
      {
        headers: this.authService.getBasicAuthHeaders(),
        responseType: 'blob'
      }
    );
  }

  exportReclamationsExcel(): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/reclamations/export-excel`,
      {
        headers: this.authService.getBasicAuthHeaders(),
        responseType: 'blob'
      }
    );
  }

  createReclamation(
    title: string,
    description: string,
    priority?: string,
    category?: string,
    image?: File | null,
    attachment?: File | null
  ): Observable<Reclamation> {
    const formData = new FormData();

    formData.append('title', title.trim());
    formData.append('description', description.trim());

    if (priority && priority.trim()) {
      formData.append('priority', priority.trim());
    }

    if (category && category.trim()) {
      formData.append('category', category.trim());
    }

    if (image) {
      formData.append('image', image);
    }

    if (attachment) {
      formData.append('attachment', attachment);
    }

    return this.http.post<Reclamation>(
      `${this.apiUrl}/reclamations`,
      formData,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  updateReclamation(id: number, data: any): Observable<Reclamation> {
    return this.http.put<Reclamation>(
      `${this.apiUrl}/reclamations/${id}`,
      data,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  deleteReclamation(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/reclamations/${id}`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  updateReclamationStatus(id: number, status: string): Observable<Reclamation> {
    return this.http.put<Reclamation>(
      `${this.apiUrl}/reclamations/${id}/status`,
      { status },
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }

  getSuggestedResponse(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/reclamations/${id}/suggested-response`,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }
}