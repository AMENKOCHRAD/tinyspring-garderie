import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../auth/services/auth.service';

@Component({
  selector: 'app-site-header',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="container-fluid bg-light position-relative shadow">
      <nav class="navbar navbar-expand-lg bg-light navbar-light py-3 py-lg-0 px-0 px-lg-5">
        <a routerLink="/" class="navbar-brand font-weight-bold text-secondary" style="font-size: 50px;">
          <i class="flaticon-043-teddy-bear"></i>
          <span class="text-primary">TinySpring</span>
        </a>

        <button type="button" class="navbar-toggler" (click)="menuOpen = !menuOpen" [attr.aria-expanded]="menuOpen">
          <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse justify-content-between" [class.show]="menuOpen">
          <div class="navbar-nav font-weight-bold mx-auto py-0">
            <a
              *ngFor="let item of navItems"
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.exact ?? false }"
              class="nav-item nav-link"
              (click)="menuOpen = false">
              {{ item.label }}
            </a>
          </div>

          <button *ngIf="isAuthenticated(); else loginButton" class="btn btn-primary px-4" type="button" (click)="logout()">
            Deconnexion
          </button>
          <ng-template #loginButton>
            <a routerLink="/login" class="btn btn-primary px-4">Connexion</a>
          </ng-template>
        </div>
      </nav>
    </div>
  `
})
export class SiteHeaderComponent {
  private readonly authService = inject(AuthService);
  protected menuOpen = false;

  protected get navItems(): Array<{ label: string; path: string; exact?: boolean }> {
    if (!this.isAuthenticated()) {
      return [
        { label: 'Accueil', path: '/', exact: true },
        { label: 'Transport', path: '/login' }
      ];
    }

    if (this.authService.getRole() === 'ANIMATRICE') {
      return [{ label: 'Dashboard', path: '/animatrice', exact: true }];
    }

    return [
      { label: 'Dashboard', path: '/parent', exact: true },
      { label: 'Mes Demandes', path: '/demandes', exact: true }
    ];
  }

  protected logout(): void {
    this.authService.logout();
  }

  protected isAuthenticated(): boolean {
    return !!this.authService.getSession();
  }
}
