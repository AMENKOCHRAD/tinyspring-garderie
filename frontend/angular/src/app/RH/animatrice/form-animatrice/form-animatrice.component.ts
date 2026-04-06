import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Animatrice } from '../animatrice.model';
import { AnimatriceService } from '../../../services/RH/animatrice.service';

@Component({
  selector: 'app-form-animatrice',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './form-animatrice.component.html',
  styleUrl: './form-animatrice.component.scss'
})
export class FormAnimatriceComponent implements OnInit {

  isEditMode: boolean = false;
  animatriceId: number | null = null;
  isLoading: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';

  animatrice: Animatrice = {
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    dateEmbauche: '',
    statut: 'ACTIVE',
    specialite: '',
    photoUrl: ''
  };

  constructor(
    private animatriceService: AnimatriceService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.animatriceId = this.route.snapshot.params['id'];
    if (this.animatriceId) {
      this.isEditMode = true;
      this.loadAnimatrice(this.animatriceId);
    }
  }

  loadAnimatrice(id: number): void {
    this.animatriceService.getAnimatriceById(id).subscribe({
      next: (data) => {
        this.animatrice = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement', err)
    });
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.isEditMode && this.animatriceId) {
      this.animatriceService.updateAnimatrice(this.animatriceId, this.animatrice).subscribe({
        next: () => {
          this.successMessage = 'Animatrice modifiée avec succès !';
          this.isLoading = false;
          setTimeout(() => this.router.navigate(['/rh/animatrices']), 1500);
        },
        error: (err) => {
          this.errorMessage = 'Erreur lors de la modification.';
          this.isLoading = false;
        }
      });
    } else {
      this.animatriceService.createAnimatrice(this.animatrice).subscribe({
        next: () => {
          this.successMessage = 'Animatrice créée avec succès !';
          this.isLoading = false;
          setTimeout(() => this.router.navigate(['/rh/animatrices']), 1500);
        },
        error: (err) => {
          this.errorMessage = 'Erreur lors de la création.';
          this.isLoading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/rh/animatrices']);
  }
}