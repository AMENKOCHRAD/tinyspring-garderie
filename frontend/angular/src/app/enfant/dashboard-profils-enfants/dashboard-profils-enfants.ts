import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { EnfantService, Enfant } from '../enfant';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard-profils-enfants',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-profils-enfants.html',
  styleUrls: ['./dashboard-profils-enfants.css']
})
export class DashboardProfilsEnfants implements OnInit {
  enfants: Enfant[] = [];
  enfantsFiltres: Enfant[] = [];
  isLoading = false;
  error = '';

  selectedNiveau: 'ALL' | 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU' = 'ALL';
  niveauGroups: Array<{
    key: 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU';
    label: string;
    ageRangeLabel: string;
    count: number;
  }> = [
    { key: 'CRECHE', label: 'Crèche', ageRangeLabel: '0 à 3 ans', count: 0 },
    { key: 'PRESCOLAIRE', label: 'Préscolaire', ageRangeLabel: '3 à 5 ans', count: 0 },
    { key: 'PREPARATOIRE', label: 'Préparatoire', ageRangeLabel: '5 à 6 ans', count: 0 },
    { key: 'HORS_NIVEAU', label: 'Hors niveau', ageRangeLabel: 'Autre', count: 0 }
  ];

  constructor(private enfantService: EnfantService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadEnfants();
  }

  loadEnfants(): void {
    this.isLoading = true;
    this.error = '';

    this.enfantService.getAllEnfants().subscribe({
      next: (data: Enfant[]) => {
        this.enfants = data ?? [];
        this.recomputeNiveaux();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  archiverEnfant(id: number): void {
    if (confirm('Archiver cet enfant ?')) {
      this.error = '';

      this.enfantService.archiverEnfant(id).subscribe({
        next: () => {
          this.enfants = this.enfants.filter((e) => e.id !== id);
          this.recomputeNiveaux();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
          this.error = 'Erreur lors de l’archivage';
          this.cdr.detectChanges();
        }
      });
    }
  }

  setNiveauFilter(value: 'ALL' | 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU'): void {
    this.selectedNiveau = value;
    this.applyFilter();
  }

  getNiveauLabel(enfant: Enfant): string {
    const key = this.getNiveauKey(enfant);
    if (key === 'CRECHE') return 'Crèche';
    if (key === 'PRESCOLAIRE') return 'Préscolaire';
    if (key === 'PREPARATOIRE') return 'Préparatoire';
    return 'Hors niveau';
  }

  private recomputeNiveaux(): void {
    const counts: Record<'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU', number> = {
      CRECHE: 0,
      PRESCOLAIRE: 0,
      PREPARATOIRE: 0,
      HORS_NIVEAU: 0
    };

    for (const enfant of this.enfants ?? []) {
      counts[this.getNiveauKey(enfant)]++;
    }

    this.niveauGroups = [
      { key: 'CRECHE', label: 'Crèche', ageRangeLabel: '0 à 3 ans', count: counts.CRECHE },
      { key: 'PRESCOLAIRE', label: 'Préscolaire', ageRangeLabel: '3 à 5 ans', count: counts.PRESCOLAIRE },
      { key: 'PREPARATOIRE', label: 'Préparatoire', ageRangeLabel: '5 à 6 ans', count: counts.PREPARATOIRE },
      { key: 'HORS_NIVEAU', label: 'Hors niveau', ageRangeLabel: 'Autre', count: counts.HORS_NIVEAU }
    ];

    this.applyFilter();
  }

  private applyFilter(): void {
    if (this.selectedNiveau === 'ALL') {
      this.enfantsFiltres = [...(this.enfants ?? [])];
      return;
    }
    this.enfantsFiltres = (this.enfants ?? []).filter((e) => this.getNiveauKey(e) === this.selectedNiveau);
  }

  private getNiveauKey(enfant: Enfant): 'CRECHE' | 'PRESCOLAIRE' | 'PREPARATOIRE' | 'HORS_NIVEAU' {
    const age = this.computeAgeYears(enfant?.dateNaissance);
    if (age === null) return 'HORS_NIVEAU';
    if (age < 3) return 'CRECHE';
    if (age < 5) return 'PRESCOLAIRE';
    if (age < 6) return 'PREPARATOIRE';
    return 'HORS_NIVEAU';
  }

  private computeAgeYears(dateNaissance: string | null | undefined): number | null {
    const raw = (dateNaissance ?? '').toString().trim();
    if (!raw) return null;
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return null;

    const now = new Date();
    let years = now.getFullYear() - d.getFullYear();
    const m = now.getMonth() - d.getMonth();
    if (m < 0 || (m === 0 && now.getDate() < d.getDate())) {
      years--;
    }
    return Math.max(0, years);
  }
}
