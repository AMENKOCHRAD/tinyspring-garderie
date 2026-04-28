import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AffectationService } from '../services/affectation.service';
import { GroupeService } from '../services/groupe.service';
import { Affectation } from '../models/affectation.model';
import { Groupe } from '../models/groupe.model';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-affectations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './affectations.component.html'
})
export class AffectationsComponent implements OnInit {
  affectations: Affectation[] = [];
  groupes: Groupe[] = [];
  currentAffectation: Affectation = { enfantId: 0, dateDebut: '', dateFin: '', statut: 'ACTIF', groupe: undefined };
  isEditMode = false;
  selectedGroupeId: number | '' = '';
  errorMessage = '';

  // IA Assistant variables
  showWizard = false;
  iaAge: number | null = null;
  iaLangue: string = 'Français';
  isSuggesting = false;
  hasSearched = false;
  suggestions: any[] = [];

  constructor(
    private affectationService: AffectationService,
    private groupeService: GroupeService,
    private cdr: ChangeDetectorRef,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadAffectations();
    this.loadGroupes();
  }

  loadAffectations(): void {
    this.affectationService.getAllAffectations().subscribe({
      next: (data) => {
        this.affectations = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  loadGroupes(): void {
    this.groupeService.getAllGroupes().subscribe({
      next: (data) => {
        this.groupes = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  saveAffectation(): void {
    this.errorMessage = '';
    const selectedGroupe = this.groupes.find(g => g.id == this.selectedGroupeId);
    this.currentAffectation.groupe = selectedGroupe;

    if (this.isEditMode && this.currentAffectation.id) {
      this.affectationService.updateAffectation(this.currentAffectation.id, this.currentAffectation).subscribe({
        next: () => {
          this.loadAffectations();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de la modification.";
          this.cdr.detectChanges();
        }
      });
    } else {
      this.affectationService.addAffectation(this.currentAffectation).subscribe({
        next: () => {
          this.loadAffectations();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de l'ajout.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  editAffectation(affectation: Affectation): void {
    this.currentAffectation = { ...affectation };
    this.selectedGroupeId = affectation.groupe?.id || '';
    this.isEditMode = true;
    this.errorMessage = '';
  }

  deleteAffectation(id: number | undefined): void {
    if (id && confirm('Voulez-vous supprimer cette affectation ?')) {
      this.affectationService.deleteAffectation(id).subscribe({
        next: () => this.loadAffectations(),
        error: (err) => console.error(err)
      });
    }
  }

  resetForm(): void {
    this.currentAffectation = { enfantId: 0, dateDebut: '', dateFin: '', statut: 'ACTIF', groupe: undefined };
    this.selectedGroupeId = '';
    this.isEditMode = false;
    this.errorMessage = '';
    this.showWizard = false;
    this.suggestions = [];
    this.hasSearched = false;
  }

  getSuggestions(): void {
    if (!this.iaAge) return;

    this.isSuggesting = true;
    this.hasSearched = true;
    
    const requestPayload = { age: this.iaAge, languePrincipale: this.iaLangue };

    setTimeout(() => {
      this.http.post<any[]>('http://localhost:8081/api/groupes/suggestions', requestPayload).subscribe({
        next: (data) => {
          this.suggestions = data;
          this.isSuggesting = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
          this.isSuggesting = false;
          this.cdr.detectChanges();
        }
      });
    }, 800);
  }

  selectSuggestion(groupeId: number): void {
    this.selectedGroupeId = groupeId;
    this.showWizard = false;
  }
}
