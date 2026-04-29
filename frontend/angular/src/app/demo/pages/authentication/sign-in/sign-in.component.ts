import { ChangeDetectorRef, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { email, Field, form, minLength, required } from '@angular/forms/signals';

import { AuthService } from 'src/app/services/auth.service';
import { SharedModule } from 'src/app/theme/shared/shared.module';

@Component({
  selector: 'app-sign-in',
  imports: [CommonModule, RouterModule, SharedModule, Field],
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.scss']
})
export class SignInComponent {
  private readonly cd = inject(ChangeDetectorRef);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

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
        if (response.role !== 'ADMIN') {
          this.authService.logout();
          this.error.set("Connexion refusée. L'accès est réservé à l'administrateur.");
          this.cd.detectChanges();
          return;
        }

        this.authService.saveUser(response);
        void this.router.navigate(['/analytics']);
        this.cd.detectChanges();
      },
      error: (err) => {
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

  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }
}
