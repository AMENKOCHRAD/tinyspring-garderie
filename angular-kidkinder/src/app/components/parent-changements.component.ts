import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { Enfant, EnfantService } from '../services/enfant.service';

interface ObservationFront {
  id: number;
  type: 'SANTE' | 'COMPORTEMENT' | string;
  titre: string;
  description: string;
  creeLe: string;
  creeParNom?: string | null;
  enfantId?: number;
  enfantNom?: string;
  enfantPrenom?: string;
  luParent?: boolean;
  luLe?: string | null;
  observeLe?: string | null;
  urgence?: string | null;
  temperature?: number | null;
  lieu?: string | null;
  symptomes?: string | null;
  actionsEffectuees?: string | null;
}

@Component({
  selector: 'app-parent-changements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './parent-changements.component.html',
  styleUrl: './parent-changements.component.css'
})
export class ParentChangementsComponent {
  private readonly enfantService = inject(EnfantService);

  @Input({ required: true }) enfants: Enfant[] = [];

  observations: ObservationFront[] = [];
  isLoadingChangements = false;
  changementsOnlyUnread = false;
  changementsEnfantId = 0;
  changementsType = '';
  changementsSearch = '';

  ngOnInit(): void {
    this.chargerChangements();
  }

  chargerChangements(): void {
    if (this.isLoadingChangements) {
      return;
    }

    this.isLoadingChangements = true;

    this.enfantService
      .getObservationsParent(this.changementsOnlyUnread)
      .pipe(
        catchError((err) => {
          this.isLoadingChangements = false;
          return of({ __error: err } as any);
        })
      )
      .subscribe((res: any) => {
        if (res && res.__error) {
          // Keep UI unchanged: no extra banner here; simply stop loading.
          this.isLoadingChangements = false;
          return;
        }

        this.observations = (res ?? []) as ObservationFront[];
        this.isLoadingChangements = false;
      });
  }

  marquerChangementLu(obs: ObservationFront): void {
    if (!obs?.id || obs.luParent) {
      return;
    }

    this.enfantService
      .marquerObservationParentLue(Number(obs.id))
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.observations = this.observations.map((o) =>
          o.id === obs.id ? { ...o, luParent: true, luLe: new Date().toISOString() } : o
        );
      });
  }

  get filteredChangements(): ObservationFront[] {
    const term = (this.changementsSearch ?? '').toLowerCase().trim();

    return (this.observations ?? []).filter((o) => {
      if (this.changementsEnfantId && o.enfantId !== this.changementsEnfantId) {
        return false;
      }

      if (this.changementsType && (o.type ?? '') !== this.changementsType) {
        return false;
      }

      if (term) {
        const hay = [
          `${o.enfantPrenom ?? ''} ${o.enfantNom ?? ''}`,
          o.type ?? '',
          o.titre ?? '',
          o.description ?? '',
          o.urgence ?? '',
          o.lieu ?? '',
          String(o.temperature ?? '')
        ]
          .join(' ')
          .toLowerCase();
        if (!hay.includes(term)) {
          return false;
        }
      }

      return true;
    });
  }
}

