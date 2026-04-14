import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AbsenceConge, TypeAbsenceConge } from '../../models/absence-conge.model';
import { AbsenceCongeService } from '../../services/absence-conge.service';
import { AuthService } from '../../../shared/auth.service';

@Component({
  selector: 'app-nouvelle-demande',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './nouvelle-demande.component.html',
  styleUrl: './nouvelle-demande.component.scss'
})
export class NouvelleDemande implements OnInit {

  isLoading = false;
  successMessage = '';
  errorMessage = '';
  animatriceId = 0;

  demande: AbsenceConge = {
    animatriceId: 0,
    type: 'CONGE_ANNUEL',
    dateDebut: '',
    dateFin: '',
    motif: ''
  };

  typesDisponibles: { value: TypeAbsenceConge; label: string }[] = [
    { value: 'CONGE_ANNUEL', label: '🌴 Congé annuel' },
    { value: 'CONGE_MALADIE', label: '🏥 Congé maladie' },
    { value: 'CONGE_MATERNITE', label: '👶 Congé maternité' },
    { value: 'ABSENCE', label: '📋 Absence' }
  ];

  constructor(
    private absenceCongeService: AbsenceCongeService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // ✅ Nouvelle template — getCurrentUser() retourne directement l'objet
    const user = this.authService.getCurrentUser();
    if (user?.email) {
      this.absenceCongeService.getMonProfil(user.email).subscribe({
        next: (animatrice: any) => {
          this.animatriceId = animatrice.id;
          this.demande.animatriceId = animatrice.id;
        },
        error: (err: any) => {
          console.error('Erreur profil', err);
          this.errorMessage = 'Impossible de récupérer votre profil.';
        }
      });
    }
  }

  get nbJoursCalcule(): number {
    if (!this.demande.dateDebut || !this.demande.dateFin) return 0;
    const debut = new Date(this.demande.dateDebut);
    const fin = new Date(this.demande.dateFin);
    const diff = Math.ceil((fin.getTime() - debut.getTime()) / (1000 * 60 * 60 * 24)) + 1;
    return diff > 0 ? diff : 0;
  }

  onSubmit(): void {
    if (!this.demande.dateDebut || !this.demande.dateFin) {
      this.errorMessage = 'Veuillez remplir les dates.';
      return;
    }
    if (new Date(this.demande.dateFin) < new Date(this.demande.dateDebut)) {
      this.errorMessage = 'La date de fin doit être après la date de début.';
      return;
    }
    if (this.animatriceId === 0) {
      this.errorMessage = 'Profil non chargé. Réessayez.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.absenceCongeService.soumettreDemande(this.demande).subscribe({
      next: () => {
        this.successMessage = '✅ Demande soumise ! En attente de validation.';
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/animateur/rh/mes-absences']), 2000);
      },
      error: (err: any) => {
        console.error('Erreur', err);
        this.errorMessage = 'Erreur lors de la soumission.';
        this.isLoading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/animateur/rh/mes-absences']);
  }
}