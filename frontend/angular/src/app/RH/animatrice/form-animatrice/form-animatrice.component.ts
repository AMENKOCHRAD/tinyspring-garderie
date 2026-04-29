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
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Impossible de charger les données de l\'animatrice.';
        console.error('Erreur chargement', err);
      }
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

  private buildPayload(): Animatrice {
    return {
      ...this.animatrice,
      telephone:    this.animatrice.telephone?.trim()    || undefined,
      dateEmbauche: this.animatrice.dateEmbauche?.trim() || undefined,
      specialite:   this.animatrice.specialite?.trim()   || undefined,
      photoUrl:     this.animatrice.photoUrl?.trim()     || undefined,
    };
  }

  private extractErrorMessage(err: any, fallback: string): string {
    if (err?.error?.message) return err.error.message;
    if (err?.error?.errors) {
      // Erreurs de validation @Valid Spring → liste de messages
      return Object.values(err.error.errors).join(', ');
    }
    if (err?.status === 0) return 'Serveur inaccessible. Vérifiez que le backend est démarré.';
    if (err?.status === 401) return 'Session expirée. Veuillez vous reconnecter.';
    if (err?.status === 403) return 'Accès refusé.';
    if (err?.status === 404) return 'Ressource introuvable.';
    if (err?.status === 500) return 'Erreur interne du serveur.';
    return fallback;
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.motDePasseTemporaire = '';

    const payload = this.buildPayload();

    if (this.isEditMode && this.animatriceId) {
      this.animatriceService.updateAnimatrice(this.animatriceId, payload).subscribe({
        next: (updated) => {
          if (this.selectedFile) {
            this.uploadPhoto(updated.id!);
          } else {
            this.successMessage = 'Animatrice modifiée avec succès !';
            this.isLoading = false;
            setTimeout(() => this.router.navigate(['/rh/animatrices']), 1500);
          }
        },
        error: (err) => {
          this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la modification.');
          this.isLoading = false;
        }
      });
    } else {
      this.animatriceService.createAnimatrice(payload).subscribe({
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
        error: (err) => {
          this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la création.');
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
      error: (err) => {
        this.errorMessage = this.extractErrorMessage(err, 'Animatrice sauvegardée mais erreur lors de l\'upload photo.');
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/rh/animatrices']), 3000);
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/rh/animatrices']);
  }
}