import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { email, Field, form, minLength, required } from '@angular/forms/signals';

<<<<<<< HEAD
import { AuthService } from 'src/app/services/auth.service';
import { SharedModule } from 'src/app/theme/shared/shared.module';
=======
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';
import { NotificationService } from 'src/app/services/notification.service';
import { PredictionService } from 'src/app/services/boutique/prediction.service';
>>>>>>> origin/gestion_boutique

@Component({
  selector: 'app-sign-in',
  imports: [CommonModule, RouterModule, SharedModule, Field],
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.scss']
})
export class SignInComponent {
<<<<<<< HEAD
  private readonly cd = inject(ChangeDetectorRef);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
=======
  private cd = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  private router = inject(Router);
  private notifService = inject(NotificationService);
  private predictionService = inject(PredictionService);
>>>>>>> origin/gestion_boutique

  readonly submitted = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);

  readonly loginModal = signal<{ email: string; password: string }>({
    email: '',
    password: ''
  });

  readonly loginForm = form(this.loginModal, (schemaPath) => {
    required(schemaPath.email, { message: 'Email is required' });
    email(schemaPath.email, { message: 'Enter a valid email address' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 8, { message: 'Password must be at least 8 characters' });
  });

  onSubmit(event: Event): void {
    this.submitted.set(true);
    this.error.set('');
    event.preventDefault();

    if (this.loginForm.email().invalid() || this.loginForm.password().invalid()) {
      this.cd.detectChanges();
      return;
    }

    const credentials = this.loginModal();

    this.authService.login(credentials).subscribe({
      next: (response) => {
<<<<<<< HEAD
        if (response.role !== 'ADMIN') {
          this.authService.logout();
          this.error.set("Connexion refusée. L'accès est réservé à l'administrateur.");
          this.cd.detectChanges();
          return;
=======
        this.authService.saveUser(response);

        const normalizedRole = this.authService.normalizeRole(response.role);
        if (normalizedRole === 'ADMIN') {
          this.notifService.startPolling();
          this.predictionService.startAlertPolling();
          this.router.navigate(['/admin/boutique/dashboard']);
        } else if (normalizedRole === 'PARENT') {
          this.router.navigate(['/sample-page']);
        } else if (normalizedRole === 'ANIMATRICE') {
          this.router.navigate(['/sample-page']);
        } else {
          this.error.set('Role non reconnu');
>>>>>>> origin/gestion_boutique
        }

        this.authService.saveUser(response);
        void this.router.navigate(['/analytics']);
        this.cd.detectChanges();
      },
      error: (err) => {
<<<<<<< HEAD
        if (err.status === 0) {
          this.error.set('Problème CORS ou backend inaccessible');
        } else if (err.status === 401) {
          this.error.set('Mot de passe incorrect');
        } else if (err.status === 404) {
          this.error.set('Utilisateur introuvable');
        } else {
          this.error.set('Erreur serveur : ' + err.status);
        }

=======
        console.log('ERREUR COMPLETE = ', err);
        console.log('status = ', err.status);
        console.log('error body = ', err.error);

        if (err.status === 0) {
          this.error.set('Probleme CORS ou backend inaccessible');
        } else if (err.status === 401) {
          this.error.set('Mot de passe incorrect');
        } else if (err.status === 404) {
          this.error.set('Utilisateur introuvable');
        } else {
          this.error.set('Erreur serveur : ' + err.status);
        }

>>>>>>> origin/gestion_boutique
        this.cd.detectChanges();
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }
}
