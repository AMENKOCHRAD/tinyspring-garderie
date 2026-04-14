import { Component, OnInit, OnDestroy, ViewChild, TemplateRef, inject, ChangeDetectorRef } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, Subscription, merge, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { CommandeService } from 'src/app/services/boutique/commande.service';
import { NotificationService } from 'src/app/services/notification.service';
import { Commande, CommandeItem, PaymentStatusCommande, StatutCommande } from 'src/app/models/boutique/commande.model';

type ActionStatut = Extract<StatutCommande, 'EXPEDIEE' | 'LIVREE' | 'ANNULEE'>;
type TimelineStatut = Extract<StatutCommande, 'PENDING' | 'CONFIRMEE' | 'EXPEDIEE' | 'LIVREE'>;
type TimelineState = 'done' | 'active' | 'pending';

interface ActionConfig {
  label: string;
  className: string;
}

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

  readonly statuts: StatutCommande[] = ['PENDING', 'CONFIRMEE', 'EXPEDIEE', 'LIVREE', 'ANNULEE'];
  readonly timelineStatuts: TimelineStatut[] = ['PENDING', 'CONFIRMEE', 'EXPEDIEE', 'LIVREE'];
  readonly transitions: Record<StatutCommande, ActionStatut[]> = {
    PENDING: ['ANNULEE'],
    CONFIRMEE: ['EXPEDIEE', 'ANNULEE'],
    EXPEDIEE: ['LIVREE', 'ANNULEE'],
    LIVREE: [],
    ANNULEE: []
  };

  readonly statutLabels: Record<StatutCommande, string> = {
    PENDING: 'En attente paiement',
    CONFIRMEE: 'Confirmee',
    EXPEDIEE: 'Expediee',
    LIVREE: 'Livree',
    ANNULEE: 'Annulee'
  };

  readonly paymentStatusLabels: Record<PaymentStatusCommande, string> = {
    PENDING: 'Non paye',
    PAID: 'Paye',
    FAILED: 'Paiement echoue',
    CANCELED: 'Rembourse'
  };

  readonly timelineLabels: Record<TimelineStatut, string> = {
    PENDING: 'Paiement',
    CONFIRMEE: 'Confirmee',
    EXPEDIEE: 'Expediee',
    LIVREE: 'Livree'
  };

  private loadTrigger$ = new Subject<void>();
  private subs = new Subscription();

  ngOnInit(): void {
    this.subs.add(
      merge(this.loadTrigger$, this.notifService.newOrderArrived$, interval(5_000))
        .pipe(switchMap(() => this.commandeService.getAll()))
        .subscribe({
          next: (data) => {
            this.commandes = data;
            this.applyFilter();
            if (this.selectedCommande) {
              this.selectedCommande = data.find((commande) => commande.id === this.selectedCommande?.id) ?? this.selectedCommande;
            }
            this.cdr.detectChanges();
          },
          error: () => {
            this.errorMsg = 'Erreur lors du chargement des commandes.';
            this.cdr.detectChanges();
          }
        })
    );

    this.loadCommandes();
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    this.loadTrigger$.complete();
  }

  loadCommandes(): void {
    this.errorMsg = '';
    this.loadTrigger$.next();
  }

  applyFilter(): void {
    this.filteredCommandes = this.filterStatut
      ? this.commandes.filter((commande) => commande.statut === this.filterStatut)
      : [...this.commandes];
  }

  onFilterChange(): void {
    this.applyFilter();
  }

  getPendingPaymentCount(): number {
    return this.commandes.filter((commande) => commande.statut === 'PENDING').length;
  }

  getDeliveredCount(): number {
    return this.commandes.filter((commande) => commande.statut === 'LIVREE').length;
  }

  getTotalItems(commande: Commande): number {
    return (commande.items ?? []).reduce((total, item) => total + Number(item.quantite ?? 0), 0);
  }

  getInitials(nom: string): string {
    return nom
      .split(' ')
      .map((part) => part.charAt(0).toUpperCase())
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

  getActionsDisponibles(commande: Commande): ActionStatut[] {
    return this.transitions[commande.statut] ?? [];
  }

  getActionConfig(statut: ActionStatut): ActionConfig {
    const config: Record<ActionStatut, ActionConfig> = {
      EXPEDIEE: { label: 'Marquer expediee', className: 'b-btn--shipping' },
      LIVREE: { label: 'Marquer livree', className: 'b-btn--delivered' },
      ANNULEE: { label: 'Annuler', className: 'b-btn--cancel' }
    };
    return config[statut];
  }

  openDetail(commande: Commande): void {
    this.selectedCommande = commande;
    this.modalService.open(this.detailModal, { size: 'xl', centered: true });
  }

  changeStatut(commande: Commande, statut: ActionStatut): void {
    this.errorMsg = '';

    this.commandeService.updateStatut(commande.id, statut).subscribe({
      next: (updated) => {
        this.replaceCommande(updated);
        this.successMsg = `Statut mis a jour : ${this.getShortStatutLabel(updated.statut)}`;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.successMsg = '';
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err) => {
        this.errorMsg = this.extractBackendErrorMessage(err);
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
            this.commandes = this.commandes.filter((currentCommande) => currentCommande.id !== commande.id);
            this.applyFilter();
            this.successMsg = 'Commande supprimee.';
            this.cdr.detectChanges();
            setTimeout(() => {
              this.successMsg = '';
              this.cdr.detectChanges();
            }, 3000);
          },
          error: (error) => {
            if (error.status === 409) {
              this.errorMsg = error.error?.message || 'Cette commande ne peut pas etre supprimee.';
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

  getStatutBadgeClass(statut: StatutCommande): string {
    const classes: Record<StatutCommande, string> = {
      PENDING: 'b-badge--pending',
      CONFIRMEE: 'b-badge--confirmee',
      EXPEDIEE: 'b-badge--expediee',
      LIVREE: 'b-badge--livree',
      ANNULEE: 'b-badge--annulee'
    };
    return classes[statut];
  }

  getPaymentBadgeClass(paymentStatus: PaymentStatusCommande): string {
    const classes: Record<PaymentStatusCommande, string> = {
      PENDING: 'b-payment-badge--pending',
      PAID: 'b-payment-badge--paid',
      FAILED: 'b-payment-badge--failed',
      CANCELED: 'b-payment-badge--canceled'
    };
    return classes[paymentStatus];
  }

  getTimelineState(step: TimelineStatut, commande: Commande): TimelineState {
    if (commande.statut === 'ANNULEE') {
      return step === 'PENDING' ? 'done' : 'pending';
    }

    const activeIndex = this.timelineStatuts.indexOf(commande.statut as TimelineStatut);
    const stepIndex = this.timelineStatuts.indexOf(step);

    if (stepIndex < activeIndex) {
      return 'done';
    }

    if (stepIndex === activeIndex) {
      return 'active';
    }

    return 'pending';
  }

  getStatutIconHtml(statut: StatutCommande): string {
    const icons: Record<StatutCommande, string> = {
      PENDING: '&#9203;',
      CONFIRMEE: '&#10003;',
      EXPEDIEE: '&#128666;',
      LIVREE: '&#9989;',
      ANNULEE: '&#10007;'
    };
    return icons[statut];
  }

  getActionIconHtml(statut: ActionStatut): string {
    const icons: Record<ActionStatut, string> = {
      EXPEDIEE: '&#128666;',
      LIVREE: '&#9989;',
      ANNULEE: '&#10007;'
    };
    return icons[statut];
  }

  getItemImageUrl(item: CommandeItem): string {
    if (!item.produitImageUrl) {
      return this.getPlaceholderDataUrl();
    }

    if (item.produitImageUrl.startsWith('http://') || item.produitImageUrl.startsWith('https://')) {
      return item.produitImageUrl;
    }

    return `http://localhost:8081${item.produitImageUrl}`;
  }

  onItemImageError(event: Event): void {
    const image = event.target as HTMLImageElement;
    image.src = this.getPlaceholderDataUrl();
  }

  trackByItem(_index: number, item: CommandeItem): string {
    return `${item.produitId}-${item.quantite}-${item.sousTotal}`;
  }

  private replaceCommande(updated: Commande): void {
    this.commandes = this.commandes.map((commande) => (commande.id === updated.id ? updated : commande));
    this.applyFilter();

    if (this.selectedCommande?.id === updated.id) {
      this.selectedCommande = updated;
    }
  }

  private getShortStatutLabel(statut: StatutCommande): string {
    const label = this.statutLabels[statut] ?? statut;
    return label.replace(/^[^A-Za-z]+/, '').trim();
  }

  private extractBackendErrorMessage(error: any): string {
    if (typeof error?.error === 'string' && error.error.trim()) {
      return error.error.trim();
    }

    if (typeof error?.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message.trim();
    }

    if (error?.status === 400) {
      return 'Transition interdite par le backend.';
    }

    return 'Erreur lors du changement de statut.';
  }

  private getPlaceholderDataUrl(): string {
    return 'data:image/svg+xml;utf8,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="#b8c1cc" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">' +
      '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>' +
      '<circle cx="8.5" cy="8.5" r="1.5"/>' +
      '<polyline points="21 15 16 10 5 21"/>' +
      '</svg>'
    );
  }
}
