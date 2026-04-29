import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AbsenceConge } from '../absence-conge.model';

@Component({
  selector: 'app-detail-absence-conge',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './detail-absence-conge.component.html',
  styleUrl: './detail-absence-conge.component.scss'
})
export class DetailAbsenceCongeComponent implements OnInit {

  absence: AbsenceConge | null = null;
  isLoading = false;
  errorMessage = '';

  private apiUrl = 'http://localhost:8081/api/admin/absences-conges';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadAbsence(Number(id));
  }

  loadAbsence(id: number): void {
    this.isLoading = true;
    this.http.get<AbsenceConge>(`${this.apiUrl}/${id}`).subscribe({
      next: (data) => {
        this.absence = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger la demande.';
        this.isLoading = false;
      }
    });
  }

  valider(): void {
    if (!this.absence?.id || !confirm('Valider cette demande ?')) return;
    this.http.put<AbsenceConge>(`${this.apiUrl}/${this.absence.id}/valider`, {}).subscribe({
      next: (data) => { this.absence = data; }
    });
  }

  refuser(): void {
    if (!this.absence?.id || !confirm('Refuser cette demande ?')) return;
    this.http.put<AbsenceConge>(`${this.apiUrl}/${this.absence.id}/refuser`, {}).subscribe({
      next: (data) => { this.absence = data; }
    });
  }

  retour(): void {
    void this.router.navigate(['/rh/absences-conges']);
  }

  getBadgeClass(statut?: string): string {
    if (statut === 'APPROUVE') return 'badge-approuve';
    if (statut === 'REFUSE')   return 'badge-refuse';
    return 'badge-attente';
  }

  getStatutLabel(statut?: string): string {
    if (statut === 'APPROUVE') return '✅ Approuvé';
    if (statut === 'REFUSE')   return '❌ Refusé';
    return '⏳ En attente';
  }

  getTypeLabel(type?: string): string {
    const labels: Record<string, string> = {
      ABSENCE:          'Absence',
      CONGE_ANNUEL:     'Congé annuel',
      CONGE_MALADIE:    'Congé maladie',
      CONGE_MATERNITE:  'Congé maternité'
    };
    return labels[type || ''] || type || '—';
  }
}