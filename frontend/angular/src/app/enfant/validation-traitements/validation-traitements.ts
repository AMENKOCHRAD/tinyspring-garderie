import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ValidationTraitementsService } from 'src/app/services/validation-traitements';

@Component({
  selector: 'app-validation-traitements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validation-traitements.html',
  styleUrls: ['./validation-traitements.scss']
})
export class ValidationTraitementsComponent implements OnInit {
  private validationService = inject(ValidationTraitementsService);

  traitements: any[] = [];
  traitementsFiltres: any[] = [];
  traitementSelectionne: any = null;

  isLoading = false;
  error = '';
  searchTerm = '';

  ngOnInit(): void {
    this.chargerTraitements();
  }

  chargerTraitements(): void {
    this.isLoading = true;
    this.error = '';

    this.validationService.getTraitementsEnAttente().subscribe({
      next: (data) => {
        this.traitements = data || [];
        this.filtrerTraitements();
        this.isLoading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement des traitements en attente';
        this.isLoading = false;
      }
    });
  }

  filtrerTraitements(): void {
    const terme = this.searchTerm.toLowerCase().trim();

    if (!terme) {
      this.traitementsFiltres = [...this.traitements];
      return;
    }

    this.traitementsFiltres = this.traitements.filter((t) =>
      (t.nomEnfant || '').toLowerCase().includes(terme) ||
      (t.prenomEnfant || '').toLowerCase().includes(terme) ||
      (t.nomParent || '').toLowerCase().includes(terme) ||
      (t.nomTraitement || '').toLowerCase().includes(terme)
    );
  }

  consulter(traitementId: number): void {
    this.validationService.consulterTraitement(traitementId).subscribe({
      next: (data) => {
        this.traitementSelectionne = data;
      },
      error: () => {
        this.error = 'Erreur lors de la consultation du traitement';
      }
    });
  }

  accepter(traitementId: number): void {
    this.validationService.validerTraitement(traitementId).subscribe({
      next: () => {
        this.traitements = this.traitements.filter(t => t.traitementId !== traitementId);
        this.filtrerTraitements();

        if (this.traitementSelectionne?.traitementId === traitementId) {
          this.traitementSelectionne = null;
        }
      },
      error: () => {
        this.error = 'Erreur lors de la validation du traitement';
      }
    });
  }

  refuser(traitement: any): void {
    alert(`Traitement "${traitement.nomTraitement}" non validé. Son statut reste en attente.`);
  }

  fermerDetails(): void {
    this.traitementSelectionne = null;
  }
}