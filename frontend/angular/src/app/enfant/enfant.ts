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

@Injectable({
  providedIn: 'root'
})
export class EnfantService {

  private apiUrl = 'http://localhost:8081/api/enfants';

  constructor(private http: HttpClient) {}

  getAllEnfants(): Observable<Enfant[]> {
    return this.http.get<Enfant[]>(this.apiUrl);
  }

  deleteEnfant(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}