import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProfilService, AnimatriceProfilData } from '../../services/profil.service';
import { AuthService } from '../../../shared/auth.service';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mon-profil.component.html',
  styleUrl: './mon-profil.component.scss'
})
export class MonProfilComponent implements OnInit {

  profil: AnimatriceProfilData | null = null;
  isLoading = true;
  isSaving = false;
  successMessage = '';
  errorMessage = '';

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  isUploadingPhoto = false;

  // Copie de travail pour le formulaire
  form: Partial<AnimatriceProfilData> = {};

  constructor(
    private profilService: ProfilService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user?.email) {
      this.profilService.getProfilParEmail(user.email).subscribe({
        next: (data) => {
          this.profil = data;
          this.form = { ...data };
          if (data.photoUrl) {
            this.previewUrl = `http://localhost:8081/uploads/animatrices/${data.photoUrl}`;
          }
          this.isLoading = false;
        },
        error: () => {
          this.errorMessage = 'Impossible de charger votre profil.';
          this.isLoading = false;
        }
      });
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    // Vérification taille (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'La photo ne doit pas dépasser 5MB.';
      return;
    }

    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.previewUrl = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  onSubmit(): void {
    if (!this.profil?.id) return;

    this.isSaving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const payload: AnimatriceProfilData = {
      id: this.profil.id,
      nom: this.form.nom || '',
      prenom: this.form.prenom || '',
      email: this.form.email || '',
      telephone: this.form.telephone?.trim() || undefined,
      specialite: this.form.specialite?.trim() || undefined,
      dateEmbauche: this.profil.dateEmbauche,
      statut: this.profil.statut,
      photoUrl: this.profil.photoUrl
    };

    this.profilService.updateProfil(this.profil.id, payload).subscribe({
      next: (updated) => {
        this.profil = updated;
        this.form = { ...updated };

        // Si une photo a été sélectionnée, on l'upload après la sauvegarde
        if (this.selectedFile) {
          this.uploadPhoto();
        } else {
          this.successMessage = 'Profil mis à jour avec succès !';
          this.isSaving = false;
        }
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Erreur lors de la mise à jour.';
        this.isSaving = false;
      }
    });
  }

  private uploadPhoto(): void {
    if (!this.profil?.id || !this.selectedFile) return;

    this.isUploadingPhoto = true;
    this.profilService.uploadPhoto(this.profil.id, this.selectedFile).subscribe({
      next: (updated) => {
        this.profil = updated;
        if (updated.photoUrl) {
          this.previewUrl = `http://localhost:8081/uploads/animatrices/${updated.photoUrl}`;
        }
        this.selectedFile = null;
        this.successMessage = 'Profil et photo mis à jour avec succès !';
        this.isSaving = false;
        this.isUploadingPhoto = false;
      },
      error: () => {
        this.successMessage = 'Profil mis à jour, mais erreur lors de l\'upload photo.';
        this.isSaving = false;
        this.isUploadingPhoto = false;
      }
    });
  }

  get initiales(): string {
    const prenom = this.form.prenom || '';
    const nom = this.form.nom || '';
    return `${prenom[0] || ''}${nom[0] || ''}`.toUpperCase();
  }

  private extractError(err: any, fallback: string): string {
    if (err?.error?.message) return err.error.message;
    if (err?.status === 0) return 'Serveur inaccessible.';
    if (err?.status === 401) return 'Session expirée. Veuillez vous reconnecter.';
    return fallback;
  }
}