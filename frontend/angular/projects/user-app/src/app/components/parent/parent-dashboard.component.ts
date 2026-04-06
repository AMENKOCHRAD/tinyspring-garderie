import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../auth/services/auth.service';
import { PageHeroComponent } from '../shared/page-hero.component';
import { DemandeTransport } from '../../models/transport.models';
import { DemandesService } from '../../services/demandes.service';

@Component({
  selector: 'app-parent-dashboard',
  imports: [CommonModule, RouterLink, PageHeroComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-page-hero title="Portail parent"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center mb-4">
          <div>
            <p class="section-title pr-5 mb-2"><span class="pr-2">Portail parent</span></p>
            <h1 class="mb-0">Bienvenue dans votre espace</h1>
          </div>
          <div class="mt-3 mt-lg-0">
            <a class="btn btn-primary" routerLink="/demandes">Demander un transport</a>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-md-6 col-lg-3 mb-3" *ngFor="let item of overview()">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <small class="text-muted d-block mb-2">{{ item.label }}</small>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-md-6 col-xl-4 mb-4" *ngFor="let module of modules">
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

        <div class="row mb-4">
          <div class="col-lg-8 mb-4">
            <div class="portal-card bg-white p-4 h-100">
              <h4 class="mb-3">Mes demandes recentes</h4>
              <div class="table-responsive portal-table">
                <table class="table table-hover mb-0">
                  <thead class="thead-light">
                    <tr>
                      <th>ID</th>
                      <th>Trajet</th>
                      <th>Date demande</th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let demande of latestDemandes()">
                      <td>#{{ demande.id }}</td>
                      <td>{{ demande.pointDepart }} -> {{ demande.destination }}</td>
                      <td>{{ demande.dateDemande }}</td>
                      <td>{{ demande.statut }}</td>
                    </tr>
                    <tr *ngIf="!latestDemandes().length">
                      <td colspan="4" class="text-center text-muted">Aucune demande enregistree.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div class="col-lg-4 mb-4">
            <div class="portal-card bg-white p-4 h-100">
              <h4 class="mb-3">Transport</h4>
              <p>
                Votre portail parent reste totalement separe du dashboard admin. Ici, vous pouvez uniquement
                consulter et gerer vos propres demandes de transport.
              </p>
              <a class="btn btn-primary" routerLink="/demandes">Ouvrir Mes Demandes</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ParentDashboardComponent {
  private readonly demandesService = inject(DemandesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly authService = inject(AuthService);

  protected readonly demandes = signal<DemandeTransport[]>([]);

  protected readonly modules = [
    {
      title: 'Transport',
      description: 'Creer, modifier ou supprimer vos demandes de transport.',
      action: 'Demander un transport',
      colorClass: 'bg-info',
      route: '/demandes'
    },
    {
      title: 'Suivi',
      description: 'Verifier rapidement le statut de vos demandes en attente ou traitees.',
      action: 'Voir mes demandes',
      colorClass: 'bg-primary',
      route: '/demandes'
    },
    {
      title: 'Espace parent',
      description: 'Acceder a un portail dedie, sans aucune interface admin.',
      action: 'Rester sur le portail',
      colorClass: 'bg-secondary',
      route: '/parent'
    }
  ];

  constructor() {
    this.demandesService
      .getMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((demandes) => this.demandes.set(demandes));
  }

  protected overview(): Array<{ label: string; value: string }> {
    const session = this.authService.getSession();
    return [
      { label: 'Parent connecte', value: session?.email ?? '-' },
      { label: 'Demandes totales', value: `${this.demandes().length}` },
      { label: 'En attente', value: `${this.demandes().filter((item) => item.statut === 'EN_ATTENTE').length}` },
      { label: 'Derniere action', value: this.demandes()[0]?.dateDemande ?? '-' }
    ];
  }

  protected latestDemandes(): DemandeTransport[] {
    return this.demandes().slice(0, 5);
  }
}
