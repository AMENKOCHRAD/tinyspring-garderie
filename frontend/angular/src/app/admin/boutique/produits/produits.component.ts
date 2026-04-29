import { ChangeDetectorRef, Component, OnInit, TemplateRef, ViewChild, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ProduitService } from 'src/app/services/boutique/produit.service';
import { CategorieService } from 'src/app/services/boutique/categorie.service';
import { NotificationService } from 'src/app/services/notification.service';
import { Produit } from 'src/app/models/boutique/produit.model';
import { Categorie } from 'src/app/models/boutique/categorie.model';
import { SpringPage, createEmptySpringPage } from 'src/app/models/boutique/spring-page.model';

const BASE_URL = 'http://localhost:8081';

@Component({
  selector: 'app-admin-produits',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './produits.component.html',
  styleUrls: ['./produits.component.scss']
})
export class AdminProduitsComponent implements OnInit {
  private readonly pageSize = 10;
  private produitService = inject(ProduitService);
  private categorieService = inject(CategorieService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);
  private notifService = inject(NotificationService);

  @ViewChild('formModal') formModal!: TemplateRef<any>;
  @ViewChild('deleteModal') deleteModal!: TemplateRef<any>;

  readonly baseUrl = BASE_URL;

  produits: Produit[] = [];
  filteredProduits: Produit[] = [];
  produitsPage: SpringPage<Produit> = createEmptySpringPage<Produit>(this.pageSize);
  categories: Categorie[] = [];
  selectedProduit: Produit | null = null;
  isEditMode = false;
  errorMsg = '';
  successMsg = '';
  filterCategorieId: number | null = null;
  searchQuery = '';
  lowStockProduits: Produit[] = [];

  selectedFile: File | null = null;
  imagePreview: string | null = null;

  form = new FormGroup({
    nom: new FormControl('', [Validators.required, Validators.minLength(2)]),
    description: new FormControl(''),
    prix: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    stock: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    categorieId: new FormControl<number | null>(null, [Validators.required])
  });

  ngOnInit(): void {
    this.loadCategories();
    this.loadProduitStats();
    this.loadProduits(0);
  }

  loadCategories(): void {
    this.categorieService.getAllAdmin().subscribe({
      next: (data) => {
        this.categories = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Erreur lors du chargement des categories.';
        this.cdr.detectChanges();
      }
    });
  }

  loadProduitStats(): void {
    this.produitService.getAllAdmin().subscribe({
      next: (data) => {
        this.produits = data;
        this.lowStockProduits = data.filter((produit) => produit.stock <= (produit.seuilAlerte ?? 3));
        this.notifService.lowStockProduits.set(this.lowStockProduits);
        this.notifService.lowStockCount.set(this.lowStockProduits.length);
        this.cdr.detectChanges();
      },
      error: () => {
        this.produits = [];
        this.lowStockProduits = [];
        this.notifService.lowStockProduits.set([]);
        this.notifService.lowStockCount.set(0);
        this.cdr.detectChanges();
      }
    });
  }

  loadProduits(page = this.produitsPage.number): void {
    this.errorMsg = '';
    this.produitService.getAdminPage({
      page,
      size: this.pageSize,
      nom: this.searchQuery,
      categorieId: this.filterCategorieId
    }).subscribe({
      next: (data) => {
        if (data.totalPages > 0 && data.content.length === 0 && page >= data.totalPages) {
          this.loadProduits(data.totalPages - 1);
          return;
        }

        this.produitsPage = data;
        this.filteredProduits = data.content;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Erreur lors du chargement des produits.';
        this.cdr.detectChanges();
      }
    });
  }

  getStockCount(): number {
    return this.produits.filter((produit) => produit.stock > 0).length;
  }

  getRuptureCount(): number {
    return this.produits.filter((produit) => produit.stock === 0).length;
  }

  getLowStockList(): string {
    return this.lowStockProduits.map((produit) => produit.nom).join(', ');
  }

