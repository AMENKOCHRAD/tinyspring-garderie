import {
  ChangeDetectorRef,
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin } from 'rxjs';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ProduitService } from 'src/app/services/boutique/produit.service';
import { MarketingAdvisorService } from 'src/app/services/boutique/marketing-advisor.service';
import { NotificationService } from 'src/app/services/notification.service';
import { AuthService } from 'src/app/services/auth.service';
import {
  MarketingAnalysis,
  MarketingAutomationResponse,
  MarketingSolution
} from 'src/app/models/boutique/marketing-advisor.model';
import { Produit } from 'src/app/models/boutique/produit.model';

type DetailTabId = 'description' | 'tags' | 'promotion' | 'email';

interface MarketingCardViewModel extends MarketingAnalysis {
  catalogueProduit: Produit | null;
  selectedTypes: string[];
  refreshing: boolean;
}

@Component({
  selector: 'app-marketing-advisor',
  standalone: true,
  imports: [SharedModule, CommonModule],
  templateUrl: './marketing-advisor.component.html',
  styleUrls: ['./marketing-advisor.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketingAdvisorComponent implements OnInit, OnDestroy {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly marketingService = inject(MarketingAdvisorService);
  private readonly produitService = inject(ProduitService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthService);
  private readonly modalService = inject(NgbModal);
  private readonly analysedProductIds = [9, 11, 13, 14, 15];

  @ViewChild('detailModal') detailModal!: TemplateRef<unknown>;
  @ViewChild('confirmModal') confirmModal!: TemplateRef<unknown>;

  analyses: MarketingCardViewModel[] = [];
  selectedAnalysis: MarketingCardViewModel | null = null;
  analysisPendingAutomation: MarketingCardViewModel | null = null;

  activeTabId: DetailTabId = 'description';
  loading = false;
  automating = false;
  progressValue = 0;
  messageProgress = 'Llama analyse vos produits...';
  errorMsg = '';
  successMsg = '';
  toastMessage = '';

  private toastTimer: ReturnType<typeof setTimeout> | null = null;
  private produitsIndex = new Map<number, Produit>();

  ngOnInit(): void {
    this.loadCatalogueProduits();
  }

  ngOnDestroy(): void {
    this.clearToast();
  }

  onAnalyseClick(): void {
    if (this.loading) {
      return;
    }

    void this.lancerAnalyse();
  }

  async lancerAnalyse(): Promise<void> {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';
    this.progressValue = 0;
    this.messageProgress = 'Preparation de l analyse IA...';
    this.analyses = [];
    this.selectedAnalysis = null;
    this.analysisPendingAutomation = null;
    this.cdr.detectChanges();

    const analyses: MarketingCardViewModel[] = [];
    let failedCount = 0;

    try {
      this.notificationService.stopPolling();
      const token = this.authService.getToken();

      if (!token) {
        this.errorMsg = 'JWT introuvable pour lancer l analyse marketing.';
        return;
      }

      if (this.analysedProductIds.length === 0) {
        this.successMsg = 'Aucun produit configure pour l analyse.';
        return;
      }

      for (let index = 0; index < this.analysedProductIds.length; index += 1) {
        const produitId = this.analysedProductIds[index];

        this.progressValue = Math.round((index / this.analysedProductIds.length) * 100) + 5;
        this.messageProgress = `Llama analyse le produit ${index + 1}/${this.analysedProductIds.length}...`;
        this.cdr.detectChanges();

        try {
          const response = await this.fetchWithTimeout(
            `http://localhost:8081/api/admin/marketing/analyser/${produitId}`,
            {
              headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json'
              }
            },
            120000
          );

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
          }

          const analysis = (await response.json()) as MarketingAnalysis;
          analyses.push(this.toCardViewModel(analysis));
          this.analyses = [...analyses].sort((a, b) => b.score_urgence - a.score_urgence);
          this.progressValue = Math.round(((index + 1) / this.analysedProductIds.length) * 100);
          this.cdr.detectChanges();
        } catch (error) {
          failedCount += 1;
          console.error('Erreur produit', produitId, error);
          this.cdr.detectChanges();
        }
      }

      this.analyses = analyses.sort((a, b) => b.score_urgence - a.score_urgence);
      this.successMsg =
        failedCount > 0
          ? `${this.analyses.length} produit(s) analyses, ${failedCount} en erreur.`
          : `${this.analyses.length} produit(s) analyses par Llama.`;
      this.cdr.detectChanges();
    } catch (error) {
      this.errorMsg = this.buildErrorMessage(error);
      this.cdr.detectChanges();
    } finally {
      this.notificationService.startPolling();
      this.loading = false;
      this.progressValue = 100;
      this.messageProgress = 'Analyse terminee.';
      this.cdr.detectChanges();
    }
  }

  openDetails(analysis: MarketingCardViewModel, tabId: DetailTabId = 'description'): void {
    this.selectedAnalysis = analysis;
    this.activeTabId = tabId;
    this.modalService.open(this.detailModal, { size: 'xl', centered: true, scrollable: true });
  }

  onDetailTabChange(event: NgbNavChangeEvent): void {
    this.activeTabId = event.nextId as DetailTabId;
  }

  toggleSolutionSelection(analysis: MarketingCardViewModel, solution: MarketingSolution, checked: boolean): void {
    if (!solution.automatisable) {
      return;
    }

    const selected = new Set(analysis.selectedTypes);
    if (checked) {
      selected.add(solution.type);
    } else {
      selected.delete(solution.type);
    }

    analysis.selectedTypes = [...selected];
  }

  prepareAutomation(analysis: MarketingCardViewModel): void {
    if (this.getSelectedCount(analysis) === 0) {
      return;
    }

    this.analysisPendingAutomation = analysis;
    this.modalService.open(this.confirmModal, { size: 'md', centered: true });
  }

  confirmAutomation(modal: { close: () => void }): void {
    const analysis = this.analysisPendingAutomation;
    if (!analysis || this.automating) {
      return;
    }

    this.automating = true;
    this.errorMsg = '';

    this.marketingService
      .automatiserProduit(analysis.produit_id, {
        types: [...analysis.selectedTypes],
        suggestion: this.buildSuggestionPayload(analysis)
      })
      .subscribe({
        next: (response) => {
          const appliedCount = this.getAppliedActionsCount(response, analysis.selectedTypes.length);
          modal.close();
          this.automating = false;
          this.analysisPendingAutomation = null;
          this.showToast(`${appliedCount} action(s) appliquee(s) sur ${analysis.produit_nom}`);
          this.refreshSingleAnalysis(analysis.produit_id);
        },
        error: (error) => {
          this.automating = false;
          this.errorMsg = this.buildErrorMessage(error);
        }
      });
  }

  selectTagsAction(): void {
    if (!this.selectedAnalysis) {
      return;
    }

    const tagSolution = this.selectedAnalysis.solutions.find((solution) => solution.type === 'TAGS' && solution.automatisable);
    if (!tagSolution) {
      return;
    }

    this.toggleSolutionSelection(this.selectedAnalysis, tagSolution, true);
    this.showToast(`Suggestion TAGS preselectionnee pour ${this.selectedAnalysis.produit_nom}`);
  }

  copyDescription(): void {
    const description = this.selectedAnalysis?.nouvelle_description?.trim();
    if (!description) {
      return;
    }

    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(description).then(() => {
        this.showToast('Nouvelle description copiee');
      });
      return;
    }

    this.showToast('Copie non disponible sur ce navigateur');
  }

  getSelectedCount(analysis: MarketingCardViewModel): number {
    return analysis.selectedTypes.length;
  }

  getSelectedSolutions(analysis: MarketingCardViewModel): MarketingSolution[] {
    return analysis.solutions.filter((solution) => analysis.selectedTypes.includes(solution.type));
  }

  isSelected(analysis: MarketingCardViewModel, solutionType: string): boolean {
    return analysis.selectedTypes.includes(solutionType);
  }

  getUrgencyLabel(score: number): string {
    if (score >= 8) {
      return 'Urgent';
    }
    if (score >= 5) {
      return 'Attention';
    }
    return 'Faible';
  }

  getUrgencyClass(score: number): string {
    if (score >= 8) {
      return 'm-urgency-badge--red';
    }
    if (score >= 5) {
      return 'm-urgency-badge--orange';
    }
    return 'm-urgency-badge--green';
  }

  getImpactClass(impact: string): string {
    switch (impact) {
      case 'Fort':
        return 'm-impact-badge--strong';
      case 'Moyen':
        return 'm-impact-badge--medium';
      default:
        return 'm-impact-badge--soft';
    }
  }

  formatDiagnostic(diagnostic: MarketingAnalysis['diagnostic']): string {
    if (typeof diagnostic === 'string') {
      return diagnostic;
    }

    if (diagnostic && typeof diagnostic === 'object') {
      const erreur = diagnostic['erreur'];
      if (typeof erreur === 'string' && erreur.trim()) {
        return erreur;
      }
      return JSON.stringify(diagnostic, null, 2);
    }

    return 'Aucun diagnostic fourni.';
  }

  get totalSuggestedActions(): number {
    return this.analyses.reduce((sum, analysis) => sum + analysis.solutions.length, 0);
  }

  get averageUrgencyScore(): string {
    if (this.analyses.length === 0) {
      return '0.0';
    }

    const average = this.analyses.reduce((sum, analysis) => sum + Number(analysis.score_urgence ?? 0), 0) / this.analyses.length;
    return average.toFixed(1);
  }

  getPriceAfterPromotion(analysis: MarketingCardViewModel): number | null {
    const currentPrice = Number(analysis.catalogueProduit?.prix ?? 0);
    const discount = Number(analysis.promotion_recommandee ?? 0);

    if (currentPrice <= 0 || discount <= 0) {
      return null;
    }

    return currentPrice * (1 - discount / 100);
  }

  trackAnalysis(_index: number, analysis: MarketingCardViewModel): number {
    return analysis.produit_id;
  }

  trackSolution(_index: number, solution: MarketingSolution): string {
    return `${solution.type}-${solution.titre}`;
  }

  trackTag(_index: number, tag: string): string {
    return tag;
  }

  private loadCatalogueProduits(): void {
    this.produitService.getAllAdmin().subscribe({
      next: (produits) => {
        this.produitsIndex = new Map(produits.map((produit) => [produit.id, produit]));
        this.analyses = this.analyses.map((analysis) => this.attachCatalogueProduct(analysis));
      },
      error: () => {
        this.produitsIndex = new Map<number, Produit>();
      }
    });
  }

  private refreshSingleAnalysis(produitId: number): void {
    const existing = this.analyses.find((analysis) => analysis.produit_id === produitId);
    if (existing) {
      existing.refreshing = true;
    }

    forkJoin({
      analysis: this.marketingService.analyserProduit(produitId),
      produits: this.produitService.getAllAdmin()
    }).subscribe({
      next: ({ analysis, produits }) => {
        this.produitsIndex = new Map(produits.map((produit) => [produit.id, produit]));
        const updatedCard = this.toCardViewModel(analysis);
        this.analyses = this.analyses
          .map((item) => (item.produit_id === produitId ? updatedCard : item))
          .sort((a, b) => b.score_urgence - a.score_urgence);

        if (this.selectedAnalysis?.produit_id === produitId) {
          this.selectedAnalysis = this.analyses.find((item) => item.produit_id === produitId) ?? null;
        }
      },
      error: (error) => {
        if (existing) {
          existing.refreshing = false;
        }
        this.errorMsg = this.buildErrorMessage(error);
      }
    });
  }

  private toCardViewModel(analysis: MarketingAnalysis): MarketingCardViewModel {
    return this.attachCatalogueProduct({
      ...analysis,
      selectedTypes: [],
      refreshing: false,
      catalogueProduit: null
    });
  }

  private attachCatalogueProduct(analysis: MarketingCardViewModel): MarketingCardViewModel {
    return {
      ...analysis,
      refreshing: false,
      catalogueProduit: this.produitsIndex.get(analysis.produit_id) ?? null
    };
  }

  private buildSuggestionPayload(analysis: MarketingCardViewModel): Record<string, unknown> {
    return {
      produit_id: analysis.produit_id,
      produit_nom: analysis.produit_nom,
      solutions: this.getSelectedSolutions(analysis),
      diagnostic: analysis.diagnostic,
      nouvelle_description: analysis.nouvelle_description,
      tags_suggeres: analysis.tags_suggeres,
      promotion_recommandee: analysis.promotion_recommandee,
      message_email: analysis.message_email
    };
  }

  private buildErrorMessage(error: HttpErrorResponse | { name?: string } | unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim()) {
        return error.error.trim();
      }

      if (typeof error.error?.message === 'string' && error.error.message.trim()) {
        return error.error.message.trim();
      }

      if (error.status === 401) {
        return 'Acces refuse : JWT absent ou invalide.';
      }

      if (error.status === 403) {
        return 'Acces admin requis pour Assistant Marketing IA.';
      }

      if (error.status === 0) {
        return 'Backend inaccessible ou probleme reseau.';
      }

      return `Erreur backend (${error.status || 'inconnue'}).`;
    }

    if (typeof error === 'object' && error !== null && 'name' in error && error.name === 'TimeoutError') {
      return 'Le backend marketing a depasse le delai de 5 minutes.';
    }

    return 'Erreur backend inconnue.';
  }

  private getAppliedActionsCount(response: MarketingAutomationResponse, fallback: number): number {
    const actions = response.actions_effectuees;
    return Array.isArray(actions) && actions.length > 0 ? actions.length : fallback;
  }

  private fetchWithTimeout(url: string, init: RequestInit, timeoutMs: number): Promise<Response> {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    return fetch(url, {
      ...init,
      signal: controller.signal
    }).finally(() => {
      clearTimeout(timer);
    });
  }

  private showToast(message: string): void {
    this.toastMessage = message;
    this.clearToastTimer();
    this.toastTimer = setTimeout(() => {
      this.toastMessage = '';
    }, 4000);
  }

  private clearToast(): void {
    this.toastMessage = '';
    this.clearToastTimer();
  }

  private clearToastTimer(): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }
}
