import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Formation } from '../formation.model';
import { FormationService } from '../../../services/RH/formation.service';

@Component({
  selector: 'app-form-formation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './form-formation.component.html',
  styleUrl: './form-formation.component.scss'
})
export class FormFormationComponent implements OnInit {

  isEditMode: boolean = false;
  formationId: number | null = null;
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  formation: Formation = {
    titre: '',
    description: '',
    type: 'INTERNE',
    dateDebut: '',
    dateFin: '',
    formateur: '',
    placesMax: 10,
    statutInscription: 'INSCRITE'
  };

  constructor(
    private formationService: FormationService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.formationId = this.route.snapshot.params['id'];
    if (this.formationId) {
      this.isEditMode = true;
      this.loadFormation(this.formationId);
    }
  }

  loadFormation(id: number): void {
    this.formationService.getFormationById(id).subscribe({
      next: (data) => {
        this.formation = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement', err)
    });
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.isEditMode && this.formationId) {
      this.formationService.updateFormation(this.formationId, this.formation).subscribe({
        next: () => {
          this.successMessage = 'Formation modifiée avec succès !';
          this.isLoading = false;
          setTimeout(() => this.router.navigate(['/rh/formations']), 1500);
        },
        error: () => {
          this.errorMessage = 'Erreur lors de la modification.';
          this.isLoading = false;
        }
      });
    } else {
      this.formationService.createFormation(this.formation).subscribe({
        next: () => {
          this.successMessage = 'Formation créée avec succès !';
          this.isLoading = false;
          setTimeout(() => this.router.navigate(['/rh/formations']), 1500);
        },
        error: () => {
          this.errorMessage = 'Erreur lors de la création.';
          this.isLoading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/rh/formations']);
  }
}