  get displayStart(): number {
    if (this.produitsPage.totalElements === 0 || this.filteredProduits.length === 0) {
      return 0;
    }

    return this.produitsPage.number * this.produitsPage.size + 1;
  }

  get displayEnd(): number {
    if (this.produitsPage.totalElements === 0 || this.filteredProduits.length === 0) {
      return 0;
    }

    return this.produitsPage.number * this.produitsPage.size + this.filteredProduits.length;
  }

  get visiblePages(): number[] {
    if (this.produitsPage.totalPages === 0) {
      return [];
    }

    let start = Math.max(0, this.produitsPage.number - 2);
    let end = Math.min(this.produitsPage.totalPages - 1, start + 4);

    start = Math.max(0, end - 4);

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
  }

  onSearchChange(query: string): void {
    this.searchQuery = query;
    this.loadProduits(0);
  }

  onFilterChange(): void {
    this.loadProduits(0);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.produitsPage.totalPages || page === this.produitsPage.number) {
      return;
    }

    this.loadProduits(page);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      const reader = new FileReader();
      reader.onload = () => (this.imagePreview = reader.result as string);
      reader.readAsDataURL(this.selectedFile);
    }
  }

  openAdd(): void {
    this.isEditMode = false;
    this.selectedProduit = null;
    this.selectedFile = null;
    this.imagePreview = null;
    this.form.reset();
    this.modalService.open(this.formModal, { size: 'md', centered: true });
  }

  openEdit(produit: Produit): void {
    this.isEditMode = true;
    this.selectedProduit = produit;
    this.selectedFile = null;
    this.imagePreview = produit.imageUrl ? `${BASE_URL}${produit.imageUrl}` : null;
    this.form.patchValue({
      nom: produit.nom,
      description: produit.description,
      prix: produit.prix,
      stock: produit.stock,
      categorieId: produit.categorieId
    });
    this.modalService.open(this.formModal, { size: 'md', centered: true });
  }

  save(modal: any): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const fd = new FormData();
    fd.append('nom', this.form.value.nom ?? '');
    fd.append('description', this.form.value.description ?? '');
    fd.append('prix', String(this.form.value.prix ?? 0));
    fd.append('stock', String(this.form.value.stock ?? 0));
    fd.append('categorieId', String(this.form.value.categorieId ?? ''));
    if (this.selectedFile) {
      fd.append('image', this.selectedFile);
    }

    const request$ = this.isEditMode
      ? this.produitService.update(this.selectedProduit!.id, fd)
      : this.produitService.create(fd);

    request$.subscribe({
      next: () => {
        modal.close();
        this.successMsg = this.isEditMode ? 'Produit modifie.' : 'Produit ajoute.';
        this.cdr.detectChanges();
        this.loadProduitStats();
        this.loadProduits(this.produitsPage.number);
        setTimeout(() => {
          this.successMsg = '';
          this.cdr.detectChanges();
        }, 3000);
      },
      error: () => {
        this.errorMsg = 'Erreur lors de la sauvegarde.';
        this.cdr.detectChanges();
      }
    });
  }

  openDelete(produit: Produit): void {
    this.selectedProduit = produit;
    this.modalService.open(this.deleteModal, { size: 'sm', centered: true }).result.then(
      () => {
        this.produitService.delete(+produit.id).subscribe({
          next: () => {
            this.successMsg = 'Produit supprime.';
            this.cdr.detectChanges();
            this.loadProduitStats();
            this.loadProduits(this.produitsPage.number);
            setTimeout(() => {
              this.successMsg = '';
              this.cdr.detectChanges();
            }, 3000);
          },
          error: (error) => {
            if (error.status === 409) {
              this.errorMsg = error.error?.message || 'Ce produit est lie a des commandes, supprimez-les d abord.';
            } else if (error.status === 404) {
              this.errorMsg = 'Produit introuvable.';
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
}
