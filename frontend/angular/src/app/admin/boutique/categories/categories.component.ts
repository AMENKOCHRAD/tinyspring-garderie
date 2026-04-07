import { Component, OnInit, ViewChild, TemplateRef, inject, ChangeDetectorRef } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { CategorieService } from 'src/app/services/boutique/categorie.service';
import { Categorie } from 'src/app/models/boutique/categorie.model';

const BASE_URL = 'http://localhost:8081';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.scss']
})
export class AdminCategoriesComponent implements OnInit {
  private categorieService = inject(CategorieService);
  private modalService = inject(NgbModal);
  private cdr = inject(ChangeDetectorRef);

  @ViewChild('formModal') formModal!: TemplateRef<any>;
  @ViewChild('deleteModal') deleteModal!: TemplateRef<any>;

  readonly baseUrl = BASE_URL;

  categories: Categorie[] = [];
  selectedCategorie: Categorie | null = null;
  isEditMode = false;
  errorMsg = '';
  successMsg = '';
  searchQuery = '';

  get filteredCategories(): Categorie[] {
    const q = this.searchQuery.toLowerCase().trim();
    if (!q) return this.categories;
    return this.categories.filter(c =>
      c.nom.toLowerCase().includes(q) ||
      (c.description ?? '').toLowerCase().includes(q)
    );
  }

  selectedFile: File | null = null;
  imagePreview: string | null = null;

  form = new FormGroup({
    nom: new FormControl('', [Validators.required, Validators.minLength(2)]),
    description: new FormControl('')
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.errorMsg = '';
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
    this.selectedCategorie = null;
    this.selectedFile = null;
    this.imagePreview = null;
    this.form.reset();
    this.modalService.open(this.formModal, { size: 'md', centered: true });
  }

  openEdit(cat: Categorie): void {
    this.isEditMode = true;
    this.selectedCategorie = cat;
    this.selectedFile = null;
    this.imagePreview = cat.imageUrl ? `${BASE_URL}${cat.imageUrl}` : null;
    this.form.patchValue({ nom: cat.nom, description: cat.description });
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
    if (this.selectedFile) {
      fd.append('image', this.selectedFile);
    }

    const request$ = this.isEditMode
      ? this.categorieService.update(this.selectedCategorie!.id, fd)
      : this.categorieService.create(fd);

    request$.subscribe({
      next: () => {
        modal.close();
        this.successMsg = this.isEditMode ? 'Catégorie modifiée.' : 'Catégorie ajoutée.';
        this.cdr.detectChanges();
        this.loadCategories();
        setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
      },
      error: () => {
        this.errorMsg = 'Erreur lors de la sauvegarde.';
        this.cdr.detectChanges();
      }
    });
  }

  openDelete(cat: Categorie): void {
    this.selectedCategorie = cat;
    this.modalService.open(this.deleteModal, { size: 'sm', centered: true }).result.then(
      () => {
        this.categorieService.delete(cat.id).subscribe({
          next: () => {
            this.successMsg = 'Catégorie supprimée.';
            this.cdr.detectChanges();
            this.loadCategories();
            setTimeout(() => { this.successMsg = ''; this.cdr.detectChanges(); }, 3000);
          },
          error: (error) => {
            if (error.status === 409) {
              this.errorMsg = error.error?.message || 'Cette catégorie contient des produits, supprimez-les d\'abord.';
            } else if (error.status === 404) {
              this.errorMsg = 'Catégorie introuvable.';
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
