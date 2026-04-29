import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { BoutiqueService } from '../shared/boutique.service';
import { CommandeDto } from '../shared/boutique.models';

interface OrderStatusMeta {
  className: string;
  label: string;
}

@Component({
  selector: 'app-parent-boutique-orders-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-orders-page.component.html',
  styleUrl: './parent-boutique-orders-page.component.css'
})
export class ParentBoutiqueOrdersPageComponent implements OnInit {
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly authService = inject(AuthService);

  protected readonly commandes = signal<CommandeDto[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly skeletonCards = Array.from({ length: 3 });

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();

    if (!user) {
      this.errorMessage.set('Votre session est introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.boutiqueService.getMesCommandes(user.userId).subscribe({
      next: (commandes) => {
        this.commandes.set(commandes);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos commandes.');
        this.isLoading.set(false);
      }
    });
  }

  protected getStatusMeta(statut: string): OrderStatusMeta {
    switch (statut) {
      case 'PENDING':
        return { className: 'status-badge status-badge--pending', label: 'En attente de paiement' };
      case 'CONFIRMEE':
        return { className: 'status-badge status--info', label: 'Confirmee' };
      case 'EXPEDIEE':
        return { className: 'status-badge status--violet', label: 'Expediee' };
      case 'LIVREE':
        return { className: 'status-badge status--success', label: 'Livree' };
      case 'ANNULEE':
        return { className: 'status-badge status--danger', label: 'Annulee' };
      default:
        return { className: 'status-badge status--mint', label: statut };
    }
  }

  protected formatDate(dateCommande: string): string {
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(dateCommande));
  }

  protected getImageUrl(imageUrl: string | null | undefined): string {
    return this.boutiqueService.getImageUrl(imageUrl);
  }
}
