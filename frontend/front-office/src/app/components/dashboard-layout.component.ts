import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { managementNavItems } from '../shared/site-data';
import { AuthService } from '../shared/auth.service';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="container-fluid">
      <div class="row min-vh-100">
        <aside class="col-lg-2 bg-secondary text-white py-4 px-0">
          <div class="px-4 mb-4">
            <h3 class="text-white mb-1">TinySpring</h3>
            <p class="mb-0 text-light">Espace gestion</p>
          </div>
          <nav class="nav flex-column">
            <a
              *ngFor="let item of items"
              class="nav-link text-white px-4 py-3"
              [routerLink]="item.path"
              routerLinkActive="bg-primary"
              [routerLinkActiveOptions]="{ exact: item.path === '/dashboard' }">
              {{ item.label }}
            </a>
          </nav>
        </aside>

        <main class="col-lg-10 bg-light px-0">
          <div class="d-flex justify-content-between align-items-center bg-white shadow-sm px-4 py-3">
            <div>
              <h4 class="mb-0">Tableau de bord</h4>
              <small class="text-muted">Navigation de gestion pour les donnees TinySpring</small>
            </div>
            <div class="d-flex">
              <a routerLink="/" class="btn btn-outline-primary mr-2">Voir le site</a>
              <button type="button" class="btn btn-danger" (click)="logout()">Déconnexion</button>
            </div>
          </div>

          <div class="p-4">
            <router-outlet></router-outlet>
          </div>
        </main>
      </div>
    </div>
  `
})
export class DashboardLayoutComponent {
  protected readonly items = managementNavItems;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Logout user
   */
  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}
