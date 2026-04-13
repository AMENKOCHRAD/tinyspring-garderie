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
    <app-site-header *ngIf="showPublicChrome()"></app-site-header>
    <router-outlet></router-outlet>
    <app-site-footer *ngIf="showPublicChrome()"></app-site-footer>

    <button
      type="button"
      class="back-to-top"
      [class.visible]="showBackToTop() && showPublicChrome()"
      (click)="scrollToTop($event)">
      ↑
    </button>
  `
})
export class AppComponent {
  private readonly router = inject(Router);
  protected readonly showBackToTop = signal(false);
  protected readonly showPublicChrome = signal(true);

  public constructor() {
    this.showPublicChrome.set(this.shouldShowPublicChrome(this.router.url));
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.showPublicChrome.set(this.shouldShowPublicChrome(event.urlAfterRedirects));
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

  private shouldShowPublicChrome(url: string): boolean {
    return !(
      url.startsWith('/connexion') ||
      url.startsWith('/parent') ||
      url.startsWith('/animateur')
    );
  }
}
