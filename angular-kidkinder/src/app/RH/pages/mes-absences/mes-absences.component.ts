import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AbsenceConge } from '../../models/absence-conge.model';
import { AbsenceCongeService } from '../../services/absence-conge.service';
import { AuthService } from '../../../shared/auth.service';

@Component({
  selector: 'app-mes-absences',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mes-absences.component.html',
  styleUrl: './mes-absences.component.scss'
})
export class MesAbsencesComponent implements OnInit, OnDestroy {

  absences: AbsenceConge[] = [];
  isLoading = false;
  errorMessage = '';
  animatriceId = 0;
  private refreshInterval: any;

  constructor(
    private absenceCongeService: AbsenceCongeService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // ✅ Nouvelle template — getCurrentUser() retourne directement l'objet
    const user = this.authService.getCurrentUser();
    if (user?.email) {
      this.absenceCongeService.getMonProfil(user.email).subscribe({
        next: (animatrice: any) => {
          this.animatriceId = animatrice.id;
          this.loadAbsences();
          this.refreshInterval = setInterval(() => this.loadAbsences(), 30000);
        },
        error: (err: any) => {
          console.error('Erreur profil', err);
          this.errorMessage = 'Impossible de récupérer votre profil.';
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadAbsences(): void {
    this.isLoading = true;
    this.absenceCongeService.getMesAbsenceConges(this.animatriceId).subscribe({
      next: (data: AbsenceConge[]) => {
        this.absences = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Erreur absences', err);
        this.errorMessage = 'Erreur lors du chargement.';
        this.isLoading = false;
      }
    });
  }

  getCountByStatut(statut: string): number {
    return this.absences.filter(a => a.statut === statut).length;
  }

  getBadgeClass(statut?: string): string {
    if (statut === 'APPROUVE') return 'badge-approuve';
    if (statut === 'REFUSE') return 'badge-refuse';
    return 'badge-attente';
  }

  getStatutLabel(statut?: string): string {
    if (statut === 'APPROUVE') return '✅ Approuvé';
    if (statut === 'REFUSE') return '❌ Refusé';
    return '⏳ En attente';
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'ABSENCE': 'Absence',
      'CONGE_ANNUEL': 'Congé annuel',
      'CONGE_MALADIE': 'Congé maladie',
      'CONGE_MATERNITE': 'Congé maternité'
    };
    return labels[type] || type;
  }
}