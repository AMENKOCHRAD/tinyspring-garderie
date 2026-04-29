import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Parent {
  id: number;
  nom: string;
  email: string;
  contactUrgence: string;
}

export interface Enfant {
  id: number;
  nom: string;
  prenom: string;
  dateNaissance: string;
  groupeSanguin: string;
  allergies: string;
  contactUrgence: string;
  photo: string;
  parent: Parent;
}

export interface EnfantUpdateDTO {
  nom: string;
  prenom: string;
  dateNaissance: string;
  groupeSanguin: string;
  allergies: string;
  contactUrgence: string;
  photo: string;
  parentId: number;
}

@Injectable({
  providedIn: 'root'
})
export class EnfantService {
  private apiUrl = '/api/enfants';

  constructor(private http: HttpClient) {}

  getAllEnfants(): Observable<Enfant[]> {
    return this.http.get<Enfant[]>(this.apiUrl);
  }

  getEnfantById(id: number): Observable<Enfant> {
    return this.http.get<Enfant>(`${this.apiUrl}/${id}`);
  }

  updateEnfant(id: number, enfant: EnfantUpdateDTO): Observable<Enfant> {
    return this.http.put<Enfant>(`${this.apiUrl}/${id}`, enfant);
  }

  archiverEnfant(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/archiver`, {});
  }
}
