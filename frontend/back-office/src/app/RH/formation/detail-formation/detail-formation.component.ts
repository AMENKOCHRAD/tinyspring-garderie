import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormationService, Formation, AnimatriceFormation } from '../../../services/RH/formation.service';

@Component({
  selector: 'app-detail-formation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './detail-formation.component.html',
  styleUrl: './detail-formation.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class DetailFormationComponent implements OnInit {

  @ViewChild('modalInscrire') modalInscrire!: ElementRef<HTMLDialogElement>;

  formation: Formation | null = null;
  inscriptions: AnimatriceFormation[] = [];
  animatrices: any[] = [];
  animatricesNonInscrites: any[] = [];
  animatriceSelectionnee: number | null = null;
  isLoading = false;
  successMessage = '';
  errorMessage = '';
  formationId!: number;

  private adminUrl = 'http://localhost:8081/api/admin';

  constructor(
    private formationService: FormationService,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.formationId = +this.route.snapshot.paramMap.get('id')!;
    this.loadFormation();
    this.loadAnimatrices();
  }

  loadFormation(): void {
    this.isLoading = true;
    this.formationService.getFormationById(this.formationId).subscribe({
      next: (data) => {
        this.formation = data;
        this.inscriptions = data.inscriptions || [];
        this.calculerAnimatricesNonInscrites();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; }
    });
  }

  loadAnimatrices(): void {
    this.http.get<any[]>(`${this.adminUrl}/animatrices`).subscribe({
      next: (data) => {
        this.animatrices = data;
        this.calculerAnimatricesNonInscrites();
        this.cdr.detectChanges();
      }
    });
  }

  calculerAnimatricesNonInscrites(): void {
    const idsInscrits = this.inscriptions.map(i => i.animatrice?.id);
    this.animatricesNonInscrites = this.animatrices.filter(
      a => !idsInscrits.includes(a.id)
    );
  }

  // ===== CYCLE DE VIE =====
  demarrer(): void {
    if (!confirm(`Démarrer la formation "${this.formation?.titre}" ?`)) return;
    this.formationService.demarrerFormation(this.formationId).subscribe({
      next: () => {
        this.successMessage = '✅ Formation démarrée — animatrices notifiées';
        this.loadFormation();
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur'; }
    });
  }

  terminer(): void {
    if (!confirm('Terminer la formation ? Les certificats seront générés automatiquement.')) return;
    this.formationService.terminerFormation(this.formationId).subscribe({
      next: () => {
        this.successMessage = '🎓 Formation terminée — certificats générés !';
        this.loadFormation();
        setTimeout(() => this.successMessage = '', 4000);
      },
      error: (err) => { this.errorMessage = err.error?.message || 'Erreur'; }
    });
  }

  // ===== GESTION INSCRIPTIONS =====
  ouvrirModalInscrire(): void {
    this.animatriceSelectionnee = null;
    this.errorMessage = '';
    this.cdr.detectChanges();
    this.modalInscrire.nativeElement.showModal();
  }

  confirmerInscription(): void {
    if (!this.animatriceSelectionnee) {
      this.errorMessage = 'Sélectionnez une animatrice';
      return;
    }
    this.formationService.inscrireAnimatrice(this.formationId, this.animatriceSelectionnee)
      .subscribe({
        next: (result: any) => {
          const enAttente = result.statut === 'LISTE_ATTENTE';
          this.successMessage = enAttente
            ? '⏳ Animatrice ajoutée en liste d\'attente'
            : '✅ Animatrice inscrite avec succès';
          this.modalInscrire.nativeElement.close();
          this.loadFormation();
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => { this.errorMessage = err.error?.message || 'Erreur inscription'; }
      });
  }

  desinscrire(animatriceId: number, nom: string): void {
    if (!confirm(`Désinscrire ${nom} ?`)) return;
    this.formationService.desinscrireAnimatrice(this.formationId, animatriceId).subscribe({
      next: () => {
        this.successMessage = '✅ Animatrice désinscrite — liste d\'attente mise à jour';
        this.loadFormation();
        setTimeout(() => this.successMessage = '', 3000);
      }
    });
  }

  retour(): void {
    this.router.navigate(['/rh/formations']);
  }

  // ===== HELPERS =====
  getStatutColor(statut?: string): string {
    switch (statut) {
      case 'OUVERTE':       return '#3b82f6';
      case 'EN_COURS':      return '#f59e0b';
      case 'TERMINEE':      return '#16a34a';
      case 'ANNULEE':       return '#ef4444';
      case 'INSCRITE':      return '#3b82f6';
      case 'LISTE_ATTENTE': return '#f59e0b';
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

  getTypeIcon(type?: string): string {
    const icons: any = {
      SECOURISME: '🛡️', PEDAGOGIE: '📚', SANTE: '❤️',
      MUSICAL: '🎵', ARTISTIQUE: '🎨', COMPORTEMENT: '🧠',
      NUTRITION: '🥗', SECURITE: '🔒', AUTRE: '⭐'
    };
    return icons[type || ''] || '⭐';
  }

  formatHeure(heureStr?: string): string {
    if (!heureStr) return '';
    return heureStr.substring(0, 5);
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('fr-FR');
  }

  getRemplissagePct(): number {
    if (!this.formation?.placesMax) return 0;
    const inscrits = this.inscriptions.filter(i => i.statut === 'INSCRITE').length;
    return Math.min(100, Math.round((inscrits / this.formation.placesMax) * 100));
  }

  getNbInscrits(): number {
    return this.inscriptions.filter(i => i.statut === 'INSCRITE').length;
  }

  getNbAttente(): number {
    return this.inscriptions.filter(i => i.statut === 'LISTE_ATTENTE').length;
  }

  getNbCertificats(): number {
    return this.inscriptions.filter(i => i.certificationGeneree).length;
  }

  peutDemarrer(): boolean { return this.formation?.statut === 'OUVERTE'; }
  peutTerminer(): boolean { return this.formation?.statut === 'EN_COURS'; }
  peutInscrire(): boolean { return this.formation?.statut === 'OUVERTE'; }
}