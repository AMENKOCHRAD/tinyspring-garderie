import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../shared/auth.service';

export interface EnfantDTO {
  nom: string;
  prenom: string;
  dateNaissance: string;
  groupeSanguin?: string;
  allergies?: string;
  contactUrgence?: string;
  photo?: string;
  parentId: number;
}

export interface ParentDTO {
  id: number;
  nom: string;
  email: string;
}

export interface Enfant {
  id: number;
  nom: string;
  prenom: string;
  dateNaissance: string;
  groupeSanguin?: string;
  allergies?: string;
  contactUrgence?: string;
  photo?: string;
  archive?: boolean;
  parent?: ParentDTO;
}

@Injectable({
  providedIn: 'root'
})
export class EnfantService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = '/api/enfants';

  private getAuthHeadersStrict(): { headers: HttpHeaders } {
    const token = this.authService.getToken();

    if (!token) {
      throw new Error('Non authentifie (token manquant ou invalide).');
    }

    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }

  private getAuthHeaders(): { headers?: HttpHeaders } {
    const token = this.authService.getToken();

    if (!token) {
      return {};
    }

    return {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`
      })
    };
  }
  getEnfantsParParent(parentId: number): Observable<Enfant[]> {
    return this.http.get<Enfant[]>(
      `${this.apiUrl}/parent/${parentId}`,
      this.getAuthHeaders()
    );
  }

  ajouterEnfant(dto: EnfantDTO): Observable<Enfant> {
    return this.http.post<Enfant>(
      this.apiUrl,
      dto,
      this.getAuthHeaders()
    );
  }

  getEnfantById(id: number): Observable<Enfant> {
    return this.http.get<Enfant>(
      `${this.apiUrl}/${id}`,
      this.getAuthHeaders()
    );
  }

  modifierEnfant(id: number, dto: EnfantDTO): Observable<Enfant> {
    return this.http.put<Enfant>(
      `${this.apiUrl}/${id}`,
      dto,
      this.getAuthHeaders()
    );
  }

  getConditionsParEnfant(enfantId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `/api/conditions/enfant/${enfantId}`,
      this.getAuthHeaders()
    );
  }

  getAllEnfants(): Observable<Enfant[]> {
    return this.http.get<Enfant[]>(this.apiUrl, this.getAuthHeaders());
  }

  ajouterConditionSanitaire(enfantId: number, payload: any): Observable<any> {
    return this.http.post<any>(
      `/api/conditions/enfant/${enfantId}`,
      payload,
      this.getAuthHeaders()
    );
  }

  getTraitementsParCondition(conditionId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `/api/traitements/condition/${conditionId}`,
      this.getAuthHeaders()
    );
  }

  getPrisesTraitementParEnfant(enfantId: number, dateIso?: string): Observable<any[]> {
    const qs = dateIso ? `?date=${encodeURIComponent(dateIso)}` : '';
    return this.http.get<any[]>(
      `/api/animatrice/sante/enfant/${enfantId}/prises${qs}`,
      this.getAuthHeadersStrict()
    );
  }

  getPrisesTraitementParEnfantPeriode(enfantId: number, fromIso: string, toIso: string): Observable<any[]> {
    const qs = `?from=${encodeURIComponent(fromIso)}&to=${encodeURIComponent(toIso)}`;
    return this.http.get<any[]>(
      `/api/animatrice/sante/enfant/${enfantId}/prises${qs}`,
      this.getAuthHeadersStrict()
    );
  }

  getToutesPrisesTraitements(dateIso?: string): Observable<any[]> {
    const qs = dateIso ? `?date=${encodeURIComponent(dateIso)}` : '';
    return this.http.get<any[]>(
      `/api/animatrice/sante/prises${qs}`,
      this.getAuthHeadersStrict()
    );
  }

  getMesPrisesTraitements(fromIso: string, toIso: string): Observable<any[]> {
    const qs = `?from=${encodeURIComponent(fromIso)}&to=${encodeURIComponent(toIso)}`;
    return this.http.get<any[]>(
      `/api/animatrice/sante/mes-prises${qs}`,
      this.getAuthHeadersStrict()
    );
  }

  enregistrerPriseTraitement(traitementId: number, payload: any): Observable<any> {
    return this.http.post<any>(
      `/api/animatrice/sante/traitements/${traitementId}/prises`,
      payload,
      this.getAuthHeadersStrict()
    );
  }

  creerObservationEnfant(enfantId: number, payload: any): Observable<any> {
    return this.http.post<any>(
      `/api/animatrice/sante/enfant/${enfantId}/observations`,
      payload,
      this.getAuthHeadersStrict()
    );
  }

  listerDernieresObservations(): Observable<any[]> {
    return this.http.get<any[]>(
      `/api/animatrice/sante/observations`,
      this.getAuthHeadersStrict()
    );
  }

  ajouterTraitement(conditionId: number, payload: any): Observable<any> {
    return this.http.post<any>(
      `/api/traitements/condition/${conditionId}`,
      payload,
      this.getAuthHeaders()
    );
  }

  ajouterTraitementAvecOrdonnance(conditionId: number, formData: FormData): Observable<any> {
    return this.http.post<any>(
      `/api/traitements/condition/${conditionId}/avec-ordonnance`,
      formData,
      this.getAuthHeaders()
    );
  }

  modifierTraitementParent(traitementId: number, payload: any): Observable<any> {
    return this.http.put<any>(
      `/api/parent/traitements/${traitementId}`,
      payload,
      this.getAuthHeaders()
    );
  }

  supprimerTraitementParent(traitementId: number): Observable<void> {
    return this.http.delete<void>(
      `/api/parent/traitements/${traitementId}`,
      this.getAuthHeaders()
    );
  }

  telechargerOrdonnance(traitementId: number): Observable<Blob> {
    return this.http.get(
      `/api/traitements/${traitementId}/ordonnance`,
      { ...this.getAuthHeaders(), responseType: 'blob' as const }
    );
  }

  telechargerJustificatifPrise(priseId: number): Observable<Blob> {
    return this.http.get(
      `/api/animatrice/sante/prises/${priseId}/pdf`,
      { ...this.getAuthHeaders(), responseType: 'blob' as const }
    );
  }

  getObservationsParentParEnfant(enfantId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `/api/parent/observations/enfant/${enfantId}`,
      this.getAuthHeadersStrict()
    );
  }

  getObservationsParent(unreadOnly = false): Observable<any[]> {
    const qs = unreadOnly ? '?unreadOnly=true' : '';
    return this.http.get<any[]>(
      `/api/parent/observations${qs}`,
      this.getAuthHeadersStrict()
    );
  }

  getObservationsParentUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `/api/parent/observations/unread-count`,
      this.getAuthHeadersStrict()
    );
  }

  marquerObservationParentLue(observationId: number): Observable<any> {
    return this.http.put<any>(
      `/api/parent/observations/${observationId}/lu`,
      {},
      this.getAuthHeadersStrict()
    );
  }

  genererObservationDescription(payload: any): Observable<any> {
    return this.http.post<any>(
      `/api/animatrice/sante/observations/suggestion`,
      payload,
      this.getAuthHeadersStrict()
    );
  }
  modifierConditionSanitaire(id: number, payload: any) {
  return this.http.put<any>(
    `/api/conditions/${id}`,
    payload,
    this.getAuthHeaders()
  );
}

supprimerConditionSanitaire(id: number) {
  return this.http.delete<void>(
    `/api/conditions/${id}`,
    this.getAuthHeaders()
  );
}
}
