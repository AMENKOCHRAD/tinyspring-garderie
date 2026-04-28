import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Groupe } from '../models/groupe.model';

@Injectable({
  providedIn: 'root'
})
export class GroupeService {
  private apiUrl = 'http://localhost:8081/api/groupes';

  constructor(private http: HttpClient) {}

  getAllGroupes(): Observable<Groupe[]> {
    return this.http.get<Groupe[]>(this.apiUrl);
  }

  getGroupeById(id: number): Observable<Groupe> {
    return this.http.get<Groupe>(`${this.apiUrl}/${id}`);
  }

  addGroupe(groupe: Groupe): Observable<Groupe> {
    return this.http.post<Groupe>(this.apiUrl, groupe);
  }

  updateGroupe(id: number, groupe: Groupe): Observable<Groupe> {
    return this.http.put<Groupe>(`${this.apiUrl}/${id}`, groupe);
  }

  deleteGroupe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
