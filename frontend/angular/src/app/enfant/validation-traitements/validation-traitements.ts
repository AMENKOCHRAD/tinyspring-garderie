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

  private service = inject(ValidationTraitementsService);

  traitements: any[] = [];
  traitementsFiltres: any[] = [];
  traitementSelectionne: any = null;

  isLoading = false;
  error = '';
  searchTerm = '';

  ngOnInit(): void {
    this.chargerTraitements();
  }

  chargerTraitements() {
    this.isLoading = false;

    this.service.getTraitementsEnAttente().subscribe({
      next: (data) => {
        this.traitements = data || [];
        this.filtrer();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement';
        this.isLoading = false;
      }
    });
  }

  filtrer() {
    const t = this.searchTerm.toLowerCase();

    this.traitementsFiltres = this.traitements.filter(tr =>
      tr.nomEnfant?.toLowerCase().includes(t) ||
      tr.prenomEnfant?.toLowerCase().includes(t) ||
      tr.nomParent?.toLowerCase().includes(t) ||
      tr.nomTraitement?.toLowerCase().includes(t)
    );
  }

  consulter(id: number) {
    this.service.consulterTraitement(id).subscribe(data => {
      this.traitementSelectionne = data;
    });
  }

  accepter(id: number) {
    this.service.validerTraitement(id).subscribe(() => {
      this.traitements = this.traitements.filter(t => t.traitementId !== id);
      this.filtrer();
      this.traitementSelectionne = null;
    });
  }

  refuser(traitement: any) {
    alert("Traitement non validé");
  }

  fermerDetails() {
    this.traitementSelectionne = null;
  }

  
  isTraitementSensible(tr: any): boolean {
    if ((tr.typeCondition || '').toLowerCase() === 'chronique') return true;

    const txt = (tr.nomTraitement + ' ' + tr.description).toLowerCase();

    return txt.includes('asthme') ||
           txt.includes('insuline') ||
           txt.includes('allergie');
  }
}