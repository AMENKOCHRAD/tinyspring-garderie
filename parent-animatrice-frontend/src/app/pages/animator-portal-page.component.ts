import { CommonModule } from '@angular/common';
import { Component, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { animatorModules } from '../shared/site-data';
import { AuthService } from '../shared/auth.service';

@Component({
  selector: 'app-animator-portal-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container-fluid bg-light py-5">
      <div class="container">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center mb-4">
          <div>
            <p class="section-title pr-5 mb-2"><span class="pr-2">Espace animatrice</span></p>
            <h1 class="mb-0">Outils front pour l animatrice</h1>
            <small class="text-muted" *ngIf="currentUserEmail()">{{ currentUserEmail() }}</small>
          </div>
          <div class="mt-3 mt-lg-0">
            <a class="btn btn-outline-primary mr-2" routerLink="/">Retour au site</a>
            <button type="button" class="btn btn-danger" (click)="logout()">Deconnexion</button>
          </div>
        </div>

        <div class="row">
          <div class="col-md-6 col-xl-3 mb-4" *ngFor="let module of modules">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body">
                <span class="badge text-white mb-3" [ngClass]="module.colorClass">{{ module.title }}</span>
                <h5>{{ module.title }}</h5>
                <p>{{ module.description }}</p>
                <a class="btn btn-outline-primary btn-sm" [routerLink]="module.route">{{ module.action }}</a>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded shadow-sm p-4">
          <h4 class="mb-3">Vue operationnelle</h4>
          <p class="mb-0">Cet espace reste dedie a l animatrice. Il ne contient aucun module admin et garde une separation nette avec le backoffice.</p>
        </div>
      </div>
    </div>
  `
})
export class AnimatorPortalPageComponent {
  protected readonly modules = animatorModules;
  protected readonly currentUserEmail = computed(() => this.authService.getCurrentUser()?.email ?? '');

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}
