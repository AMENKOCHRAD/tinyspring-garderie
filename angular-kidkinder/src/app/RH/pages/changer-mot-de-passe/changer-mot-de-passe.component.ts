import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../shared/auth.service';

@Component({
  selector: 'app-changer-mot-de-passe',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './changer-mot-de-passe.component.html',
  styleUrl: './changer-mot-de-passe.component.scss'
})
export class ChangerMotDePasseComponent implements OnInit {

  private readonly fb          = inject(FormBuilder);
  private readonly router      = inject(Router);
  private readonly http        = inject(HttpClient);
  private readonly authService = inject(AuthService);

  protected readonly isSubmitting   = signal(false);
  protected readonly successMessage = signal('');
  protected readonly errorMessage   = signal('');
  protected readonly showAncien     = signal(false);
  protected readonly showNouveau    = signal(false);
  protected readonly showConfirm    = signal(false);

  // ✅ ID réel de la table animatrices (résolu via par-email)
  private animatriceId: number | null = null;

  protected readonly form = this.fb.nonNullable.group(
    {
      ancienMotDePasse:    ['', [Validators.required]],
      nouveauMotDePasse:   ['', [Validators.required, Validators.minLength(6)]],
      confirmerMotDePasse: ['', [Validators.required]]
    },
    { validators: this.motsDePasseIdentiques }
  );

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    // ✅ Récupérer l'ID animatrice via email — sans modifier les modèles partagés
    this.http
      .get<{ id: number }>(`/api/animatrice/profil/par-email?email=${user.email}`)
      .subscribe({
        next: (animatrice) => {
          this.animatriceId = animatrice.id;
        },
        error: () => {
          this.errorMessage.set('Impossible de charger le profil. Veuillez vous reconnecter.');
        }
      });
  }

  private motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
    const nouveau   = group.get('nouveauMotDePasse')?.value;
    const confirmer = group.get('confirmerMotDePasse')?.value;
    return nouveau === confirmer ? null : { mismatch: true };
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const user = this.authService.getCurrentUser();
    if (!user) {
      this.errorMessage.set('Session expirée. Veuillez vous reconnecter.');
      return;
    }

    if (!this.animatriceId) {
      this.errorMessage.set('Profil non chargé. Veuillez réessayer.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { ancienMotDePasse, nouveauMotDePasse } = this.form.getRawValue();

    this.http
      .post<{ message: string }>(
        `/api/animatrice/profil/${this.animatriceId}/changer-mot-de-passe`,
        { ancienMotDePasse, nouveauMotDePasse }
      )
      .subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.successMessage.set(res.message);
          setTimeout(() => {
            void this.router.navigate(['/animateur/tableau-de-bord']);
          }, 2000);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(err?.error?.message || 'Erreur lors du changement de mot de passe.');
        }
      });
  }
}