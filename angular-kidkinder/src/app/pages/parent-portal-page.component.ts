import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { EventsSectionComponent } from '../events/events-section.component';
import { AuthService } from '../shared/auth.service';
import { parentOverview } from '../shared/site-data';

@Component({
  selector: 'app-parent-portal-page',
  standalone: true,
  imports: [CommonModule, RouterLink, EventsSectionComponent],
  template: `
    <div class="container-fluid bg-light py-4">
      <div class="container">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center mb-4">
          <div>
            <p class="section-title pr-5 mb-2"><span class="pr-2">Portail parent</span></p>
            <h1 class="mb-0">Bienvenue dans votre espace</h1>
          </div>
          <div class="mt-3 mt-lg-0">
            <a class="btn btn-outline-primary mr-2" routerLink="/">Retour au site</a>
            <button type="button" class="btn btn-danger" (click)="logout()">Deconnexion</button>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-md-6 col-lg-3 mb-3" *ngFor="let item of overview">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <small class="text-muted d-block mb-2">{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <app-events-section></app-events-section>
      </div>
    </div>
  `
})
export class ParentPortalPageComponent {
  protected readonly overview = parentOverview;

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}
