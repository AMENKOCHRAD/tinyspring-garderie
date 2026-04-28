import { CommonModule } from '@angular/common';
import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { SiteFooterComponent } from './components/site-footer.component';
import { SiteHeaderComponent } from './components/site-header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SiteHeaderComponent, SiteFooterComponent],
  template: `
    <app-site-header *ngIf="!isDashboardRoute()"></app-site-header>
    <router-outlet></router-outlet>
    <app-site-footer *ngIf="!isDashboardRoute()"></app-site-footer>

    <a href="#" class="btn btn-primary p-3 back-to-top" [style.display]="showBackToTop() && !isDashboardRoute() ? 'inline-flex' : 'none'" (click)="scrollToTop($event)">
      <i class="fa fa-angle-double-up"></i>
    </a>
  `
})
export class AppComponent {
  private readonly router = inject(Router);
  protected readonly showBackToTop = signal(false);
  protected readonly isDashboardRoute = signal(false);

  public constructor() {
    this.isDashboardRoute.set(this.router.url.startsWith('/dashboard'));
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.isDashboardRoute.set(event.urlAfterRedirects.startsWith('/dashboard'));
      }
    });
  }

  @HostListener('window:scroll')
  protected onWindowScroll(): void {
    this.showBackToTop.set(window.scrollY > 100);
  }

  protected scrollToTop(event: Event): void {
    event.preventDefault();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
