import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';
import { DashboardService } from 'src/app/services/boutique/dashboard.service';
import { DashboardStats } from 'src/app/models/boutique/dashboard-stats.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [SharedModule, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);
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
      endpoint: 'http://localhost:8081/api/admin/boutique/dashboard/stats',
      hasToken: !!this.authService.getToken(),
      role: currentUser?.role ?? null,
      email: currentUser?.email ?? null
    });

    this.dashboardService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
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
    if (!this.stats) return '';
    const warnings = [];
    if (this.stats.produitsRupture > 0) {
      warnings.push(`${this.stats.produitsRupture} produit(s) en rupture`);
    }
    if (this.stats.produitsStockFaible > 0) {
      warnings.push(`${this.stats.produitsStockFaible} produit(s) en stock faible`);
    }
    if (this.stats.commandesEnAttente > 0) {
      warnings.push(`${this.stats.commandesEnAttente} commande(s) en attente`);
    }
    return warnings.length > 0 ? warnings.join(' • ') : 'Tout va bien !';
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
    const statusMap: { [key: string]: string } = {
      'CREEE': 'b-badge--amber',
      'CONFIRMEE': 'b-badge--blue',
      'EXPEDIEE': 'b-badge--cyan',
      'LIVREE': 'b-badge--green',
      'ANNULEE': 'b-badge--red',
      'EN_ATTENTE': 'b-badge--amber'
    };
    return statusMap[statut] || 'b-badge--gray';
  }

  getStatutLabel(statut: string): string {
    const statusMap: { [key: string]: string } = {
      'CREEE': 'Créée',
      'CONFIRMEE': 'Confirmée',
      'EXPEDIEE': 'Expédiée',
      'LIVREE': 'Livrée',
      'ANNULEE': 'Annulée',
      'EN_ATTENTE': 'En attente'
    };
    return statusMap[statut] || statut;
  }

  getMaxVentes(): number {
    if (!this.stats || this.stats.ventesParMois.length === 0) return 0;
    return Math.max(...this.stats.ventesParMois.map(v => v.montant));
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
}
