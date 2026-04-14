import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BoutiqueService } from '../shared/boutique.service';
import { CommandeDto } from '../shared/boutique.models';

interface TimelineStep {
  key: string;
  label: string;
}

@Component({
  selector: 'app-parent-boutique-order-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-order-detail-page.component.html',
  styleUrl: './parent-boutique-order-detail-page.component.css'
})
export class ParentBoutiqueOrderDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly timelineSteps: TimelineStep[] = [
    { key: 'PENDING', label: 'Commandee' },
    { key: 'CONFIRMEE', label: 'Confirmee' },
    { key: 'EXPEDIEE', label: 'Expediee' },
    { key: 'LIVREE', label: 'Livree' }
  ];

  protected readonly commande = signal<CommandeDto | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    const commandeId = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(commandeId) || commandeId <= 0) {
      this.errorMessage.set('Commande introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.boutiqueService.getCommandeById(commandeId).subscribe({
      next: (commande) => {
        this.commande.set(commande);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger cette commande.');
        this.isLoading.set(false);
      }
    });
  }

  protected getTimelineSteps(): TimelineStep[] {
    return this.timelineSteps;
  }

  protected getTimelineState(stepKey: string, statut: string): 'done' | 'active' | 'future' {
    const currentIndex = this.timelineSteps.findIndex((step) => step.key === statut);
    const stepIndex = this.timelineSteps.findIndex((step) => step.key === stepKey);

    if (currentIndex === -1) {
      return stepIndex === 0 ? 'active' : 'future';
    }

    if (stepIndex < currentIndex) {
      return 'done';
    }

    if (stepIndex === currentIndex) {
      return 'active';
    }

    return 'future';
  }

  protected getImageUrl(imageUrl: string | null | undefined): string {
    return this.boutiqueService.getImageUrl(imageUrl);
  }

  protected hasImage(imageUrl: string | null | undefined): boolean {
    return Boolean(imageUrl);
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
}
