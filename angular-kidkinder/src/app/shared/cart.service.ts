import { Injectable, computed, signal } from '@angular/core';
import { CartItem, ProduitDto } from './boutique.models';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly storageKey = 'cart';
  private readonly itemsSignal = signal<CartItem[]>(this.getItemsFromStorage());
  private readonly mutationTickSignal = signal(0);
  private readonly arrivalTickSignal = signal(0);

  readonly items = computed(() => this.itemsSignal());
  readonly itemCount = computed(() => this.getItemCount());
  readonly totalPrice = computed(() => this.getTotal());
  readonly mutationTick = computed(() => this.mutationTickSignal());
  readonly arrivalTick = computed(() => this.arrivalTickSignal());

  addItem(produit: ProduitDto): void {
    this.itemsSignal.update((items) => {
      const existingItem = items.find((item) => item.produit.id === produit.id);

      if (existingItem) {
        return items.map((item) =>
          item.produit.id === produit.id ? { ...item, quantite: item.quantite + 1 } : item
        );
      }

      return [...items, { produit, quantite: 1 }];
    });

    this.persist();
    this.bumpMutation();
  }

  updateQuantity(produitId: number, quantite: number): void {
    if (quantite <= 0) {
      this.removeItem(produitId);
      return;
    }

    this.itemsSignal.update((items) =>
      items.map((item) =>
        item.produit.id === produitId ? { ...item, quantite } : item
      )
    );

    this.persist();
    this.bumpMutation();
  }

  removeItem(produitId: number): void {
    this.itemsSignal.update((items) => items.filter((item) => item.produit.id !== produitId));
    this.persist();
    this.bumpMutation();
  }

  clear(): void {
    this.itemsSignal.set([]);
    localStorage.removeItem(this.storageKey);
    this.bumpMutation();
  }

  getItems(): CartItem[] {
    return this.itemsSignal().map((item) => ({
      produit: {
        ...item.produit,
        id: Number(item.produit.id)
      },
      quantite: Number(item.quantite)
    }));
  }

  getTotal(): number {
    return this.itemsSignal().reduce((total, item) => total + item.produit.prix * item.quantite, 0);
  }

  getItemCount(): number {
    return this.itemsSignal().reduce((total, item) => total + item.quantite, 0);
  }

  getItemQuantity(produitId: number): number {
    return this.itemsSignal().find((item) => item.produit.id === produitId)?.quantite ?? 0;
  }

  triggerArrivalAnimation(): void {
    this.arrivalTickSignal.update((value) => value + 1);
  }

  private getItemsFromStorage(): CartItem[] {
    const raw = localStorage.getItem(this.storageKey);

    if (!raw) {
      return [];
    }

    try {
      const items = JSON.parse(raw) as Array<CartItem & { quantity?: number }>;

      if (!Array.isArray(items)) {
        return [];
      }

      return items
        .map((item) => {
          const produitId = Number(item?.produit?.id);
          const quantite = Number(item?.quantite ?? item?.quantity ?? 0);

          if (!Number.isFinite(produitId) || produitId <= 0 || !Number.isFinite(quantite) || quantite <= 0) {
            return null;
          }

          return {
            produit: {
              ...item.produit,
              id: produitId
            } as ProduitDto,
            quantite
          };
        })
        .filter((item): item is CartItem => item !== null);
    } catch {
      localStorage.removeItem(this.storageKey);
      return [];
    }
  }

  private persist(): void {
    localStorage.setItem(this.storageKey, JSON.stringify(this.itemsSignal()));
  }

  private bumpMutation(): void {
    this.mutationTickSignal.update((value) => value + 1);
  }
}
