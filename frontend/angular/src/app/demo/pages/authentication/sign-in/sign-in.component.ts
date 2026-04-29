import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { email, Field, form, minLength, required } from '@angular/forms/signals';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-sign-in',
  imports: [CommonModule, RouterModule, SharedModule, Field],
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.scss']
})
export class SignInComponent {
  private cd = inject(ChangeDetectorRef);
  private authService = inject(AuthService);
  private router = inject(Router);

  submitted = signal(false);
  error = signal('');
  showPassword = signal(false);

  loginModal = signal<{ email: string; password: string }>({
    email: '',
    password: ''
  });

  loginForm = form(this.loginModal, (schemaPath) => {
    required(schemaPath.email, { message: 'Email is required' });
    email(schemaPath.email, { message: 'Enter a valid email address' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 8, { message: 'Password must be at least 8 characters' });
  });

  // Vérifie si le formulaire est soumis et si le champ a des erreurs
  submittedAndInvalid(field: any): boolean {
    return this.submitted() && field.errors().length > 0;
  }

  onSubmit(event: Event) {
    event.preventDefault();
    this.submitted.set(true);
    this.error.set('');

    // Vérifie si le formulaire est invalide
    if (this.loginForm.email().errors().length > 0 || this.loginForm.password().errors().length > 0) {
      this.cd.detectChanges();
      return;
    }

    const credentials = this.loginModal();

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.authService.saveUser(response);

        // Redirection selon le rôle
        if (response.role === 'ADMIN') {
          this.router.navigate(['/analytics']);
        } else if (response.role === 'PARENT' || response.role === 'ANIMATRICE') {
          this.router.navigate(['/sample-page']);
        } else {
          this.error.set('Rôle non reconnu');
        }

        this.cd.detectChanges();
      },
      error: (err) => {
        console.log('ERREUR COMPLETE = ', err);
        console.log('status = ', err.status);
        console.log('error body = ', err.error);

        if (err.status === 0) {
          this.error.set('Problème CORS ou backend inaccessible');
        } else if (err.status === 401) {
          this.error.set('Mot de passe incorrect');
        } else if (err.status === 404) {
          this.error.set('Utilisateur introuvable');
        } else {
          this.error.set('Erreur serveur : ' + err.status);
        }

        this.cd.detectChanges();
      }
    });
  }

  togglePasswordVisibility() {
    this.showPassword.set(!this.showPassword());
  }
}