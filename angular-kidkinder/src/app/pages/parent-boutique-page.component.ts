import { DOCUMENT, CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { BoutiqueService } from '../shared/boutique.service';
import { CategorieDto, ProduitDto } from '../shared/boutique.models';
import { CartService } from '../shared/cart.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-parent-boutique-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './parent-boutique-page.component.html',
  styleUrl: './parent-boutique-page.component.css'
})
export class ParentBoutiquePageComponent implements OnInit {
  private readonly document = inject(DOCUMENT);
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);

  protected readonly categories = signal<CategorieDto[]>([]);
  protected readonly produits = signal<ProduitDto[]>([]);
  protected readonly searchTerm = signal('');
  protected readonly selectedCategorieId = signal<number | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly cartCount = this.cartService.itemCount;
  protected readonly addingToCart = signal<Record<number, boolean>>({});
  protected readonly isGridRefreshing = signal(false);
  protected readonly skeletonCards = Array.from({ length: 6 });
  protected readonly selectedCategorie = computed(() =>
    this.categories().find((categorie) => categorie.id === this.selectedCategorieId()) ?? null
  );

  ngOnInit(): void {
    this.loadInitialData();

    if (this.cartCount() > 0) {
      this.toastService.info(`Vous avez ${this.cartCount()} articles dans votre panier`);
    }
  }

  protected onSearchSubmit(): void {
    this.loadProduits();
  }

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);

    if (!value.trim()) {
      this.loadProduits();
    }
  }

  protected selectCategorie(categorieId: number | null): void {
    this.selectedCategorieId.set(categorieId);
    this.loadProduits();
  }

  protected clearSearch(): void {
    this.searchTerm.set('');
    this.loadProduits();
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategorieId.set(null);
    this.loadProduits();
  }

  protected addToCart(produit: ProduitDto, sourceElement: HTMLElement): void {
    if (produit.stock <= 0) {
      this.toastService.error('Stock insuffisant pour ce produit');
      return;
    }

    const currentQuantity = this.cartService.getItemQuantity(produit.id);

    if (currentQuantity >= produit.stock) {
      this.toastService.error('Stock insuffisant pour ce produit');
      return;
    }

    this.addingToCart.update((state) => ({ ...state, [produit.id]: true }));
    this.cartService.addItem(produit);
    this.triggerFlyAnimation(sourceElement, produit.imageUrl);
    this.toastService.success(`${produit.nom} ajoute au panier !`);

    window.setTimeout(() => {
      this.addingToCart.update((state) => ({ ...state, [produit.id]: false }));
    }, 2000);
  }

  protected getImageUrl(imageUrl: string | null | undefined): string {
    return this.boutiqueService.getImageUrl(imageUrl);
  }

  protected hasImage(imageUrl: string | null | undefined): boolean {
    return Boolean(imageUrl);
  }

  protected isAdded(produitId: number): boolean {
    return Boolean(this.addingToCart()[produitId]);
  }

  protected isLowStock(stock: number): boolean {
    return stock > 0 && stock < 5;
  }

  private loadInitialData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      categories: this.boutiqueService.getCategories(),
      produits: this.boutiqueService.getProduits()
    }).subscribe({
      next: ({ categories, produits }) => {
        this.categories.set(categories);
        this.applyProductResults(produits);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger la boutique pour le moment.');
        this.toastService.error('Erreur lors du chargement de la boutique');
        this.isLoading.set(false);
      }
    });
  }

  private loadProduits(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const term = this.searchTerm().trim();
    const categorieId = this.selectedCategorieId();
    const request$ = term
      ? this.boutiqueService.searchProduits(term)
      : categorieId
        ? this.boutiqueService.getProduitsByCategorie(categorieId)
        : this.boutiqueService.getProduits();

    request$.subscribe({
      next: (produits) => {
        this.applyProductResults(
          term && categorieId ? produits.filter((produit) => produit.categorieId === categorieId) : produits
        );
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de mettre a jour les produits.');
        this.toastService.error('Erreur lors du chargement des produits');
        this.isLoading.set(false);
      }
    });
  }

  private applyProductResults(produits: ProduitDto[]): void {
    this.produits.set(produits);
    this.isGridRefreshing.set(true);
    window.setTimeout(() => this.isGridRefreshing.set(false), 320);
  }

  private triggerFlyAnimation(sourceElement: HTMLElement, imageUrl: string | null | undefined): void {
    const cartIcon = this.document.querySelector('[data-cart-icon="true"]') as HTMLElement | null;

    if (!cartIcon) {
      this.cartService.triggerArrivalAnimation();
      return;
    }

    const sourceRect = sourceElement.getBoundingClientRect();
    const targetRect = cartIcon.getBoundingClientRect();
    const flyElement = this.document.createElement('div');
    const flyX = targetRect.left + targetRect.width / 2 - (sourceRect.left + sourceRect.width / 2);
    const flyY = targetRect.top + targetRect.height / 2 - (sourceRect.top + sourceRect.height / 2);

    flyElement.className = 'cart-fly-ghost';
    flyElement.style.left = `${sourceRect.left + sourceRect.width / 2 - 24}px`;
    flyElement.style.top = `${sourceRect.top + sourceRect.height / 2 - 24}px`;
    flyElement.style.setProperty('--fly-x', `${flyX}px`);
    flyElement.style.setProperty('--fly-y', `${flyY}px`);

    if (imageUrl) {
      flyElement.style.backgroundImage = `url("${this.getImageUrl(imageUrl)}")`;
    } else {
      flyElement.textContent = '●';
    }

    this.document.body.appendChild(flyElement);

    window.setTimeout(() => {
      this.cartService.triggerArrivalAnimation();
      flyElement.remove();
    }, 600);
  }
}
