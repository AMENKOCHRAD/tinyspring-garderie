import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiResponse {
  response: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private apiUrl = 'http://localhost:8081/api/ai';

  constructor(private http: HttpClient) {}

  generateDailyReport(childName: string, animatorNotes: string): Observable<AiResponse> {
    return this.http.post<AiResponse>(`${this.apiUrl}/report/generate`, {
      childName,
      animatorNotes
    });
  }

  suggestActivities(ageMin: number, ageMax: number, capacite: number): Observable<AiResponse> {
    return this.http.post<AiResponse>(`${this.apiUrl}/activities/suggest`, {
      ageMin,
      ageMax,
      capacite
    });
  }

  // --- ML Avancé : K-Means Clustering ---
  clusterChildren(children: any[], numGroups: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/ml/cluster-children`, {
      children,
      numGroups
    });
  }

  // --- ML Avancé : Decision Tree ---
  recommendRoom(capacite: number, ageMoyen: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/ml/recommend-room`, {
      capacite,
      ageMoyen
    });
  }
}

