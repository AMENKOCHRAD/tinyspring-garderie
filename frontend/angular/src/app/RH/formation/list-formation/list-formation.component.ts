import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormationService, Formation } from '../../../services/RH/formation.service';

@Component({
  selector: 'app-list-formation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './list-formation.component.html',
  styleUrl: './list-formation.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class ListFormationComponent implements OnInit, OnDestroy {

  @ViewChild('modalDialog') modalDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('modalAnnuler') modalAnnuler!: ElementRef<HTMLDialogElement>;

  formations: Formation[] = [];
  formationsFiltrees: Formation[] = [];
  stats: any = null;
  alertes: any = null;
  isLoading = false;
  isEditing = false;
  searchTerm = '';
  statutFiltre = '';
  successMessage = '';
  errorMessage = '';
  motifAnnulation = '';
  formationSelectionnee: Formation | null = null;

  // ✅ PAGINATION
  pageActuelle = 1;
  parPage = 6;

  private refreshInterval: any;

  types = ['SECOURISME', 'PEDAGOGIE', 'SANTE', 'MUSICAL', 'ARTISTIQUE', 'COMPORTEMENT', 'NUTRITION', 'SECURITE', 'AUTRE'];
  statuts = ['OUVERTE', 'EN_COURS', 'TERMINEE', 'ANNULEE'];

  salles = [
    'Salle de formation A',
    'Salle de formation B',
    'Salle de formation C',
    'Salle polyvalente',
    'Salle de réunion',
    'Espace extérieur',
    'En ligne (visioconférence)',
    'Centre de formation externe'
  ];

  form: Formation = this.formVide();
  animatrices: any[] = [];
  private adminUrl = 'http://localhost:8081/api/admin';

  constructor(
    private formationService: FormationService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadFormations();
    this.loadStats();
    this.loadAlertes();
    this.loadAnimatrices();
    this.refreshInterval = setInterval(() => {
      this.loadFormations();
      this.loadStats();
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  formVide(): Formation {
    return {
      titre: '', description: '', type: '', formateur: '',
      lieu: '', placesMax: undefined, dureeValiditeMois: undefined,
      obligatoire: false, dateFormation: undefined,
      heureDebut: undefined, heureFin: undefined
    };
  }

  loadFormations(): void {
    this.formationService.getToutesFormations().subscribe({
      next: (data) => {
        this.formations = data;
        this.filtrer();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadStats(): void {
    this.formationService.getStats().subscribe({
      next: (data) => { this.stats = data; this.cdr.detectChanges(); }
    });
  }

  loadAlertes(): void {
    this.formationService.getAlertesGlobales().subscribe({
      next: (data) => { this.alertes = data; this.cdr.detectChanges(); }
    });
  }

  loadAnimatrices(): void {
    this.http.get<any[]>(`${this.adminUrl}/animatrices`).subscribe({
      next: (data) => { this.animatrices = data; this.cdr.detectChanges(); }
    });
  }

  filtrer(): void {
    this.formationsFiltrees = this.formations.filter(f => {
      const matchSearch = !this.searchTerm ||
        f.titre.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (f.formateur && f.formateur.toLowerCase().includes(this.searchTerm.toLowerCase()));
      const matchStatut = !this.statutFiltre || f.statut === this.statutFiltre;
      return matchSearch && matchStatut;
    });
    this.pageActuelle = 1; // ✅ reset page à chaque filtre
  }

  // ✅ Formations de la page actuelle
  getFormationsPaginees(): Formation[] {
    const debut = (this.pageActuelle - 1) * this.parPage;
    return this.formationsFiltrees.slice(debut, debut + this.parPage);
  }

  get totalPages(): number {
    return Math.ceil(this.formationsFiltrees.length / this.parPage);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.pageActuelle = page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  getLastItemIndex(): number {
    return Math.min(this.pageActuelle * this.parPage, this.formationsFiltrees.length);
  }

  ouvrirModal(formation?: Formation): void {
    this.isEditing = !!formation;
    this.form = formation ? { ...formation } : this.formVide();
    this.errorMessage = '';
    this.cdr.detectChanges();
    this.modalDialog.nativeElement.showModal();
  }

  fermerModal(): void {
    this.modalDialog.nativeElement.close();
    this.errorMessage = '';
  }

  sauvegarder(): void {
    if (!this.form.titre || !this.form.type || !this.form.formateur) {
      this.errorMessage = 'Titre, type et formateur sont obligatoires';
      return;
    }
    if (this.form.heureDebut && this.form.heureFin &&
        this.form.heureDebut >= this.form.heureFin) {
      this.errorMessage = 'L\'heure de fin doit être après l\'heure de début';
      return;
    }

    const action = this.isEditing
      ? this.formationService.modifierFormation(this.form.id!, this.form)
      : this.formationService.creerFormation(this.form);

    action.subscribe({
      next: () => {
        this.successMessage = this.isEditing ? '✅ Formation modifiée' : '✅ Formation créée';
        this.fermerModal();
        this.loadFormations();
        this.loadStats();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur lors de la sauvegarde'; }
    });
  }

  demarrer(formation: Formation): void {
    if (!confirm(`Démarrer la formation "${formation.titre}" ?`)) return;
    this.formationService.demarrerFormation(formation.id!).subscribe({
      next: () => {
        this.successMessage = '✅ Formation démarrée — animatrices notifiées';
        this.loadFormations(); this.loadStats();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur'; }
    });
  }

  terminer(formation: Formation): void {
    if (!confirm(`Terminer la formation "${formation.titre}" ? Les certificats seront générés automatiquement.`)) return;
    this.formationService.terminerFormation(formation.id!).subscribe({
      next: () => {
        this.successMessage = '✅ Formation terminée — certificats générés !';
        this.loadFormations(); this.loadStats();
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur'; }
    });
  }

  ouvrirModalAnnuler(formation: Formation): void {
    this.formationSelectionnee = formation;
    this.motifAnnulation = '';
    this.cdr.detectChanges();
    this.modalAnnuler.nativeElement.showModal();
  }

  confirmerAnnulation(): void {
    if (!this.motifAnnulation.trim()) {
      this.errorMessage = 'Le motif est obligatoire';
      return;
    }
    this.formationService.annulerFormation(
      this.formationSelectionnee!.id!, this.motifAnnulation).subscribe({
      next: () => {
        this.successMessage = '✅ Formation annulée — animatrices notifiées';
        this.modalAnnuler.nativeElement.close();
        this.loadFormations(); this.loadStats();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur'; }
    });
  }

  supprimer(id: number): void {
    if (!confirm('Supprimer cette formation ?')) return;
    this.formationService.supprimerFormation(id).subscribe({
      next: () => {
        this.successMessage = '✅ Formation supprimée';
        this.loadFormations(); this.loadStats();
        setTimeout(() => this.successMessage = '', 3000);
      }
    });
  }

  getStatutColor(statut?: string): string {
    switch (statut) {
      case 'OUVERTE':  return '#3b82f6';
      case 'EN_COURS': return '#f59e0b';
      case 'TERMINEE': return '#16a34a';
      case 'ANNULEE':  return '#ef4444';
      default:         return '#6b7280';
    }
  }

  getStatutIcon(statut?: string): string {
    switch (statut) {
      case 'OUVERTE':  return '📝';
      case 'EN_COURS': return '⏳';
      case 'TERMINEE': return '✅';
      case 'ANNULEE':  return '❌';
      default:         return '❓';
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

  getRemplissagePct(formation: Formation): number {
    if (!formation.placesMax) return 0;
    return Math.min(100, Math.round(((formation.nbInscrits || 0) / formation.placesMax) * 100));
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }

  formatHeure(heureStr?: string): string {
    if (!heureStr) return '';
    return heureStr.substring(0, 5);
  }

  peutDemarrer(f: Formation): boolean { return f.statut === 'OUVERTE'; }
  peutTerminer(f: Formation): boolean { return f.statut === 'EN_COURS'; }
  peutAnnuler(f: Formation): boolean  { return f.statut === 'OUVERTE' || f.statut === 'EN_COURS'; }
  peutModifier(f: Formation): boolean { return f.statut === 'OUVERTE'; }
}