import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SalleService } from '../services/salle.service';
import { Salle } from '../models/salle.model';

@Component({
  selector: 'app-salles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './salles.component.html'
})
export class SallesComponent implements OnInit {
  salles: Salle[] = [];
  currentSalle: Salle = { nom: '', surface: 0, type: '', climatise: false, equipements: '', disponible: true };
  isEditMode = false;
  errorMessage = '';

  constructor(private salleService: SalleService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadSalles();
  }

  loadSalles(): void {
    this.salleService.getAllSalles().subscribe({
      next: (data) => {
        this.salles = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur de chargement des salles', err)
    });
  }

  saveSalle(): void {
    this.errorMessage = '';
    if (this.isEditMode && this.currentSalle.id) {
      this.salleService.updateSalle(this.currentSalle.id, this.currentSalle).subscribe({
        next: () => {
          this.loadSalles();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de la modification.";
          this.cdr.detectChanges();
        }
      });
    } else {
      this.salleService.addSalle(this.currentSalle).subscribe({
        next: () => {
          this.loadSalles();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de l'ajout.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  editSalle(salle: Salle): void {
    this.currentSalle = { ...salle };
    this.isEditMode = true;
    this.errorMessage = '';
  }

  deleteSalle(id: number | undefined): void {
    if (id && confirm('Voulez-vous vraiment supprimer cette salle ?')) {
      this.salleService.deleteSalle(id).subscribe({
        next: () => this.loadSalles(),
        error: (err) => console.error('Erreur lors de la suppression', err)
      });
    }
  }

  resetForm(): void {
    this.currentSalle = { nom: '', surface: 0, type: '', climatise: false, equipements: '', disponible: true };
    this.isEditMode = false;
    this.errorMessage = '';
  }
}
