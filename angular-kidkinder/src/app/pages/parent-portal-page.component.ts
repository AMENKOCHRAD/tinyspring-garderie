import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DataTableComponent } from '../components/data-table.component';
import { AuthService } from '../shared/auth.service';
import { parentChildrenColumns, parentChildrenRows, parentModules, parentOverview, parentPaymentsColumns, parentPaymentsRows, parentTransportColumns, parentTransportRows } from '../shared/site-data';

@Component({
  selector: 'app-parent-portal-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DataTableComponent],
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
            <button type="button" class="btn btn-danger" (click)="logout()">Déconnexion</button>
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

        <div class="row mb-4">
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

        <div class="row">
          <div class="col-lg-7 mb-4">
            <h4 class="mb-3">Mes enfants</h4>
            <app-data-table [columns]="childrenColumns" [rows]="childrenRows"></app-data-table>
          </div>
          <div class="col-lg-5 mb-4">
            <h4 class="mb-3">Paiements</h4>
            <app-data-table [columns]="paymentsColumns" [rows]="paymentsRows"></app-data-table>
          </div>
        </div>

        <div class="row mb-4">
          <div class="col-lg-7 mb-4">
            <h4 class="mb-3">Demandes de transport</h4>
            <app-data-table [columns]="transportColumns" [rows]="transportRows"></app-data-table>
          </div>
          <div class="col-lg-5 mb-4">
            <div class="bg-white rounded shadow-sm p-4 h-100">
              <h4 class="mb-3">Nouvelle demande de transport</h4>
              <p>Le parent choisit un enfant, un trajet et un horaire. La demande est enregistree avec le statut <strong>En attente</strong>, puis l admin l accepte ou la refuse.</p>
              <form>
                <div class="form-group">
                  <label>Enfant</label>
                  <select class="form-control">
                    <option>Lina Ben Salah</option>
                    <option>Adam Ben Salah</option>
                  </select>
                </div>
                <div class="form-group">
                  <label>Trajet</label>
                  <input class="form-control" type="text" value="Lac 1 -> Garderie">
                </div>
                <div class="form-group">
                  <label>Horaire</label>
                  <input class="form-control" type="time" value="07:30">
                </div>
                <button class="btn btn-primary" type="button">Envoyer la demande</button>
              </form>
            </div>
          </div>
        </div>

        <div class="bg-white rounded shadow-sm p-4">
          <h4 class="mb-3">Actions rapides</h4>
          <div class="row">
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Mettre a jour le dossier</h5>
                <p class="mb-0">Modifier les informations parent et enfant.</p>
              </div>
            </div>
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Telecharger une facture</h5>
                <p class="mb-0">Acceder a l historique administratif.</p>
              </div>
            </div>
            <div class="col-md-4 mb-3">
              <div class="border rounded p-4 h-100">
                <h5>Contacter la garderie</h5>
                <p class="mb-0">Envoyer un message au secretariat.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ParentPortalPageComponent {
  protected readonly overview = parentOverview;
  protected readonly modules = parentModules;
  protected readonly childrenColumns = parentChildrenColumns;
  protected readonly childrenRows = parentChildrenRows;
  protected readonly paymentsColumns = parentPaymentsColumns;
  protected readonly paymentsRows = parentPaymentsRows;
  protected readonly transportColumns = parentTransportColumns;
  protected readonly transportRows = parentTransportRows;

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
