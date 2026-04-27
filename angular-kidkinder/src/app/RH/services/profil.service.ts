import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AnimatriceProfilData {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  dateEmbauche?: string;
  specialite?: string;
  statut?: string;
  photoUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class ProfilService {

  private apiUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  // ✅ Récupérer le profil par email (au chargement)
  getProfilParEmail(email: string): Observable<AnimatriceProfilData> {
    return this.http.get<AnimatriceProfilData>(
      `${this.apiUrl}/animatrice/profil/par-email?email=${encodeURIComponent(email)}`
    );
  }

  // ✅ Récupérer le profil par ID
  getProfilParId(id: number): Observable<AnimatriceProfilData> {
    return this.http.get<AnimatriceProfilData>(
      `${this.apiUrl}/animatrice/profil/${id}`
    );
  }

  // ✅ Modifier son profil
  updateProfil(id: number, data: AnimatriceProfilData): Observable<AnimatriceProfilData> {
    return this.http.put<AnimatriceProfilData>(
      `${this.apiUrl}/animatrice/profil/${id}`,
      data
    );
  }

  // ✅ Upload photo de profil
  uploadPhoto(id: number, file: File): Observable<AnimatriceProfilData> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<AnimatriceProfilData>(
      `${this.apiUrl}/animatrice/profil/${id}/upload-photo`,
      formData
    );
  }
}