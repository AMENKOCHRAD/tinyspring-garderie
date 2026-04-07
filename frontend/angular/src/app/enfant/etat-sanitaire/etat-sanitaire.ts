import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { EtatSanitaireService } from 'src/app/services/etat-sanitaire';

interface ParentInfo {
  nom?: string;
  email?: string;
}

interface Enfant {
  id?: number;
  enfantId?: number;
  nom: string;
  prenom: string;
  dateNaissance?: string;
  contactUrgence?: string;
  parent?: ParentInfo;
}

interface ConditionSanitaire {
  id?: number;
  nomCondition: string;
  type: string;
  description?: string;
  dateDebut?: string;
  dateFin?: string;
  enfant?: any;
}

interface Traitement {
  id?: number;
  nomTraitement: string;
  description?: string;
  ordonnance?: string;
  dateDebut?: string;
  dateFin?: string;
  heuresPrises?: string[];
  statut?: string;
}

interface EnfantEtatSanitaire {
  enfant: Enfant;
  enfantId: number;
  conditions: ConditionSanitaire[];
  traitements: Traitement[];
}

@Component({
  selector: 'app-etat-sanitaire',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './etat-sanitaire.html',
  styleUrls: ['./etat-sanitaire.scss']
})
export class EtatSanitaireComponent implements OnInit {
  private etatSanitaireService = inject(EtatSanitaireService);

  enfantsAvecEtat: EnfantEtatSanitaire[] = [];
  isLoading = false;
  error = '';

  ngOnInit(): void {
    this.chargerEtatSanitaire();
  }

  private extraireEnfantId(enfant: Enfant): number {
    return Number(enfant.id ?? enfant.enfantId ?? 0);
  }

  private toArray<T>(data: any): T[] {
    if (Array.isArray(data)) {
      return data;
    }
    if (data === null || data === undefined) {
      return [];
    }
    return [data];
  }

  chargerEtatSanitaire(): void {
    this.isLoading = true;
    this.error = '';

    this.etatSanitaireService.getAllEnfants().subscribe({
      next: (enfants: Enfant[]) => {
        console.log('ENFANTS = ', enfants);

        if (!enfants || enfants.length === 0) {
          this.enfantsAvecEtat = [];
          this.isLoading = false;
          return;
        }

        const requetes = enfants.map((enfant: Enfant) => {
          const enfantId = this.extraireEnfantId(enfant);
          console.log('Chargement enfantId = ', enfantId, enfant.nom, enfant.prenom);

          if (!enfantId) {
            return of({
              conditions: [],
              traitements: []
            });
          }

          return forkJoin({
            conditions: this.etatSanitaireService.getConditionsByEnfant(enfantId).pipe(
              catchError((err) => {
                console.error('ERREUR CONDITIONS enfantId = ' + enfantId, err);
                return of([]);
              })
            ),
            traitements: this.etatSanitaireService.getTraitementsByEnfant(enfantId).pipe(
              catchError((err) => {
                console.error('ERREUR TRAITEMENTS enfantId = ' + enfantId, err);
                return of([]);
              })
            )
          });
        });

        forkJoin(requetes).subscribe({
          next: (resultats: any[]) => {
            console.log('RESULTATS CONDITIONS/TRAITEMENTS = ', resultats);

            this.enfantsAvecEtat = enfants.map((enfant: Enfant, index: number) => {
              const conditions = this.toArray<ConditionSanitaire>(resultats[index]?.conditions);
              const traitements = this.toArray<Traitement>(resultats[index]?.traitements);

              console.log('Conditions enfant ' + this.extraireEnfantId(enfant) + ' = ', conditions);
              console.log('Traitements enfant ' + this.extraireEnfantId(enfant) + ' = ', traitements);

              return {
                enfant,
                enfantId: this.extraireEnfantId(enfant),
                conditions,
                traitements
              };
            });

            console.log('DONNEES FINALES = ', this.enfantsAvecEtat);
            this.isLoading = false;
          },
          error: (err) => {
            console.error('ERREUR GLOBALE = ', err);
            this.error = 'Erreur lors du chargement des données sanitaires';
            this.isLoading = false;
          }
        });
      },
      error: (err) => {
        console.error('ERREUR ENFANTS = ', err);
        this.error = 'Erreur lors du chargement des enfants';
        this.isLoading = false;
      }
    });
  }

  estChronique(condition: ConditionSanitaire): boolean {
    return (condition.type || '').toLowerCase().trim() === 'chronique';
  }

  getBadgeLabel(condition: ConditionSanitaire): string {
    return this.estChronique(condition) ? 'URGENT - CHRONIQUE' : (condition.type || 'Condition');
  }

  trackByEnfant(index: number, item: EnfantEtatSanitaire): number {
    return item.enfantId || index;
  }

  trackByCondition(index: number, item: ConditionSanitaire): number {
    return item.id || index;
  }

  trackByTraitement(index: number, item: Traitement): number {
    return item.id || index;
  }
}