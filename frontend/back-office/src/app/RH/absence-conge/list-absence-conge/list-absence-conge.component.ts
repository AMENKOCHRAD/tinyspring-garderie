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

  pageActuelle = 1;
  parPage = 10;

  constructor(
    private absenceCongeService: AbsenceCongeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAbsenceConges();
    this.refreshInterval = setInterval(() => this.loadAbsenceConges(), 20000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  loadAbsenceConges(): void {
    this.isLoading = true;
    this.absenceCongeService.getAllAbsenceConges().subscribe({
      next: (data) => {
        this.absenceConges = data;
        this.pageActuelle = 1;
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

  // ✅ Sans accent
  getAbsencesPaginees(): AbsenceConge[] {
    const filtered = this.getFilteredAbsenceConges();
    const debut = (this.pageActuelle - 1) * this.parPage;
    return filtered.slice(debut, debut + this.parPage);
  }

  get totalPages(): number {
    return Math.ceil(this.getFilteredAbsenceConges().length / this.parPage);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.pageActuelle = page;
  }

  onFilterChange(): void { this.pageActuelle = 1; }

  getLastItemIndex(): number {
    return Math.min(this.pageActuelle * this.parPage, this.getFilteredAbsenceConges().length);
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