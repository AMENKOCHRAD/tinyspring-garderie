import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';
import { CategorieService } from 'src/app/services/boutique/categorie.service';
import { ProduitService } from 'src/app/services/boutique/produit.service';
import { CommandeService } from 'src/app/services/boutique/commande.service';
import { DashboardStats } from 'src/app/models/boutique/dashboard-stats.model';
import { Categorie } from 'src/app/models/boutique/categorie.model';
import { Produit } from 'src/app/models/boutique/produit.model';
import { Commande, StatutCommande } from 'src/app/models/boutique/commande.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [SharedModule, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private categorieService = inject(CategorieService);
  private produitService = inject(ProduitService);
  private commandeService = inject(CommandeService);
  private cdr = inject(ChangeDetectorRef);

  stats: DashboardStats | null = null;
  loading = true;
  errorMsg = '';

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.errorMsg = '';

    const currentUser = this.authService.getUser();
    console.info('[AdminDashboard] Chargement du dashboard boutique.', {
      endpoints: [
        'http://localhost:8081/api/admin/boutique/categories',
        'http://localhost:8081/api/admin/boutique/produits',
        'http://localhost:8081/api/admin/boutique/commandes'
      ],
      hasToken: !!this.authService.getToken(),
      role: currentUser?.role ?? null,
      email: currentUser?.email ?? null
    });

    forkJoin({
      categories: this.categorieService.getAllAdmin(),
      produits: this.produitService.getAllAdmin(),
      commandes: this.commandeService.getAll()
    }).subscribe({
      next: ({ categories, produits, commandes }) => {
        this.stats = this.buildStats(categories, produits, commandes);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        console.error('[AdminDashboard] Erreur de chargement du dashboard.', {
          status: error.status,
          url: error.url,
          errorBody: error.error
        });

        this.stats = null;
        this.errorMsg = this.buildErrorMessage(error);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get summaryMessage(): string {
    if (!this.stats) {
      return '';
    }

    const warnings = [];
    if (this.stats.produitsRupture > 0) {
      warnings.push(`${this.stats.produitsRupture} produit(s) en rupture`);
    }
    if (this.stats.produitsStockFaible > 0) {
      warnings.push(`${this.stats.produitsStockFaible} produit(s) en stock faible`);
    }
    if (this.stats.commandesPendingPayment > 0) {
      warnings.push(`${this.stats.commandesPendingPayment} commande(s) en attente paiement`);
    }

    return warnings.length > 0 ? warnings.join(' | ') : 'Tout va bien !';
  }

  formatDate(dateStr: string): string {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return dateStr;
    }
  }

  getStatutBadgeClass(statut: string): string {
    const statusMap: Record<string, string> = {
      PENDING: 'b-badge--gray',
      CONFIRMEE: 'b-badge--blue',
      EXPEDIEE: 'b-badge--cyan',
      LIVREE: 'b-badge--green',
      ANNULEE: 'b-badge--red'
    };
    return statusMap[statut] || 'b-badge--gray';
  }

  getStatutLabel(statut: string): string {
    const statusMap: Record<string, string> = {
      PENDING: 'En attente paiement',
      CONFIRMEE: 'Confirmee',
      EXPEDIEE: 'Expediee',
      LIVREE: 'Livree',
      ANNULEE: 'Annulee'
    };
    return statusMap[statut] || statut;
  }

  getMaxVentes(): number {
    if (!this.stats || this.stats.ventesParMois.length === 0) {
      return 0;
    }
    return Math.max(...this.stats.ventesParMois.map((vente) => vente.montant));
  }

  getVentesPercentage(montant: number): number {
    const max = this.getMaxVentes();
    return max > 0 ? (montant / max) * 100 : 0;
  }

  getImageUrl(imageUrl?: string | null): string {
    if (!imageUrl || imageUrl.trim() === '') {
      return this.getPlaceholderDataUrl();
    }

    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
      return imageUrl;
    }

    return `http://localhost:8081${imageUrl}`;
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = this.getPlaceholderDataUrl();
  }

  private buildErrorMessage(error: HttpErrorResponse): string {
    const statusText = error.status ? `HTTP ${error.status}` : 'HTTP inconnu';

    if (error.status === 401) {
      return `Chargement du dashboard impossible (${statusText}) : JWT absent, expire ou invalide.`;
    }

    if (error.status === 403) {
      return `Chargement du dashboard impossible (${statusText}) : acces ADMIN requis.`;
    }

    if (error.status === 404) {
      return `Chargement du dashboard impossible (${statusText}) : endpoint introuvable.`;
    }

    if (error.status === 0) {
      return 'Chargement du dashboard impossible (HTTP 0) : backend inaccessible ou probleme CORS.';
    }

    return `Chargement du dashboard impossible (${statusText}).`;
  }

  private getPlaceholderDataUrl(): string {
    return 'data:image/svg+xml;utf8,' + encodeURIComponent(
      '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 24 24" ' +
      'fill="none" stroke="#ccc" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
      '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>' +
      '<circle cx="8.5" cy="8.5" r="1.5"/>' +
      '<polyline points="21 15 16 10 5 21"/>' +
      '</svg>'
    );
  }

  private buildStats(categories: Categorie[], produits: Produit[], commandes: Commande[]): DashboardStats {
    const totalProduits = produits.length;
    const produitsRupture = produits.filter((produit) => produit.stock === 0).length;
    const produitsEnStock = produits.filter((produit) => produit.stock > 0).length;
    const produitsStockFaible = produits.filter((produit) => {
      const seuilAlerte = produit.seuilAlerte ?? 3;
      return produit.stock > 0 && produit.stock <= seuilAlerte;
    }).length;

    const commandesParStatut = this.countCommandesByStatut(commandes);
    const montantTotalVentes = commandes.reduce((total, commande) => total + Number(commande.montantTotal ?? 0), 0);

    const dernieresCommandes = [...commandes]
      .sort((a, b) => this.getDateValue(b.dateCommande) - this.getDateValue(a.dateCommande))
      .slice(0, 5)
      .map((commande) => ({
        id: commande.id,
        dateCommande: commande.dateCommande,
        statut: commande.statut,
        montantTotal: commande.montantTotal,
        userNom: commande.userNom,
        userEmail: commande.userEmail
      }));

    const produitIndex = new Map(produits.map((produit) => [produit.id, produit]));
    const topProduitsMap = new Map<number, { id: number; nom: string; imageUrl: string | null; totalCommandes: number }>();

    for (const commande of commandes) {
      for (const item of commande.items ?? []) {
        const existing = topProduitsMap.get(item.produitId);
        const produitAdmin = produitIndex.get(item.produitId);

        topProduitsMap.set(item.produitId, {
          id: item.produitId,
          nom: item.produitNom,
          imageUrl: produitAdmin?.imageUrl ?? item.produitImageUrl ?? null,
          totalCommandes: (existing?.totalCommandes ?? 0) + Number(item.quantite ?? 0)
        });
      }
    }

    const topProduits = [...topProduitsMap.values()]
      .sort((a, b) => b.totalCommandes - a.totalCommandes || a.nom.localeCompare(b.nom, 'fr'))
      .slice(0, 5);

    const ventesParMoisMap = new Map<string, number>();
    for (const commande of commandes) {
      const date = new Date(commande.dateCommande);
      if (Number.isNaN(date.getTime())) {
        continue;
      }

      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      ventesParMoisMap.set(key, (ventesParMoisMap.get(key) ?? 0) + Number(commande.montantTotal ?? 0));
    }

    const ventesParMois = [...ventesParMoisMap.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([key, montant]) => {
        const [year, month] = key.split('-').map(Number);
        const labelDate = new Date(year, month - 1, 1);
        return {
          mois: new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric' }).format(labelDate),
          montant
        };
      });

    return {
      totalProduits,
      produitsEnStock,
      produitsRupture,
      produitsStockFaible,
      totalCategories: categories.length,
      totalCommandes: commandes.length,
      commandesPendingPayment: commandesParStatut.PENDING,
      commandesConfirmees: commandesParStatut.CONFIRMEE,
      commandesExpediees: commandesParStatut.EXPEDIEE,
      commandesLivrees: commandesParStatut.LIVREE,
      commandesAnnulees: commandesParStatut.ANNULEE,
      montantTotalVentes,
      dernieresCommandes,
      topProduits,
      ventesParMois
    };
  }

  private countCommandesByStatut(commandes: Commande[]): Record<StatutCommande, number> {
    return commandes.reduce<Record<StatutCommande, number>>(
      (acc, commande) => {
        acc[commande.statut] = (acc[commande.statut] ?? 0) + 1;
        return acc;
      },
      {
        PENDING: 0,
        CONFIRMEE: 0,
        EXPEDIEE: 0,
        LIVREE: 0,
        ANNULEE: 0
      }
    );
  }

  private getDateValue(dateCommande: string): number {
    const parsed = new Date(dateCommande).getTime();
    return Number.isNaN(parsed) ? 0 : parsed;
  }
}
