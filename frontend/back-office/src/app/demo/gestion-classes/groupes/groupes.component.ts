import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GroupeService } from '../services/groupe.service';
import { ClasseService } from '../services/classe.service';
import { AiService } from '../services/ai.service';
import { Groupe } from '../models/groupe.model';
import { Classe } from '../models/classe.model';

@Component({
  selector: 'app-groupes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './groupes.component.html'
})
export class GroupesComponent implements OnInit {
  groupes: Groupe[] = [];
  classes: Classe[] = [];
  currentGroupe: Groupe = { nom: '', capacite: 0, animatriceId: 0, horaireDebut: '08:00', horaireFin: '17:00', languePrincipale: 'Français', classe: undefined };
  isEditMode = false;
  selectedClasseId: number | '' = '';
  errorMessage = '';

  // ML Room Recommendation
  roomRecommendation = '';
  isRecommendingRoom = false;
  recommendingGroupeName = '';
  roomConfidence: number | null = null;
  roomAccuracy: number | null = null;

  constructor(
    private groupeService: GroupeService,
    private classeService: ClasseService,
    private aiService: AiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadGroupes();
    this.loadClasses();
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

  loadClasses(): void {
    this.classeService.getAllClasses().subscribe({
      next: (data) => {
        this.classes = [...data];
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  saveGroupe(): void {
    this.errorMessage = '';
    const selectedClasse = this.classes.find((c) => c.id == this.selectedClasseId);
    this.currentGroupe.classe = selectedClasse;

    if (this.isEditMode && this.currentGroupe.id) {
      this.groupeService.updateGroupe(this.currentGroupe.id, this.currentGroupe).subscribe({
        next: () => {
          this.loadGroupes();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de la modification.";
          this.cdr.detectChanges();
        }
      });
    } else {
      this.groupeService.addGroupe(this.currentGroupe).subscribe({
        next: () => {
          this.loadGroupes();
          this.resetForm();
        },
        error: (err) => {
          this.errorMessage = err.error?.error || "Erreur lors de l'ajout.";
          this.cdr.detectChanges();
        }
      });
    }
  }

  editGroupe(groupe: Groupe): void {
    this.currentGroupe = { ...groupe };
    this.selectedClasseId = groupe.classe?.id || '';
    this.isEditMode = true;
    this.errorMessage = '';
  }

  deleteGroupe(id: number | undefined): void {
    if (id && confirm('Voulez-vous supprimer ce groupe ?')) {
      this.groupeService.deleteGroupe(id).subscribe({
        next: () => this.loadGroupes(),
        error: (err) => console.error(err)
      });
    }
  }

  resetForm(): void {
    this.currentGroupe = { nom: '', capacite: 0, animatriceId: 0, horaireDebut: '08:00', horaireFin: '17:00', languePrincipale: 'Français', classe: undefined };
    this.selectedClasseId = '';
    this.isEditMode = false;
    this.errorMessage = '';
  }

  recommendRoom(groupe: Groupe): void {
    this.isRecommendingRoom = true;
    this.recommendingGroupeName = groupe.nom;
    this.roomRecommendation = '';
    this.roomConfidence = null;
    this.roomAccuracy = null;
    this.cdr.detectChanges();

    const capacite = groupe.capacite || 15;
    const ageMoyen = groupe.classe ? Math.round((groupe.classe.ageMinimum + groupe.classe.ageMaximum) / 2) : 4;

    this.aiService.recommendRoom(capacite, ageMoyen).subscribe({
      next: (res) => {
        this.roomRecommendation = res.room_type || res.recommended_room_type || 'Type inconnu';
        this.roomConfidence = res.confidence || null;
        this.roomAccuracy = res.accuracy_model || null;
        this.isRecommendingRoom = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.roomRecommendation = 'Erreur lors de la communication avec le modèle ML.';
        this.isRecommendingRoom = false;
        this.cdr.detectChanges();
      }
    });
  }

  clearRoomRecommendation(): void {
    this.roomRecommendation = '';
    this.recommendingGroupeName = '';
    this.roomConfidence = null;
    this.roomAccuracy = null;
    this.cdr.detectChanges();
  }
}
