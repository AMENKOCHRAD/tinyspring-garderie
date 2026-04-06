import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { PageHeroComponent } from '../shared/page-hero.component';

@Component({
  selector: 'app-animatrice-dashboard',
  imports: [CommonModule, PageHeroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-hero title="Espace animatrice"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="portal-card bg-white p-5">
          <p class="section-title pr-5"><span class="pr-2">Animatrice</span></p>
          <h1 class="mb-4">Tableau de bord simplifie</h1>
          <p class="mb-0">
            Cet espace ne contient aucune gestion de transport. La logique des demandes et des CRUD transport
            reste reservee au portail parent et au back-office admin separe.
          </p>
        </div>
      </div>
    </div>
  `
})
export class AnimatriceDashboardComponent {}
