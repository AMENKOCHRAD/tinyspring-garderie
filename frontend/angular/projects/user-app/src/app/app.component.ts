import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';

import { SiteFooterComponent } from './components/layout/site-footer.component';
import { SiteHeaderComponent } from './components/layout/site-header.component';

@Component({
  selector: 'user-root',
  imports: [CommonModule, RouterOutlet, SiteHeaderComponent, SiteFooterComponent],
  template: `
    <app-site-header *ngIf="showChrome()"></app-site-header>
    <router-outlet></router-outlet>
    <app-site-footer *ngIf="showChrome()"></app-site-footer>
  `
})
export class AppComponent {
  private readonly router = inject(Router);

  protected readonly showChrome = signal(false);

  constructor() {
    this.updateChrome(this.router.url);
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.updateChrome(event.urlAfterRedirects);
      }
    });
  }

  private updateChrome(url: string): void {
    this.showChrome.set(url !== '/login');
  }
}
