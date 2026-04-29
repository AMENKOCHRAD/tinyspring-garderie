import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AbsenceConge } from '../models/absence-conge.model';

export interface AnimatriceInfo {
  id: number;
  nom: string;
  prenom: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AbsenceCongeService {

  // ✅ URL relative — le proxy redirige vers localhost:8081
  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  getMonProfil(email: string): Observable<AnimatriceInfo> {
    return this.http.get<AnimatriceInfo>(
      `${this.apiUrl}/animatrice/profil/par-email?email=${encodeURIComponent(email)}`
    );
  }

  getMesAbsenceConges(animatriceId: number): Observable<AbsenceConge[]> {
    return this.http.get<AbsenceConge[]>(
      `${this.apiUrl}/animatrice/absences-conges/${animatriceId}`
    );
  }

  soumettreDemande(demande: AbsenceConge): Observable<AbsenceConge> {
    return this.http.post<AbsenceConge>(
      `${this.apiUrl}/animatrice/absences-conges`,
      demande
    );
  }
}