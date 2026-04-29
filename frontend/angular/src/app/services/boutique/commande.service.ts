import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Commande, CommandeRequest, StatutCommande } from 'src/app/models/boutique/commande.model';

@Injectable({ providedIn: 'root' })
export class CommandeService {
  private adminUrl = 'http://localhost:8081/api/admin/boutique/commandes';
  private http = inject(HttpClient);

  getAll(): Observable<Commande[]> {
    return this.http.get<Commande[]>(this.adminUrl);
  }

  getById(id: number): Observable<Commande> {
    return this.http.get<Commande>(`${this.adminUrl}/${id}`);
  }

  getByStatut(statut: StatutCommande): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.adminUrl}/statut/${statut}`);
  }

  updateStatut(id: number, statut: StatutCommande): Observable<Commande> {
    return this.http.patch<Commande>(`${this.adminUrl}/${id}/statut?statut=${statut}`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/${id}`);
  }

  create(data: CommandeRequest): Observable<Commande> {
    return this.http.post<Commande>('http://localhost:8081/api/boutique/commandes', data);
  }
}
