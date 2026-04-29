import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ClasseService } from '../services/classe.service';
import { SalleService } from '../services/salle.service';
import { AiService } from '../services/ai.service';
import { Classe } from '../models/classe.model';
import { Salle } from '../models/salle.model';

@Component({
  selector: 'app-classes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './classes.component.html'
})
export class ClassesComponent implements OnInit {
  classes: Classe[] = [];
  salles: Salle[] = [];
  currentClasse: Classe = { nom: '', niveau: '', capaciteMax: 0, anneeScolaire: '2025/2026', ageMinimum: 1, ageMaximum: 6, salle: undefined };
  isEditMode = false;
  selectedSalleId: number | '' = '';
  errorMessage = '';
  aiSuggestion = '';
  isSuggesting = false;
  suggestingClasseName = '';

  constructor(
    private classeService: ClasseService,
    private salleService: SalleService,
    private aiService: AiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadClasses();
    this.loadSalles();
  }

  loadClasses(): void {
    this.classeService.getAllClasses().subscribe({
      next: (data) => {
        this.classes = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  loadSalles(): void {
    this.salleService.getAllSalles().subscribe({
      next: (data) => {
        this.salles = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  saveClasse(): void {
    this.errorMessage = '';
    const selectedSalle = this.salles.find(s => s.id == this.selectedSalleId);
    this.currentClasse.salle = selectedSalle;

    if (this.isEditMode && this.currentClasse.id) {
      this.classeService.updateClasse(this.currentClasse.id, this.currentClasse).subscribe({
        next: () => {
          this.loadClasses();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de la modification.";
          this.cdr.detectChanges();
        }
      });
    } else {
      this.classeService.addClasse(this.currentClasse).subscribe({
        next: () => {
          this.loadClasses();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de l'ajout.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  editClasse(classe: Classe): void {
    this.currentClasse = { ...classe };
    this.selectedSalleId = classe.salle?.id || '';
    this.isEditMode = true;
    this.errorMessage = '';
  }

  deleteClasse(id: number | undefined): void {
    if (id && confirm('Voulez-vous supprimer cette classe ?')) {
      this.classeService.deleteClasse(id).subscribe({
        next: () => this.loadClasses(),
        error: (err) => console.error(err)
      });
    }
  }

  resetForm(): void {
    this.currentClasse = { nom: '', niveau: '', capaciteMax: 0, anneeScolaire: '2025/2026', ageMinimum: 1, ageMaximum: 6, salle: undefined };
    this.selectedSalleId = '';
    this.isEditMode = false;
    this.errorMessage = '';
  }

  suggestActivities(classe: Classe): void {
    if(!classe.ageMinimum || !classe.ageMaximum || !classe.capaciteMax) return;
    this.isSuggesting = true;
    this.suggestingClasseName = classe.nom;
    this.aiSuggestion = '';
    this.cdr.detectChanges();

    this.aiService.suggestActivities(classe.ageMinimum, classe.ageMaximum, classe.capaciteMax).subscribe({
      next: (res) => {
        this.aiSuggestion = res.response;
        this.isSuggesting = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = "Erreur lors de la communication avec l'IA.";
        this.isSuggesting = false;
        this.cdr.detectChanges();
      }
    });
  }

  clearSuggestion(): void {
    this.aiSuggestion = '';
    this.suggestingClasseName = '';
    this.cdr.detectChanges();
  }
}
