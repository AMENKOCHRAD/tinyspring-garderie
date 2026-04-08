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
  motDePasseTemporaire: string = '';

  selectedFile: File | null = null;
  previewUrl: string | null = null;

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
        if (data.photoUrl) {
          this.previewUrl = `http://localhost:8081/uploads/animatrices/${data.photoUrl}`;
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement', err)
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.previewUrl = e.target.result;
        this.cdr.detectChanges();
      };
      reader.readAsDataURL(file);
    }
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.motDePasseTemporaire = '';

    if (this.isEditMode && this.animatriceId) {
      this.animatriceService.updateAnimatrice(this.animatriceId, this.animatrice).subscribe({
        next: (updated) => {
          if (this.selectedFile) {
            this.uploadPhoto(updated.id!);
          } else {
            this.successMessage = 'Animatrice modifiée avec succès !';
            this.isLoading = false;
            setTimeout(() => this.router.navigate(['/rh/animatrices']), 1500);
          }
        },
        error: () => {
          this.errorMessage = 'Erreur lors de la modification.';
          this.isLoading = false;
        }
      });
    } else {
      this.animatriceService.createAnimatrice(this.animatrice).subscribe({
        next: (created) => {
          if (created.motDePasseTemporaire) {
            this.motDePasseTemporaire = created.motDePasseTemporaire;
          }
          if (this.selectedFile && created.id) {
            this.uploadPhoto(created.id);
          } else {
            this.successMessage = 'Animatrice créée avec succès !';
            this.isLoading = false;
            setTimeout(() => this.router.navigate(['/rh/animatrices']), 3000);
          }
        },
        error: () => {
          this.errorMessage = 'Erreur lors de la création.';
          this.isLoading = false;
        }
      });
    }
  }

  uploadPhoto(id: number): void {
    this.animatriceService.uploadPhoto(id, this.selectedFile!).subscribe({
      next: () => {
        this.successMessage = this.isEditMode
          ? 'Animatrice modifiée avec succès !'
          : 'Animatrice créée avec succès !';
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/rh/animatrices']), 3000);
      },
      error: () => {
        this.successMessage = this.isEditMode
          ? 'Animatrice modifiée mais erreur upload photo.'
          : 'Animatrice créée mais erreur upload photo.';
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/rh/animatrices']), 3000);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/rh/animatrices']);
  }
}