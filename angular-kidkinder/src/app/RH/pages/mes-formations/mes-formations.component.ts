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

  get formationsTerminees(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'TERMINEE') || [];
  }

  get formationsEnCours(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'INSCRITE') || [];
  }

  get formationsAttente(): any[] {
    return this.profil?.formations?.filter((af: any) => af.statut === 'LISTE_ATTENTE') || [];
  }

  getTauxCompletion(): number {
    if (!this.profil?.stats) return 0;
    const { terminees, total } = this.profil.stats;
    return total > 0 ? Math.round((terminees / total) * 100) : 0;
  }

  getStatutColor(statut?: string): string {
    switch (statut) {
      case 'INSCRITE':        return '#3b82f6';
      case 'LISTE_ATTENTE':   return '#f59e0b';
      case 'TERMINEE':        return '#16a34a';
      case 'ABANDONNEE':      return '#ef4444';
      default:                return '#6b7280';
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