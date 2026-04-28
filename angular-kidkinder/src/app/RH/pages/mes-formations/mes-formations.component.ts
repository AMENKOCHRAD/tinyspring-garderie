import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-mes-formations',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mes-formations.component.html',
  styleUrl: './mes-formations.component.scss'
})
export class MesFormationsComponent implements OnInit {

  profil: any = null;
  isLoading = false;
  animatriceId: number = 0;
  onglet: 'historique' | 'suggestions' | 'alertes' = 'historique';

  // ✅ PAGINATION — une page par onglet
  pageHistorique = 1;
  pageSuggestions = 1;
  parPage = 8;

  private adminUrl = 'http://localhost:8081/api/admin/formations';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.chargerAnimatrice();
  }

  chargerAnimatrice(): void {
    const userData = localStorage.getItem('tinyspring.auth.user');
    if (!userData) return;
    const user = JSON.parse(userData);
    this.http.get<any>(`http://localhost:8081/api/animatrice/profil/par-email?email=${user.email}`)
      .subscribe({
        next: (a) => {
          this.animatriceId = a.id;
          this.loadProfil();
        }
      });
  }

  loadProfil(): void {
    this.isLoading = true;
    this.http.get<any>(`${this.adminUrl}/animatrices/${this.animatriceId}/profil`)
      .subscribe({
        next: (data) => {
          this.profil = data;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: () => { this.isLoading = false; }
      });
  }

  // ===== LISTES COMPLÈTES =====
  get formationsTerminees(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'TERMINEE') || [];
  }

  get formationsEnCours(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'INSCRITE') || [];
  }

  get formationsAttente(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'LISTE_ATTENTE') || [];
  }

  get toutesFormations(): any[] {
    return [...this.formationsEnCours, ...this.formationsAttente, ...this.formationsTerminees];
  }

  // ===== PAGINATION HISTORIQUE =====
  get formationsPaginéesHistorique(): any[] {
    const debut = (this.pageHistorique - 1) * this.parPage;
    return this.toutesFormations.slice(debut, debut + this.parPage);
  }

  get totalPagesHistorique(): number {
    return Math.ceil(this.toutesFormations.length / this.parPage);
  }

  get pagesHistorique(): number[] {
    return Array.from({ length: this.totalPagesHistorique }, (_, i) => i + 1);
  }

  goToPageHistorique(page: number): void {
    if (page < 1 || page > this.totalPagesHistorique) return;
    this.pageHistorique = page;
  }

  // ===== PAGINATION SUGGESTIONS =====
  get suggestionsPaginées(): any[] {
    const debut = (this.pageSuggestions - 1) * this.parPage;
    return (this.profil?.suggestions || []).slice(debut, debut + this.parPage);
  }

  get totalPagesSuggestions(): number {
    return Math.ceil((this.profil?.suggestions?.length || 0) / this.parPage);
  }

  get pagesSuggestions(): number[] {
    return Array.from({ length: this.totalPagesSuggestions }, (_, i) => i + 1);
  }

  goToPageSuggestions(page: number): void {
    if (page < 1 || page > this.totalPagesSuggestions) return;
    this.pageSuggestions = page;
  }

  // ===== RESET PAGE AU CHANGEMENT D'ONGLET =====
  setOnglet(o: 'historique' | 'suggestions' | 'alertes'): void {
    this.onglet = o;
    this.pageHistorique = 1;
    this.pageSuggestions = 1;
  }

  getTauxCompletion(): number {
    if (!this.profil?.stats) return 0;
    const { terminees, total } = this.profil.stats;
    return total > 0 ? Math.round((terminees / total) * 100) : 0;
  }

  getStatutColor(statut?: string): string {
    switch (statut) {
      case 'INSCRITE':      return '#3b82f6';
      case 'LISTE_ATTENTE': return '#f59e0b';
      case 'TERMINEE':      return '#16a34a';
      case 'ABANDONNEE':    return '#ef4444';
      default:              return '#6b7280';
    }
  }

  getValiditeColor(statut?: string): string {
    switch (statut) {
      case 'VALIDE':          return '#16a34a';
      case 'BIENTOT_EXPIREE': return '#f59e0b';
      case 'EXPIREE':         return '#ef4444';
      default:                return '#6b7280';
    }
  }

  getPrioriteColor(priorite: string): string {
    switch (priorite) {
      case 'URGENT':     return '#ef4444';
      case 'IMPORTANT':  return '#f59e0b';
      case 'RECOMMANDÉ': return '#3b82f6';
      case 'SUGGÉRÉ':    return '#10b981';
      default:           return '#6b7280';
    }
  }

  getTypeIcon(type?: string): string {
    const icons: any = {
      SECOURISME: '🛡️', PEDAGOGIE: '📚', SANTE: '❤️',
      MUSICAL: '🎵', ARTISTIQUE: '🎨', COMPORTEMENT: '🧠',
      NUTRITION: '🥗', SECURITE: '🔒', AUTRE: '⭐'
    };
    return icons[type || ''] || '⭐';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
}