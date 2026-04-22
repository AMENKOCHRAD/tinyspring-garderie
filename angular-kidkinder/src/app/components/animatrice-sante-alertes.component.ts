import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

interface AlerteDoseLike {
  enfant: { id: number; prenom: string; nom: string };
  condition: { nomCondition: string };
  traitement: { id: number; nomTraitement: string; heuresPrises?: string[] };
  heure: string;
  dejaDonne: boolean;
}

@Component({
  selector: 'app-animatrice-sante-alertes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './animatrice-sante-alertes.component.html',
  styleUrl: './animatrice-sante-alertes.component.css'
})
export class AnimatriceSanteAlertesComponent {
  @Input({ required: true }) alertes: AlerteDoseLike[] = [];
  @Input() searchTerm = '';
  @Input() isSavingPrise = false;

  @Output() mark = new EventEmitter<AlerteDoseLike>();

  get filteredAlertes(): AlerteDoseLike[] {
    const term = this.normalize(this.searchTerm);
    if (!term) {
      return this.alertes;
    }

    return (this.alertes ?? []).filter((alerte) => {
      const hay = [
        `${alerte.enfant?.prenom ?? ''} ${alerte.enfant?.nom ?? ''}`,
        alerte.traitement?.nomTraitement ?? '',
        alerte.condition?.nomCondition ?? '',
        alerte.heure ?? ''
      ].join(' ');
      return this.normalize(hay).includes(term);
    });
  }

  isDoseDueNow(alerte: AlerteDoseLike): boolean {
    if (!alerte || alerte.dejaDonne) {
      return false;
    }

    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}` === alerte.heure;
  }

  private normalize(value: string): string {
    return (value ?? '')
      .toLowerCase()
      .replace(/\s+/g, ' ')
      .trim();
  }
}
