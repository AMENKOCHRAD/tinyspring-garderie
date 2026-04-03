import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { Conversation } from '../models/conversation.model';
import { User } from '../models/user.model';

export interface CreateConversationRequest {
  subject: string;
  receiverId: number;
  receiverRole: string;
}

export interface Message {
  id: number;
  content: string;
  sentAt: string;
  isRead: boolean;
  sender: User;
}

export interface SendMessageRequest {
  content: string;
}

@Injectable({
  providedIn: 'root'
})
export class MessagerieService {
  private apiUrl = 'http://localhost:8081/api';

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

  sendMessage(conversationId: number, data: SendMessageRequest): Observable<Message> {
    return this.http.post<Message>(
      `${this.apiUrl}/conversations/${conversationId}/messages`,
      data,
      { headers: this.authService.getBasicAuthHeaders() }
    );
  }
}