import { Component, OnInit, OnDestroy, ViewChild, TemplateRef, inject, ChangeDetectorRef, HostListener } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, Subscription, merge, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { CommandeService } from 'src/app/services/boutique/commande.service';
import { NotificationService } from 'src/app/services/notification.service';
import { Commande, StatutCommande } from 'src/app/models/boutique/commande.model';

@Component({
  selector: 'app-admin-commandes',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './commandes.component.html',
  styleUrls: ['./commandes.component.scss']
})
export class AdminCommandesComponent implements OnInit, OnDestroy {
  private commandeService = inject(CommandeService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);
  private notifService = inject(NotificationService);

  @ViewChild('detailModal') detailModal!: TemplateRef<any>;
  @ViewChild('deleteModal') deleteModal!: TemplateRef<any>;

  commandes: Commande[] = [];
  filteredCommandes: Commande[] = [];
  selectedCommande: Commande | null = null;
  errorMsg = '';
  successMsg = '';
  filterStatut: StatutCommande | '' = '';

  readonly statuts: StatutCommande[] = ['EN_ATTENTE', 'CONFIRMEE', 'EXPEDIEE', 'LIVREE', 'ANNULEE'];

  // Subject interne qui déclenche un (re)chargement — switchMap annule toute requête en cours
  private loadTrigger$ = new Subject<void>();
  private subs = new Subscription();
  openStatutId: number | null = null;

  readonly statutLabels: Record<StatutCommande, string> = {
    EN_ATTENTE: 'En attente',
    CONFIRMEE: 'Confirmée',
    EXPEDIEE: 'Expédiée',
    LIVREE: 'Livrée',
    ANNULEE: 'Annulée'
  };

  @HostListener('document:click')
  closeStatutDropdown(): void { this.openStatutId = null; }

  toggleStatutDropdown(event: MouseEvent, id: number): void {
    event.stopPropagation();
    this.openStatutId = this.openStatutId === id ? null : id;
  }

  selectStatut(cmd: Commande, statut: StatutCommande): void {
    this.openStatutId = null;
    this.changeStatut(cmd, statut);
  }

  ngOnInit(): void {
    // Pipeline principal : une seule requête active à la fois grâce à switchMap
    this.subs.add(
      merge(
        this.loadTrigger$,
        this.notifService.newOrderArrived$,
        interval(5_000)   // rafraîchissement automatique toutes les 5s
      ).pipe(
        switchMap(() => this.commandeService.getAll())  // BUG 2 : annule les requêtes concurrentes
      ).subscribe({
        next: (data) => {
          console.log('[Commandes] réponse :', data);
          this.commandes = data;
          this.filteredCommandes = [...data];
          this.applyFilter();
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('[Commandes] erreur :', error);
          this.errorMsg = 'Erreur lors du chargement des commandes.';
          this.cdr.detectChanges();
        }
      })
    );

    // Chargement initial
    this.loadCommandes();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.loadTrigger$.complete();
  }

  loadCommandes(): void {
    console.log('[Commandes] loadCommandes appelé');
    this.errorMsg = '';
    this.loadTrigger$.next();  // déclenche le pipeline switchMap
  }

  applyFilter(): void {
    if (this.filterStatut) {
      this.filteredCommandes = this.commandes.filter((c) => c.statut === this.filterStatut);
    } else {
      this.filteredCommandes = [...this.commandes];
    }
  }

  getWaitingCount(): number {
    return this.commandes.filter(c => c.statut === 'EN_ATTENTE').length;
  }

  getDeliveredCount(): number {
    return this.commandes.filter(c => c.statut === 'LIVREE').length;
  }

  getInitials(nom: string): string {
    return nom
      .split(' ')
      .map(part => part.charAt(0).toUpperCase())
      .join('')
      .substring(0, 2);
  }

  getAvatarColor(nom: string): string {
    const firstLetter = nom.charAt(0).toUpperCase();
    if (firstLetter >= 'A' && firstLetter <= 'E') return 'blue';
    if (firstLetter >= 'F' && firstLetter <= 'J') return 'green';
    if (firstLetter >= 'K' && firstLetter <= 'O') return 'orange';
    if (firstLetter >= 'P' && firstLetter <= 'T') return 'purple';
    return 'red';
  }

  onFilterChange(): void {
    this.applyFilter();
  }

  openDetail(commande: Commande): void {
    this.selectedCommande = commande;
    this.modalService.open(this.detailModal, { size: 'lg', centered: true });
  }

  changeStatut(commande: Commande, statut: StatutCommande): void {
    this.commandeService.updateStatut(commande.id, statut).subscribe({
      next: (updated) => {
        commande.statut = updated.statut;
        this.successMsg = `Statut mis à jour : ${statut}`;
        this.cdr.detectChanges();
        setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: (err) => {
        this.errorMsg = err?.error?.message || 'Erreur lors du changement de statut.';
        this.cdr.detectChanges();
      }
    });
  }

  openDelete(commande: Commande): void {
    this.selectedCommande = commande;
    this.modalService.open(this.deleteModal, { size: 'sm', centered: true }).result.then(
      () => {
        this.commandeService.delete(commande.id).subscribe({
          next: () => {
            this.successMsg = 'Commande supprimée.';
            this.cdr.detectChanges();
            this.loadCommandes();
            setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
          },
          error: (error) => {
            if (error.status === 409) {
              this.errorMsg = error.error?.message || 'Cette commande ne peut pas être supprimée.';
            } else if (error.status === 404) {
              this.errorMsg = 'Commande introuvable.';
            } else {
              this.errorMsg = 'Erreur lors de la suppression.';
            }
            this.cdr.detectChanges();
          }
        });
      },
      () => {}
    );
  }

  statutBadgeClass(statut: StatutCommande): string {
    const map: Record<StatutCommande, string> = {
      EN_ATTENTE: 'bg-warning text-dark',
      CONFIRMEE: 'bg-primary',
      EXPEDIEE: 'bg-info',
      LIVREE: 'bg-success',
      ANNULEE: 'bg-danger'
    };
    return map[statut] ?? 'bg-secondary';
  }
}
