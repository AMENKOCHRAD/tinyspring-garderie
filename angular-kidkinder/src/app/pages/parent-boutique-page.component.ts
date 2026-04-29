import { DOCUMENT, CommonModule } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AffiniteService } from '../services/affinite.service';
import { BoutiqueService } from '../shared/boutique.service';
import { CategorieDto, PageResponse, ProduitDto } from '../shared/boutique.models';
import { CartService } from '../shared/cart.service';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-parent-boutique-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './parent-boutique-page.component.html',
  styleUrl: './parent-boutique-page.component.css'
})
export class ParentBoutiquePageComponent implements OnInit, OnDestroy {
  @ViewChild('catalogueResultsTop')
  private catalogueResultsTop?: ElementRef<HTMLElement>;

  private readonly document = inject(DOCUMENT);
  private readonly boutiqueService = inject(BoutiqueService);
  private readonly affiniteService = inject(AffiniteService);
  private readonly cartService = inject(CartService);
  private readonly toastService = inject(ToastService);
  private readonly hoverStartTimes = new Map<number, number>();
  private readonly pageSize = 9;

  private searchInteractionPending = false;
  private produitsRequestId = 0;
  private searchDebounceHandle: number | null = null;

  protected readonly categories = signal<CategorieDto[]>([]);
  protected readonly produitsPage = signal<PageResponse<ProduitDto> | null>(null);
  protected readonly produits = computed(() => this.produitsPage()?.content ?? []);
  protected readonly totalProduits = computed(() => this.produitsPage()?.totalElements ?? 0);
  protected readonly totalPages = computed(() => this.produitsPage()?.totalPages ?? 0);
  protected readonly currentPage = signal(0);
  protected readonly searchTerm = signal('');
  protected readonly selectedCategorieId = signal<number | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly cartCount = this.cartService.itemCount;
  protected readonly addingToCart = signal<Record<number, boolean>>({});
  protected readonly isGridRefreshing = signal(false);
  protected readonly skeletonCards = Array.from({ length: this.pageSize });
  protected readonly selectedCategorie = computed(() =>
    this.categories().find((categorie) => categorie.id === this.selectedCategorieId()) ?? null
  );
  protected readonly isFirstPage = computed(() => this.produitsPage()?.first ?? this.currentPage() === 0);
  protected readonly isLastPage = computed(() => this.produitsPage()?.last ?? true);
  protected readonly visiblePages = computed(() => {
    const totalPages = this.totalPages();

    if (totalPages <= 0) {
      return [];
    }

    const maxVisiblePages = 5;
    const currentPage = this.currentPage();
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages);

    startPage = Math.max(0, endPage - maxVisiblePages);

    return Array.from({ length: endPage - startPage }, (_, index) => startPage + index);
  });

  ngOnInit(): void {
    this.loadInitialData();

    if (this.cartCount() > 0) {
      this.toastService.info(`Vous avez ${this.cartCount()} articles dans votre panier`);
    }
  }

  ngOnDestroy(): void {
    this.hoverStartTimes.clear();
    this.clearSearchDebounce();
  }

  protected onSearchSubmit(): void {
    this.searchInteractionPending = Boolean(this.searchTerm().trim());
    this.currentPage.set(0);
    this.clearSearchDebounce();
    this.loadProduits(0);
  }

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(0);
    this.searchInteractionPending = Boolean(value.trim());

    this.clearSearchDebounce();
    this.searchDebounceHandle = window.setTimeout(() => {
      this.loadProduits(0);
      this.searchDebounceHandle = null;
    }, value.trim() ? 300 : 0);
  }

  protected selectCategorie(categorieId: number | null): void {
    this.selectedCategorieId.set(categorieId);
    this.currentPage.set(0);
    this.clearSearchDebounce();
    this.loadProduits(0);
  }

  protected clearSearch(): void {
    this.searchTerm.set('');
    this.searchInteractionPending = false;
    this.currentPage.set(0);
    this.clearSearchDebounce();
    this.loadProduits(0);
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategorieId.set(null);
    this.searchInteractionPending = false;
    this.currentPage.set(0);
    this.clearSearchDebounce();
    this.loadProduits(0);
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.clearSearchDebounce();
    this.scrollToCatalogueResults();
    this.loadProduits(page);
  }

  protected goToPreviousPage(): void {
    if (this.isFirstPage()) {
      return;
    }

    this.goToPage(this.currentPage() - 1);
  }

  protected goToNextPage(): void {
    if (this.isLastPage()) {
      return;
    }

    this.goToPage(this.currentPage() + 1);
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
    this.trackSearchInteractionIfNeeded(produit.id);
    this.affiniteService.envoyerInteraction(produit.id, 'AJOUT_PANIER').subscribe();
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

  protected onProduitMouseEnter(produitId: number): void {
    this.hoverStartTimes.set(produitId, Date.now());
  }

  protected onProduitMouseLeave(produitId: number): void {
    const startedAt = this.hoverStartTimes.get(produitId);
    this.hoverStartTimes.delete(produitId);

    if (!startedAt) {
      return;
    }

    const durationMs = Date.now() - startedAt;

    if (durationMs >= 30000) {
      this.affiniteService.envoyerInteraction(produitId, 'VUE_30S').subscribe();
      return;
    }

    if (durationMs >= 10000) {
      this.affiniteService.envoyerInteraction(produitId, 'VUE_10S').subscribe();
      return;
    }

    if (durationMs >= 3000) {
      this.affiniteService.envoyerInteraction(produitId, 'VUE_3S').subscribe();
    }
  }

  protected onDetailClick(produitId: number): void {
    this.trackSearchInteractionIfNeeded(produitId);
    this.affiniteService.envoyerInteraction(produitId, 'CLIC_DETAIL').subscribe();
  }

  private loadInitialData(): void {
    const requestId = ++this.produitsRequestId;

    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      categories: this.boutiqueService.getCategories(),
      produitsPage: this.boutiqueService.getProduits({
        page: 0,
        size: this.pageSize
      })
    }).subscribe({
      next: ({ categories, produitsPage }) => {
        if (requestId !== this.produitsRequestId) {
          return;
        }

        this.categories.set(categories);
        this.applyProductResults(produitsPage);
        this.isLoading.set(false);
      },
      error: () => {
        if (requestId !== this.produitsRequestId) {
          return;
        }

        this.errorMessage.set('Impossible de charger la boutique pour le moment.');
        this.toastService.error('Erreur lors du chargement de la boutique');
        this.isLoading.set(false);
      }
    });
  }

  private loadProduits(page: number): void {
    const requestId = ++this.produitsRequestId;

    this.isLoading.set(true);
    this.errorMessage.set('');

    const term = this.searchTerm().trim();
    const categorieId = this.selectedCategorieId();

    this.boutiqueService.getProduits({
      page,
      size: this.pageSize,
      nom: term || undefined,
      categorieId
    }).subscribe({
      next: (produitsPage) => {
        if (requestId !== this.produitsRequestId) {
          return;
        }

        this.applyProductResults(produitsPage);
        this.isLoading.set(false);
      },
      error: () => {
        if (requestId !== this.produitsRequestId) {
          return;
        }

        this.errorMessage.set('Impossible de mettre a jour les produits.');
        this.toastService.error('Erreur lors du chargement des produits');
        this.isLoading.set(false);
      }
    });
  }

  private applyProductResults(produitsPage: PageResponse<ProduitDto>): void {
    this.produitsPage.set(produitsPage);
    this.currentPage.set(produitsPage.number);
    this.isGridRefreshing.set(true);
    window.setTimeout(() => this.isGridRefreshing.set(false), 320);
  }

  private trackSearchInteractionIfNeeded(produitId: number): void {
    if (!this.searchInteractionPending || !this.searchTerm().trim()) {
      return;
    }

    this.searchInteractionPending = false;
    this.affiniteService.envoyerInteraction(produitId, 'RECHERCHE').subscribe();
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

  private scrollToCatalogueResults(): void {
    if (this.catalogueResultsTop) {
      this.catalogueResultsTop.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
      return;
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  private clearSearchDebounce(): void {
    if (this.searchDebounceHandle === null) {
      return;
    }

    window.clearTimeout(this.searchDebounceHandle);
    this.searchDebounceHandle = null;
  }
}
