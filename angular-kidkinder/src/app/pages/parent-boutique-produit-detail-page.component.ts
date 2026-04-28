import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AffiniteService } from '../services/affinite.service';
import { BoutiqueService } from '../shared/boutique.service';
import { ProduitDto } from '../shared/boutique.models';

@Component({
  selector: 'app-parent-boutique-produit-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-produit-detail-page.component.html',
  styleUrl: './parent-boutique-produit-detail-page.component.css'
})
export class ParentBoutiqueProduitDetailPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly affiniteService = inject(AffiniteService);
  private readonly timeoutIds: number[] = [];

  protected readonly produit = signal<ProduitDto | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');

  ngOnInit(): void {
    const produitId = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(produitId) || produitId <= 0) {
      this.errorMessage.set('Produit introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.startInteractionTimers(produitId);

    this.boutiqueService.getProduitById(produitId).subscribe({
      next: (produit) => {
        this.produit.set(produit);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger ce produit.');
        this.isLoading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.timeoutIds.forEach((timeoutId) => window.clearTimeout(timeoutId));
    this.timeoutIds.length = 0;
  }

  protected getImageUrl(imageUrl: string | null | undefined): string {
    return this.boutiqueService.getImageUrl(imageUrl);
  }

  protected hasImage(imageUrl: string | null | undefined): boolean {
    return Boolean(imageUrl);
  }

  protected isLowStock(stock: number): boolean {
    return stock > 0 && stock < 5;
  }

  private startInteractionTimers(produitId: number): void {
    this.timeoutIds.push(
      window.setTimeout(() => this.affiniteService.envoyerInteraction(produitId, 'VUE_3S').subscribe(), 3000),
      window.setTimeout(() => this.affiniteService.envoyerInteraction(produitId, 'VUE_10S').subscribe(), 10000),
      window.setTimeout(() => this.affiniteService.envoyerInteraction(produitId, 'VUE_30S').subscribe(), 30000)
    );
  }
}
