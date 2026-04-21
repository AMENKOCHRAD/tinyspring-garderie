import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-formations-disponibles',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './formations-disponibles.component.html',
  styleUrl: './formations-disponibles.component.scss'
})
export class FormationsDisponiblesComponent implements OnInit {

  formations: any[] = [];
  suggestions: any[] = [];
  isLoading = false;
  animatriceId: number = 0;
  successMessage = '';
  errorMessage = '';
  inscriptionEnCours: number | null = null;
  typeFiltre = '';

  private apiUrl = 'http://localhost:8081/api/animatrice/formations';
  private adminUrl = 'http://localhost:8081/api/admin/formations';

  types = ['SECOURISME', 'PEDAGOGIE', 'SANTE', 'MUSICAL', 'ARTISTIQUE', 'COMPORTEMENT', 'NUTRITION', 'SECURITE'];

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.chargerAnimatrice();
    this.loadFormations();
  }

  chargerAnimatrice(): void {
    const userData = localStorage.getItem('tinyspring.auth.user');
    if (!userData) return;
    const user = JSON.parse(userData);
    this.http.get<any>(`http://localhost:8081/api/animatrice/profil/par-email?email=${user.email}`)
      .subscribe({
        next: (a) => {
          this.animatriceId = a.id;
          this.loadSuggestions();
        }
      });
  }

  loadFormations(): void {
    this.isLoading = true;
    this.http.get<any[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.formations = data.filter(f => f.statut === 'OUVERTE' || f.statut === 'EN_COURS');
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadSuggestions(): void {
    if (!this.animatriceId) return;
    this.http.get<any[]>(`${this.adminUrl}/animatrices/${this.animatriceId}/suggestions`)
      .subscribe({
        next: (data) => { this.suggestions = data; this.cdr.detectChanges(); }
      });
  }

  get formationsFiltrees(): any[] {
    if (!this.typeFiltre) return this.formations;
    return this.formations.filter(f => f.type === this.typeFiltre);
  }

  isSuggere(formationId: number): any {
    return this.suggestions.find(s => s.formation?.id === formationId);
  }

  estInscrit(formation: any): boolean {
    if (!this.animatriceId || !formation.inscriptions) return false;
    return formation.inscriptions.some((i: any) =>
      i.animatrice?.id === this.animatriceId &&
      (i.statut === 'INSCRITE' || i.statut === 'LISTE_ATTENTE')
    );
  }

  getStatutInscription(formation: any): string | null {
    if (!this.animatriceId || !formation.inscriptions) return null;
    const insc = formation.inscriptions.find((i: any) => i.animatrice?.id === this.animatriceId);
    return insc?.statut || null;
  }

  inscrire(formation: any): void {
    if (!this.animatriceId) return;
    this.inscriptionEnCours = formation.id;

    this.http.post<any>(`${this.apiUrl}/${formation.id}/inscrire`,
      { animatriceId: this.animatriceId }).subscribe({
      next: (result) => {
        this.successMessage = result.statut === 'LISTE_ATTENTE'
          ? '⏳ Vous êtes en liste d\'attente — vous serez notifiée si une place se libère'
          : '✅ Inscription confirmée !';
        this.inscriptionEnCours = null;
        this.loadFormations();
        setTimeout(() => this.successMessage = '', 4000);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'inscription';
        this.inscriptionEnCours = null;
        setTimeout(() => this.errorMessage = '', 4000);
        this.cdr.detectChanges();
      }
    });
  }

  getPlacesDisponibles(formation: any): number {
    if (!formation.placesMax) return 99;
    const inscrits = formation.inscriptions?.filter((i: any) => i.statut === 'INSCRITE').length || 0;
    return Math.max(0, formation.placesMax - inscrits);
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

  getTypeColor(type?: string): string {
    const colors: any = {
      SECOURISME: '#ef4444', PEDAGOGIE: '#3b82f6', SANTE: '#10b981',
      MUSICAL: '#8b5cf6', ARTISTIQUE: '#f59e0b', COMPORTEMENT: '#6366f1',
      NUTRITION: '#14b8a6', SECURITE: '#f97316', AUTRE: '#6b7280'
    };
    return colors[type || ''] || '#6b7280';
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
}