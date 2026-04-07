import { Component, OnInit, ViewChild, TemplateRef, inject, ChangeDetectorRef } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ProduitService } from 'src/app/services/boutique/produit.service';
import { CategorieService } from 'src/app/services/boutique/categorie.service';
import { NotificationService } from 'src/app/services/notification.service';
import { Produit } from 'src/app/models/boutique/produit.model';
import { Categorie } from 'src/app/models/boutique/categorie.model';

const BASE_URL = 'http://localhost:8081';

@Component({
  selector: 'app-admin-produits',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './produits.component.html',
  styleUrls: ['./produits.component.scss']
})
export class AdminProduitsComponent implements OnInit {
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
  categories: Categorie[] = [];
  selectedProduit: Produit | null = null;
  isEditMode = false;
  errorMsg = '';
  successMsg = '';
  filterCategorieId: number | null = null;
  searchQuery = '';
  lowStockProduits: any[] = [];

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
    this.loadProduits();
  }

  loadCategories(): void {
    this.categorieService.getAll().subscribe({
      next: (data) => {
        this.categories = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Erreur lors du chargement des catégories.';
        this.cdr.detectChanges();
      }
    });
  }

  loadProduits(): void {
    this.errorMsg = '';
    this.produitService.getAll().subscribe({
      next: (data) => {
        this.produits = data;
        this.applyFilter();
        this.lowStockProduits = data.filter(p => p.stock <= (p.seuilAlerte ?? 3));
        this.notifService.lowStockProduits.set(this.lowStockProduits);
        this.notifService.lowStockCount.set(this.lowStockProduits.length);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Erreur lors du chargement des produits.';
        this.cdr.detectChanges();
      }
    });
  }

  applyFilter(): void {
    let result = [...this.produits];
    if (this.filterCategorieId) {
      result = result.filter(p => p.categorieId === Number(this.filterCategorieId));
    }
    const q = this.searchQuery.toLowerCase().trim();
    if (q) {
      result = result.filter(p => p.nom.toLowerCase().includes(q));
    }
    this.filteredProduits = result;
  }

  getStockCount(): number {
    return this.produits.filter(p => p.stock > 0).length;
  }

  getRuptureCount(): number {
    return this.produits.filter(p => p.stock === 0).length;
  }

  getLowStockList(): string {
    return this.lowStockProduits.map(p => p.nom).join(', ');
  }

  onFilterChange(): void {
    this.applyFilter();
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
        this.successMsg = this.isEditMode ? 'Produit modifié.' : 'Produit ajouté.';
        this.cdr.detectChanges();
        this.loadProduits();
        setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
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
            this.successMsg = 'Produit supprimé.';
            this.cdr.detectChanges();
            this.loadProduits();
            setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
          },
          error: (error) => {
            if (error.status === 409) {
              this.errorMsg = error.error?.message || 'Ce produit est lié à des commandes, supprimez-les d\'abord.';
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
