import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AbsenceConge } from '../absence-conge.model';
import { AbsenceCongeService } from '../../../services/RH/absence-conge.service';

@Component({
  selector: 'app-list-absence-conge',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './list-absence-conge.component.html',
  styleUrl: './list-absence-conge.component.scss'
})
export class ListAbsenceCongeComponent implements OnInit, OnDestroy {

  absenceConges: AbsenceConge[] = [];
  filterStatut: string = '';
  isLoading: boolean = false;
  private refreshInterval: any;

  constructor(
    private absenceCongeService: AbsenceCongeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAbsenceConges();
    // ✅ Rafraîchissement automatique toutes les 20 secondes
    this.refreshInterval = setInterval(() => this.loadAbsenceConges(), 20000);
  }

  ngOnDestroy(): void {
    // ✅ Nettoyage quand on quitte la page
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadAbsenceConges(): void {
    this.isLoading = true;
    this.absenceCongeService.getAllAbsenceConges().subscribe({
      next: (data) => {
        this.absenceConges = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur', err);
        this.isLoading = false;
      }
    });
  }

  getFilteredAbsenceConges(): AbsenceConge[] {
    return this.absenceConges.filter(a =>
      !this.filterStatut || a.statut === this.filterStatut
    );
  }

  getCountByStatut(statut: string): number {
    return this.absenceConges.filter(a => a.statut === statut).length;
  }

  valider(id: number): void {
    if (confirm('Valider cette demande ?')) {
      this.absenceCongeService.validerDemande(id).subscribe({
        next: (updated) => {
          const index = this.absenceConges.findIndex(a => a.id === id);
          if (index !== -1) this.absenceConges[index] = updated;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erreur validation', err)
      });
    }
  }

  refuser(id: number): void {
    if (confirm('Refuser cette demande ?')) {
      this.absenceCongeService.refuserDemande(id).subscribe({
        next: (updated) => {
          const index = this.absenceConges.findIndex(a => a.id === id);
          if (index !== -1) this.absenceConges[index] = updated;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erreur refus', err)
      });
    }
  }

  supprimer(id: number): void {
    if (confirm('Supprimer cette demande ?')) {
      this.absenceCongeService.deleteAbsenceConge(id).subscribe({
        next: () => {
          this.absenceConges = this.absenceConges.filter(a => a.id !== id);
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }
}