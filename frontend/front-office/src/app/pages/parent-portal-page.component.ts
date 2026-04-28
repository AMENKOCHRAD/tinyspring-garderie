import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DataTableComponent } from '../components/data-table.component';
import { AuthService } from '../shared/auth.service';
import { parentChildrenColumns, parentChildrenRows, parentModules, parentOverview, parentPaymentsColumns, parentPaymentsRows, parentTransportColumns, parentTransportRows } from '../shared/site-data';

@Component({
  selector: 'app-parent-portal-page',
  standalone: true,
  imports: [CommonModule, RouterLink, DataTableComponent, FormsModule],
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

        <div class="bg-white rounded shadow-lg p-5 mb-5 border-left border-primary" style="border-left-width: 5px !important;" id="ai-recommender">
          <div class="d-flex align-items-center mb-3">
             <h3 class="mb-0 text-primary">✨ Inscription assistée par Intelligence Artificielle</h3>
          </div>
          <p class="text-muted fa-lg mb-4">Laissez notre algorithme exclusif d'équilibrage analyser nos classes pour vous proposer le meilleur environnement et groupe pour votre enfant selon ses préférences et l'affluence du groupe !</p>
          
          <div class="row">
            <div class="col-md-4 border-right pr-4">
              <div class="form-group">
                <label class="font-weight-bold">Âge de l'enfant *</label>
                <input class="form-control form-control-lg bg-light" type="number" [(ngModel)]="iaAge" placeholder="Ex: 4 ans">
              </div>
              <div class="form-group mt-3">
                <label class="font-weight-bold">Langue Principale Préférée</label>
                <select class="form-control form-control-lg bg-light" [(ngModel)]="iaLangue">
                  <option value="Français">Français</option>
                  <option value="Anglais">Anglais</option>
                  <option value="Arabe">Arabe</option>
                </select>
              </div>
              <button class="btn btn-primary btn-lg w-100 mt-4 shadow-sm" [disabled]="!iaAge" (click)="getSuggestions()">
                 <span *ngIf="!isSuggesting">Lancer la répartition Automatique</span>
                 <span *ngIf="isSuggesting">Recherche experte...</span>
              </button>
            </div>
            
            <div class="col-md-8 pl-4">
               <div *ngIf="isSuggesting" class="text-center py-5">
                  <div class="spinner-grow text-primary" role="status" style="width: 3rem; height: 3rem;"></div>
                  <h5 class="mt-3 text-primary">Analyse cognitive et répartition des charges en cours...</h5>
                  <p class="text-muted">Calcul du quotient d'affinité pour une pédagogie parfaite.</p>
               </div>
               
               <div *ngIf="!isSuggesting && suggestions.length > 0" class="row">
                  <h5 class="col-12 mb-3 text-success">🏆 Top 3 des recommandations d'équilibrage :</h5>
                  <div class="col-12 mb-3" *ngFor="let s of suggestions; let i = index">
                     <div class="card shadow-sm border-0" [ngClass]="{'bg-light border-left border-success': i === 0}" [style.border-left-width]="i === 0 ? '4px !important' : '0'">
                        <div class="card-body d-flex align-items-center p-3">
                           <div class="rounded-circle d-flex align-items-center justify-content-center text-white shadow font-weight-bold mr-4" 
                                [ngClass]="s.matchPercentage >= 80 ? 'bg-success' : 'bg-warning'" 
                                style="width: 70px; height: 70px; font-size: 1.4rem;">
                              {{ s.matchPercentage }}%
                           </div>
                           <div class="flex-grow-1">
                              <h4 class="mb-1 text-dark">{{ s.groupe.nom }} <span class="badge badge-info ml-2 shadow-sm">{{ s.availableSeats }} places libres</span></h4>
                              <p class="mb-0 mt-2 text-muted" style="font-size: 0.95rem;">
                                  <span class="mr-3" *ngFor="let reason of s.matchReasons">✔️ {{ reason }}</span>
                              </p>
                           </div>
                           <button class="btn btn-success px-4 rounded-pill shadow-sm">Affecter</button>
                        </div>
                     </div>
                  </div>
               </div>

               <div *ngIf="!isSuggesting && suggestions.length === 0 && hasSearched" class="alert alert-danger p-4 shadow-sm">
                  <h5>⚠️ Aucun groupe complètement adapté n'a été trouvé !</h5>
                  <p class="mb-0">Les statistiques indiquent que soit aucune classe ne gère cette tranche d'âge, soit que notre algorithme d'équilibrage des charges a détecté que tous les groupes de cet âge sont <strong>saturés à 100%</strong> de leur capacité.</p>
               </div>

               <div *ngIf="!hasSearched && !isSuggesting" class="h-100 d-flex flex-column align-items-center justify-content-center text-muted">
                    <h1 style="font-size: 5rem; opacity: 0.1;">🎯</h1>
                    <p class="mt-2 text-center">Entrez l'âge et la langue pour réveiller notre algorithme cognitif.</p>
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

  // Variables for IA Algorithme de Répartition
  iaAge: number | null = null;
  iaLangue: string = 'Français';
  isSuggesting: boolean = false;
  hasSearched: boolean = false;
  suggestions: any[] = [];

  constructor(
    private authService: AuthService,
    private router: Router,
    private http: HttpClient
  ) {}

  public getSuggestions(): void {
    if (!this.iaAge) return;

    this.isSuggesting = true;
    this.hasSearched = true;
    
    const requestPayload = { age: this.iaAge, languePrincipale: this.iaLangue };

    // Simulate cognitive thinking delay for wow factor
    setTimeout(() => {
      this.http.post<any[]>('http://localhost:8081/api/groupes/suggestions', requestPayload).subscribe({
        next: (data) => {
          this.suggestions = data;
          this.isSuggesting = false;
        },
        error: (err) => {
          console.error(err);
          this.isSuggesting = false;
        }
      });
    }, 1200);
  }

  /**
   * Logout user
   */
  protected logout(): void {
    this.authService.logout();
    void this.router.navigate(['/']);
  }
}
