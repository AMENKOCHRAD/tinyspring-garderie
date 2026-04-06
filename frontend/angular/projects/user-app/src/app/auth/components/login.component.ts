import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PageHeroComponent } from '../../components/shared/page-hero.component';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'user-login',
  imports: [CommonModule, ReactiveFormsModule, PageHeroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-hero title="Connexion"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-6 mb-5">
            <p class="section-title pr-5"><span class="pr-2">Portail parent</span></p>
            <h1 class="mb-4">Connexion unique avec redirection par role</h1>
            <p>
              L authentification reste commune, mais chaque role est envoye vers son application dediee.
              L admin sort vers l application admin, le parent ouvre son portail TinySpring, et l animatrice
              accede a un tableau de bord minimal sans gestion transport.
            </p>
            <ul class="list-inline m-0">
              <li class="py-2">Admin -> http://localhost:4200</li>
              <li class="py-2">Parent -> http://localhost:4201/parent</li>
              <li class="py-2">Animatrice -> http://localhost:4201/animatrice</li>
            </ul>
          </div>

          <div class="col-lg-5 offset-lg-1">
            <div class="card border-0 shadow">
              <div class="card-header bg-primary text-center p-4">
                <h2 class="text-white m-0">Se connecter</h2>
              </div>
              <div class="card-body p-5">
                <form [formGroup]="form" (ngSubmit)="submit()">
                  <div class="form-group">
                    <label>Email</label>
                    <input type="email" class="form-control p-4" formControlName="email" placeholder="parent@garderie.com" />
                  </div>
                  <div class="form-group">
                    <label>Mot de passe</label>
                    <input type="password" class="form-control p-4" formControlName="password" placeholder="********" />
                  </div>

                  <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>

                  <button type="submit" class="btn btn-primary btn-block py-3" [disabled]="form.invalid || loading()">
                    {{ loading() ? 'Connexion...' : 'Connexion' }}
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  protected readonly loading = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const credentials = this.form.getRawValue();
    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.authService.saveSession(response, credentials);
        this.authService.redirectToRoleHome(response.role);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        if (error.status === 401) {
          this.error.set('Mot de passe incorrect.');
        } else if (error.status === 404) {
          this.error.set('Utilisateur introuvable.');
        } else {
          this.error.set('Connexion impossible. Verifiez le backend et le CORS.');
        }
      }
    });
  }
}
