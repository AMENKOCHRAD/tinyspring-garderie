import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EnfantService, Enfant, EnfantUpdateDTO } from '../enfant';

@Component({
  selector: 'app-modifier-enfant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './modifier-enfant.html',
  styleUrls: ['./modifier-enfant.scss']
})
export class ModifierEnfantComponent implements OnInit {
  enfantId!: number;
  isLoading = false;
  error = '';
  success = '';

  formData: EnfantUpdateDTO = {
    nom: '',
    prenom: '',
    dateNaissance: '',
    groupeSanguin: '',
    allergies: '',
    contactUrgence: '',
    photo: '',
    parentId: 0
  };

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;

    if (!file) {
      return;
    }

    if (!file.type?.startsWith('image/')) {
      this.error = 'Veuillez choisir une image (PNG, JPG...).';
      input.value = '';
      return;
    }

    const maxBytes = 2 * 1024 * 1024; // 2MB
    if (file.size > maxBytes) {
      this.error = 'Image trop grande. Maximum 2 Mo.';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.formData.photo = (reader.result as string) || '';
      if (this.formData.photo) {
        this.error = '';
      }
    };
    reader.onerror = () => {
      this.error = "Impossible de lire l'image selectionnee.";
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  clearPhoto(photoInput?: HTMLInputElement | null): void {
    this.formData.photo = '';
    if (photoInput) {
      photoInput.value = '';
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private enfantService: EnfantService
  ) {}

  ngOnInit(): void {
    this.enfantId = Number(this.route.snapshot.paramMap.get('id'));

    this.enfantService.getEnfantById(this.enfantId).subscribe({
      next: (enfant: Enfant) => {
        this.formData = {
          nom: enfant.nom,
          prenom: enfant.prenom,
          dateNaissance: enfant.dateNaissance,
          groupeSanguin: enfant.groupeSanguin,
          allergies: enfant.allergies,
          contactUrgence: enfant.contactUrgence,
          photo: enfant.photo,
          parentId: enfant.parent.id
        };
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    this.enfantService.updateEnfant(this.enfantId, this.formData).subscribe({
      next: () => {
        this.success = 'Enfant modifié avec succès';
        setTimeout(() => {
          this.router.navigate(['/gestion-enfants/liste']);
        }, 1000);
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors de la modification';
      }
    });
  }
}
