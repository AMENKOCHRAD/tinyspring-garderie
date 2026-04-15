import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { QuotaService, QuotaConge } from '../../../services/RH/quota.service';

@Component({
  selector: 'app-list-quota',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './list-quota.component.html',
  styleUrl: './list-quota.component.scss'
})
export class ListQuotaComponent implements OnInit {

  quotas: QuotaConge[] = [];
  isLoading = false;
  successMessage = '';
  errorMessage = '';
  editingType: string | null = null;

  typeLabels: Record<string, string> = {
    'CONGE_ANNUEL':    '🌴 Congé Annuel',
    'CONGE_MALADIE':   '🏥 Congé Maladie',
    'CONGE_MATERNITE': '👶 Congé Maternité',
    'ABSENCE':         '📋 Absence'
  };

  typeColors: Record<string, string> = {
    'CONGE_ANNUEL':    'blue',
    'CONGE_MALADIE':   'orange',
    'CONGE_MATERNITE': 'pink',
    'ABSENCE':         'red'
  };

  constructor(
    private quotaService: QuotaService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadQuotas();
  }

  loadQuotas(): void {
    this.isLoading = true;
    this.quotaService.getAllQuotas().subscribe({
      next: (data) => {
        this.quotas = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement quotas', err);
        this.isLoading = false;
      }
    });
  }

  startEdit(type: string): void {
    this.editingType = type;
  }

  cancelEdit(): void {
    this.editingType = null;
    this.loadQuotas();
  }

  saveQuota(quota: QuotaConge): void {
    this.quotaService.updateQuota(quota.type, quota).subscribe({
      next: () => {
        this.successMessage = `✅ Quota ${this.typeLabels[quota.type]} mis à jour !`;
        this.editingType = null;
        this.loadQuotas();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de la mise à jour.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  getPourcentage(quota: QuotaConge): number {
    if (!quota.joursUtilisesAnneeEnCours || !quota.nbJoursMax) return 0;
    return Math.min(100, Math.round((quota.joursUtilisesAnneeEnCours / quota.nbJoursMax) * 100));
  }

  getProgressClass(pct: number): string {
    if (pct >= 90) return 'danger';
    if (pct >= 70) return 'warning';
    return 'success';
  }
}