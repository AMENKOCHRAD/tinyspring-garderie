import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Formation } from '../formation.model';
import { FormationService } from '../../../services/RH/formation.service';

@Component({
  selector: 'app-list-formation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './list-formation.component.html',
  styleUrl: './list-formation.component.scss'
})
export class ListFormationComponent implements OnInit {

  formations: Formation[] = [];
  searchTerm: string = '';
  filterType: string = '';

  constructor(
    private formationService: FormationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFormations();
  }

  loadFormations(): void {
    this.formationService.getAllFormations().subscribe({
      next: (data) => {
        this.formations = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur', err)
    });
  }

  getFilteredFormations(): Formation[] {
    return this.formations.filter(f => {
      const matchSearch = !this.searchTerm ||
        f.titre.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (f.formateur || '').toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchType = !this.filterType || f.type === this.filterType;
      return matchSearch && matchType;
    });
  }

  getCountByType(type: string): number {
    return this.formations.filter(f => f.type === type).length;
  }

  deleteFormation(id: number): void {
    if (confirm('Supprimer cette formation ?')) {
      this.formationService.deleteFormation(id).subscribe({
        next: () => {
          this.formations = this.formations.filter(f => f.id !== id);
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }
}