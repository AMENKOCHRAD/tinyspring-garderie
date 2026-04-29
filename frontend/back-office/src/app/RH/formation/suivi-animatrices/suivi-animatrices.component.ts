import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormationService } from '../../../services/RH/formation.service';

@Component({
  selector: 'app-suivi-animatrices',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './suivi-animatrices.component.html',
  styleUrl: './suivi-animatrices.component.scss'
})
export class SuiviAnimatricesComponent implements OnInit {

  animatrices: any[] = [];
  animatriceSelectionnee: any = null;
  profil: any = null;
  alertesGlobales: any = null;
  isLoading = false;
  isLoadingProfil = false;
  vue: 'liste' | 'profil' = 'liste';

  private adminUrl = 'http://localhost:8081/api/admin';

  constructor(
    private formationService: FormationService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAnimatrices();
    this.loadAlertesGlobales();
  }

  loadAnimatrices(): void {
    this.isLoading = true;
    this.http.get<any[]>(`${this.adminUrl}/animatrices`).subscribe({
      next: (data) => {
        this.animatrices = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadAlertesGlobales(): void {
    this.formationService.getAlertesGlobales().subscribe({
      next: (data) => { this.alertesGlobales = data; this.cdr.detectChanges(); }
    });
  }

  voirProfil(animatrice: any): void {
    this.animatriceSelectionnee = animatrice;
    this.isLoadingProfil = true;
    this.vue = 'profil';

    this.formationService.getProfilFormations(animatrice.id).subscribe({
      next: (data) => {
        this.profil = data;
        this.isLoadingProfil = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoadingProfil = false; }
    });
  }

  retourListe(): void {
    this.vue = 'liste';
    this.profil = null;
    this.animatriceSelectionnee = null;
  }

  estSansFormationRecente(animatriceId: number): boolean {
    if (!this.alertesGlobales?.sanFormationRecente) return false;
    return this.alertesGlobales.sanFormationRecente.some((a: any) => a.id === animatriceId);
  }

  aDesAlertes(animatriceId: number): boolean {
    if (!this.alertesGlobales) return false;
    const expirees = this.alertesGlobales.expirees?.some((af: any) =>
      af.animatrice?.id === animatriceId) || false;
    const bientot = this.alertesGlobales.bientotExpirees?.some((af: any) =>
      af.animatrice?.id === animatriceId) || false;
    return expirees || bientot || this.estSansFormationRecente(animatriceId);
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

  getTauxCompletion(): number {
    if (!this.profil?.stats) return 0;
    const { terminees, total } = this.profil.stats;
    return total > 0 ? Math.round((terminees / total) * 100) : 0;
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }
}