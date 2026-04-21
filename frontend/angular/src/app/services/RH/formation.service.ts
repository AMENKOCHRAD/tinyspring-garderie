import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Formation {
  id?: number;
  titre: string;
  description?: string;
  type: string;
  formateur: string;
  lieu?: string;
  dateFormation?: string;    // ✅ Date unique
  heureDebut?: string;       // ✅ Heure début
  heureFin?: string;         // ✅ Heure fin
  placesMax?: number;
  dureeValiditeMois?: number;
  obligatoire?: boolean;
  statut?: string;
  inscriptions?: any[];
  nbInscrits?: number;
  placesDisponibles?: number;
  complet?: boolean;
}

export interface AnimatriceFormation {
  id?: number;
  animatrice?: any;
  formation?: Formation;
  dateInscription?: string;
  dateCompletion?: string;
  dateExpiration?: string;
  statut?: string;
  statutValidite?: string;
  certificationGeneree?: boolean;
  commentaire?: string;
}

@Injectable({ providedIn: 'root' })
export class FormationService {

  private apiUrl = 'http://localhost:8081/api/admin/formations';

  constructor(private http: HttpClient) {}

  getToutesFormations(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.apiUrl);
  }

  getFormationById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.apiUrl}/${id}`);
  }

  creerFormation(formation: Formation): Observable<Formation> {
    return this.http.post<Formation>(this.apiUrl, formation);
  }

  modifierFormation(id: number, formation: Formation): Observable<Formation> {
    return this.http.put<Formation>(`${this.apiUrl}/${id}`, formation);
  }

  supprimerFormation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  demarrerFormation(id: number): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/${id}/demarrer`, {});
  }

  terminerFormation(id: number): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/${id}/terminer`, {});
  }

  annulerFormation(id: number, motif: string): Observable<Formation> {
    return this.http.post<Formation>(`${this.apiUrl}/${id}/annuler`, { motif });
  }

  getInscriptions(id: number): Observable<AnimatriceFormation[]> {
    return this.http.get<AnimatriceFormation[]>(`${this.apiUrl}/${id}/inscriptions`);
  }

  inscrireAnimatrice(formationId: number, animatriceId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${formationId}/inscrire/${animatriceId}`, {});
  }

  desinscrireAnimatrice(formationId: number, animatriceId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${formationId}/desinscrire/${animatriceId}`);
  }

  getProfilFormations(animatriceId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/animatrices/${animatriceId}/profil`);
  }

  getSuggestions(animatriceId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/animatrices/${animatriceId}/suggestions`);
  }

  getAlertesGlobales(): Observable<any> {
    return this.http.get(`${this.apiUrl}/alertes`);
  }

  getStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/stats`);
  }
}