import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { BoutiqueService } from '../shared/boutique.service';
import { CartItem } from '../shared/boutique.models';
import { CartService } from '../shared/cart.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-parent-boutique-cart-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './parent-boutique-cart-page.component.html',
  styleUrl: './parent-boutique-cart-page.component.css'
})
export class ParentBoutiqueCartPageComponent implements OnInit {
  private readonly cartService = inject(CartService);
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly items = this.cartService.items;
  protected readonly itemCount = this.cartService.itemCount;
  protected readonly totalPrice = this.cartService.totalPrice;
  protected readonly currentUser = computed(() => this.authService.getCurrentUser());
  protected readonly removingItemIds = signal<Record<number, boolean>>({});
  protected readonly isSubmitting = computed(() => this.loading);

  protected adresse = '';
  protected errorMessage = '';
  protected loading = false;

  ngOnInit(): void {
    if (this.itemCount() > 0) {
      this.toastService.info(`Vous avez ${this.itemCount()} articles dans votre panier`);
    }
  }

  protected increaseQty(item: CartItem): void {
    const currentItem = this.items().find((cartItem) => cartItem.produit.id === item.produit.id);

    if (!currentItem) {
      return;
    }

    if (currentItem.quantite >= currentItem.produit.stock) {
      this.toastService.error('Stock insuffisant pour ce produit');
      return;
    }

    this.cartService.updateQuantity(currentItem.produit.id, currentItem.quantite + 1);
  }

  protected decreaseQty(item: CartItem): void {
    const currentItem = this.items().find((cartItem) => cartItem.produit.id === item.produit.id);

    if (!currentItem) {
      return;
    }

    if (currentItem.quantite <= 1) {
      this.remove(currentItem.produit.id);
      return;
    }

    this.cartService.updateQuantity(currentItem.produit.id, currentItem.quantite - 1);
  }

  protected remove(produitId: number): void {
    this.removingItemIds.update((state) => ({ ...state, [produitId]: true }));

    const timeoutId = window.setTimeout(() => {
      this.cartService.removeItem(produitId);
      this.removingItemIds.update((state) => ({ ...state, [produitId]: false }));
    }, 240);

    this.destroyRef.onDestroy(() => window.clearTimeout(timeoutId));
  }

  protected clearCart(): void {
    this.cartService.clear();
    this.toastService.success('Panier vide');
  }

  protected getImageUrl(imageUrl: string | null | undefined): string {
    return this.boutiqueService.getImageUrl(imageUrl);
  }

  protected hasImage(imageUrl: string | null | undefined): boolean {
    return Boolean(imageUrl);
  }

  protected isRemoving(produitId: number): boolean {
    return Boolean(this.removingItemIds()[produitId]);
  }

  protected async checkout(): Promise<void> {
    if (!this.adresse || this.adresse.trim().length < 10) {
      this.errorMessage = 'Veuillez saisir une adresse de livraison valide.';
      this.toastService.error(this.errorMessage);
      return;
    }

    if (this.cartService.getItems().length === 0) {
      this.errorMessage = 'Votre panier est vide.';
      this.toastService.error(this.errorMessage);
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    try {
      const userId = this.authService.getCurrentUser()?.userId;
      if (!userId) {
        throw new Error('Utilisateur non connecte');
      }

      const request = {
        adresseLivraison: this.adresse.trim(),
        userId,
        items: this.cartService.getItems().map((item: CartItem) => ({
          produitId: Number(item.produit.id),
          quantite: Number(item.quantite)
        }))
      };

      console.log('[Cart] Payload:', JSON.stringify(request, null, 2));

      const commande = await this.boutiqueService.createCommande(request).toPromise();
      console.log('[Cart] Commande creee:', commande);

      if (!commande?.id) {
        throw new Error('Commande creee sans identifiant');
      }

      const session = await this.boutiqueService.createCheckoutSession(commande.id).toPromise();
      console.log('[Cart] Session Stripe:', session);

      if (!session?.checkoutUrl) {
        throw new Error('Session Stripe invalide');
      }

      this.cartService.clear();
      window.location.href = session.checkoutUrl;
    } catch (error: unknown) {
      console.error('[Cart] Erreur:', error);
      this.errorMessage =
        (error as { error?: { message?: string } })?.error?.message ||
        (error as { message?: string })?.message ||
        'Erreur lors du paiement';
      this.toastService.error(this.errorMessage);
      this.loading = false;
      return;
    }

    this.loading = false;
  }
}
