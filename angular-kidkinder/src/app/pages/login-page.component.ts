import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { PageHeroComponent } from '../components/page-hero.component';
import { AuthService } from '../shared/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHeroComponent],
  template: `
    <app-page-hero title="Connexion"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-6 mb-5">
            <p class="section-title pr-5"><span class="pr-2">Authentification</span></p>
            <h1 class="mb-4">Connectez-vous à votre espace</h1>
            <p>   </p>
            
            <div class="mb-4">
              <p class="font-weight-bold">Accès disponible :</p>
              <ul class="list-inline m-0">
                <li class="py-2"><i class="fa fa-check text-success mr-3"></i>Parents → portail de services</li>
                <li class="py-2"><i class="fa fa-check text-success mr-3"></i>Animateurs → espace de coordination</li>
              </ul>
            </div>

            <!-- Admin notice -->
            <div class="alert alert-info border-0 bg-light rounded">
              <p class="mb-0"><strong>Vous êtes administrateur ?</strong></p>
              <p class="mb-0 text-muted">Les administrateurs accédent au <strong>backoffice</strong> via une connexion dédiée. Cet espace frontoffice est réservé aux parents et aux animateurs.</p>
            </div>
          </div>

          <div class="col-lg-5 offset-lg-1">
            <div class="card border-0 shadow">
              <div class="card-header bg-primary text-center p-4">
                <h2 class="text-white m-0">Se connecter</h2>
              </div>
              <div class="card-body p-5">
                <!-- Error messages -->
                <div *ngIf="error" class="alert alert-danger alert-dismissible fade show" role="alert">
                  {{ error }}
                  <button type="button" class="close" (click)="error = ''">
                    <span aria-hidden="true">&times;</span>
                  </button>
                </div>

                <form (ngSubmit)="submit()">
                  <div class="form-group">
                    <label for="email">Email</label>
                    <input 
                      type="email" 
                      class="form-control p-4" 
                      id="email"
                      [(ngModel)]="email" 
                      name="email" 
                      placeholder="votre&#64;email.com"
                      [disabled]="isLoading"
                      required>
                  </div>
                  <div class="form-group">
                    <label for="password">Mot de passe</label>
                    <input 
                      type="password" 
                      class="form-control p-4" 
                      id="password"
                      [(ngModel)]="password" 
                      name="password" 
                      placeholder="••••••••"
                      [disabled]="isLoading"
                      required>
                  </div>
                  <button 
                    type="submit" 
                    class="btn btn-primary btn-block py-3"
                    [disabled]="isLoading">
                    <span *ngIf="!isLoading">Connexion</span>
                    <span *ngIf="isLoading">
                      <i class="fa fa-spinner fa-spin mr-2"></i>Vérification...
                    </span>
                  </button>
                </form>

                <!-- Demo credentials info -->
                <hr class="my-4">
                <p class="text-muted small mb-2"><strong>Identifiants de démonstration :</strong></p>
                <ul class="text-muted small mb-0">
                  <li>Parent: parent&#64;garderie.com</li>
                  <li>Animatrice: animatrice&#64;garderie.com</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class LoginPageComponent {
  protected email = '';
  protected password = '';
  protected error = '';
  protected isLoading = false;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  protected submit(): void {
    // Reset error state
    this.error = '';
    
    // Validate inputs
    if (!this.email || !this.password) {
      this.error = 'Veuillez saisir votre email et votre mot de passe';
      return;
    }

    this.isLoading = true;

    // Call real backend authentication
    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.isLoading = false;
        
        // Check if user is ADMIN - they are NOT allowed in this frontoffice
        if (response.role === 'ADMIN') {
          this.error = 'Les administrateurs doivent accéder via le backoffice dédié. Cet espace est réservé aux parents et animateurs.';
          // Clear the auth state since we're refusing admin access
          this.authService.logout();
          return;
        }
        
        // Redirect based on user role from backend
        const redirectUrl = this.getRedirectUrl(response.role);
        void this.router.navigateByUrl(redirectUrl);
      },
      error: (err) => {
        this.isLoading = false;
        this.error = err.message || 'Erreur de connexion au serveur';
      }
    });
  }

  /**
   * Determine redirect URL based on user role from backend
   * ADMIN is NOT allowed - they should use the backoffice instead
   * @param role User role from login response
   * @returns Redirect URL string
   */
  private getRedirectUrl(role: string): string {
    const roleRedirectMap: Record<string, string> = {
      'PARENT': '/parent/portal',
      'ANIMATRICE': '/animateur/portal'
      // ADMIN intentionally excluded - see submit() method
    };

    return roleRedirectMap[role] || '/';
  }
}
