import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { navItems } from '../shared/site-data';
import { AuthService } from '../shared/auth.service';
import { AuthUser } from '../shared/auth.models';

@Component({
  selector: 'app-site-header',
  standalone: true,
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
          <!-- Navigation based on auth state and role -->
          <div class="navbar-nav font-weight-bold mx-auto py-0">
            <!-- Public nav items (always visible) -->
            <a
              *ngFor="let item of publicNavItems"
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              class="nav-item nav-link"
              (click)="menuOpen = false">
              {{ item.label }}
            </a>
            <a
              *ngIf="isLoggedIn && currentUser?.role === 'PARENT'"
              [routerLink]="'/parent/portal'"
              fragment="events-section"
              [class.active]="isEventsNavActive()"
              class="nav-item nav-link"
              (click)="menuOpen = false">
              Evenements
            </a>

            <!-- Parent nav items (if logged in as parent) -->
            <a
              *ngIf="isLoggedIn && currentUser?.role === 'PARENT'"
              [routerLink]="'/parent/portal'"
              [class.active]="isPortalNavActive()"
              class="nav-item nav-link"
              (click)="menuOpen = false">
              Mon Portail
            </a>

            <!-- Animator nav items (if logged in as animator) -->
            <a
              *ngIf="isLoggedIn && currentUser?.role === 'ANIMATRICE'"
              [routerLink]="'/animateur/portal'"
              routerLinkActive="active"
              class="nav-item nav-link"
              (click)="menuOpen = false">
              Mon Espace
            </a>
          </div>

          <!-- Auth buttons -->
          <div class="d-flex flex-column flex-lg-row align-items-lg-center gap-2">
            <!-- Not logged in: show Login button -->
            <a 
              *ngIf="!isLoggedIn"
              routerLink="/login" 
              class="btn btn-primary px-4">
              Connexion
            </a>

            <!-- Logged in: show user info and logout button -->
            <div *ngIf="isLoggedIn" class="d-flex align-items-center gap-3">
              <span class="text-muted">
                {{ currentUser?.email }}
                <small class="d-block text-secondary">({{ getRoleLabel(currentUser?.role) }})</small>
              </span>
              <button 
                type="button"
                class="btn btn-outline-danger px-4"
                (click)="logout()">
                Déconnexion
              </button>
            </div>
          </div>
        </div>
      </nav>
    </div>
  `
})
export class SiteHeaderComponent implements OnInit {
  protected menuOpen = false;
  protected isLoggedIn = false;
  protected currentUser: AuthUser | null = null;
  protected readonly publicNavItems = navItems;
  protected currentUrl = '';
  protected currentHash = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to auth state changes
    this.authService.auth$.subscribe((user) => {
      this.currentUser = user;
      this.isLoggedIn = user?.isAuthenticated ?? false;
    });

    this.updateRouteState();
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.updateRouteState();
      }
    });
  }

  /**
   * Logout user and redirect to home
   */
  protected logout(): void {
    this.authService.logout();
    this.menuOpen = false;
    void this.router.navigate(['/']);
  }

  /**
   * Get human-readable role label
   * @param role User role
   * @returns Role label string
   */
  protected getRoleLabel(role: string | null | undefined): string {
    const roleLabels: Record<string, string> = {
      'PARENT': 'Parent',
      'ANIMATRICE': 'Animateur'
      // Admin is NOT part of this frontoffice
    };
    return roleLabels[role || ''] || 'Utilisateur';
  }

  protected isEventsNavActive(): boolean {
    return this.currentUrl.startsWith('/parent/portal') && this.currentHash === 'events-section';
  }

  protected isPortalNavActive(): boolean {
    return this.currentUrl.startsWith('/parent/portal') && this.currentHash !== 'events-section';
  }

  private updateRouteState(): void {
    this.currentUrl = this.router.url.split('#')[0] ?? '';
    this.currentHash = window.location.hash.replace('#', '');
  }
}
