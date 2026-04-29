import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
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
  private cdr = inject(ChangeDetectorRef);

  traitements: any[] = [];
  traitementsFiltres: any[] = [];
  traitementSelectionne: any = null;
  validationHistory: any[] = [];
  recentEvents: any[] = [];
  recentEventsFiltres: any[] = [];

  isLoading = false;
  error = '';
  searchTerm = '';
  historyLoading = false;
  historyError = '';
  eventsLoading = false;
  eventsError = '';

  eventSearchName = '';
  eventSearchDate = ''; // yyyy-MM-dd

  ngOnInit(): void {
    this.chargerEvenementsRecents();
  }

  chargerTraitements() {
    this.isLoading = true;
    this.error = '';

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
    this.traitementSelectionne = null;
    this.validationHistory = [];
    this.historyError = '';

    this.service.consulterTraitement(id).subscribe({
      next: (data) => {
        this.traitementSelectionne = data;
        this.chargerHistorique(id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement des détails';
        this.cdr.detectChanges();
      }
    });
  }

  chargerEvenementsRecents() {
    this.eventsLoading = true;
    this.eventsError = '';

    this.service.getLatestValidationEvents(200).subscribe({
      next: (data) => {
        this.recentEvents = data || [];
        this.filtrerEvenements();
        this.eventsLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.recentEvents = [];
        const status = err?.status;
        const backendMsg = err?.error?.message;

        if (status === 401) {
          this.eventsError = 'Session expirée. Reconnectez-vous pour charger l’historique global.';
        } else if (status === 403) {
          this.eventsError = 'Accès refusé. Ce module nécessite le rôle ADMIN.';
        } else if (status === 404) {
          this.eventsError = 'Endpoint introuvable (404). Redémarrez le backend, puis réessayez.';
        } else {
          const msg = backendMsg ? ` - ${backendMsg}` : '';
          const code = status != null ? ` (HTTP ${status})` : '';
          this.eventsError = `Impossible de charger l'historique global${code}${msg}`;
        }
        this.eventsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filtrerEvenements() {
    const name = (this.eventSearchName || '').trim().toLowerCase();
    const date = (this.eventSearchDate || '').trim(); // yyyy-MM-dd

    this.recentEventsFiltres = (this.recentEvents || []).filter((ev) => {
      const fullName = `${ev?.nomEnfant || ''} ${ev?.prenomEnfant || ''}`.trim().toLowerCase();
      const okName = !name || fullName.includes(name);

      let okDate = true;
      if (date) {
        try {
          const d = new Date(ev?.creeLe);
          const yyyy = d.getFullYear();
          const mm = String(d.getMonth() + 1).padStart(2, '0');
          const dd = String(d.getDate()).padStart(2, '0');
          okDate = `${yyyy}-${mm}-${dd}` === date;
        } catch {
          okDate = false;
        }
      }

      return okName && okDate;
    });
  }

  chargerHistorique(traitementId: number) {
    this.historyLoading = true;
    this.historyError = '';

    this.service.getValidationHistory(traitementId).subscribe({
      next: (events) => {
        this.validationHistory = events || [];
        this.historyLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.validationHistory = [];
        const status = err?.status;
        const backendMsg = err?.error?.message;

        if (status === 401) {
          this.historyError = 'Session expirée. Reconnectez-vous pour charger l’historique.';
        } else if (status === 403) {
          this.historyError = 'Accès refusé. Ce module nécessite le rôle ADMIN.';
        } else if (status === 404) {
          this.historyError = 'Historique introuvable (404). Redémarrez le backend, puis réessayez.';
        } else {
          const msg = backendMsg ? ` - ${backendMsg}` : '';
          const code = status != null ? ` (HTTP ${status})` : '';
          this.historyError = `Impossible de charger l'historique${code}${msg}`;
        }
        this.historyLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  accepter(id: number) {
    this.service.validerTraitement(id).subscribe(() => {
      this.traitements = this.traitements.filter(t => t.traitementId !== id);
      this.filtrer();
      this.traitementSelectionne = null;
      this.validationHistory = [];
    });
  }

  refuser(traitement: any) {
    const note = prompt('Motif du refus (optionnel) :') || undefined;

    this.service.refuserTraitement(traitement.traitementId, note).subscribe({
      next: () => {
        this.traitements = this.traitements.filter(t => t.traitementId !== traitement.traitementId);
        this.filtrer();
        this.traitementSelectionne = null;
        this.validationHistory = [];
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du refus';
      }
    });
  }

  fermerDetails() {
    this.traitementSelectionne = null;
    this.validationHistory = [];
    this.historyLoading = false;
    this.historyError = '';
  }

  
  isTraitementSensible(tr: any): boolean {
    if ((tr.typeCondition || '').toLowerCase() === 'chronique') return true;

    const txt = (tr.nomTraitement + ' ' + tr.description).toLowerCase();

    return txt.includes('asthme') ||
           txt.includes('insuline') ||
           txt.includes('allergie');
  }
}
