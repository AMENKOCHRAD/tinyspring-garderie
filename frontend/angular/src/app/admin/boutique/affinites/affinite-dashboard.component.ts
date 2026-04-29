import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AffiniteAdminService } from 'src/app/services/affinite-admin.service';
import { UserCategorieScoreDto } from 'src/app/models/boutique/user-categorie-score.dto';

type SortDirection = 'asc' | 'desc';

interface AffiniteStats {
  totalInteractions: number;
  parentsWithProfile: number;
  topCategorie: string;
  topCategorieInteractions: number;
  topParent: string;
  topParentInteractions: number;
}

@Component({
  selector: 'app-affinite-dashboard',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './affinite-dashboard.component.html',
  styleUrls: ['./affinite-dashboard.component.scss']
})
export class AffiniteDashboardComponent implements OnInit {
  private readonly affiniteAdminService = inject(AffiniteAdminService);
  private readonly cdr = inject(ChangeDetectorRef);

  scores: UserCategorieScoreDto[] = [];
  selectedParentScores: UserCategorieScoreDto[] = [];
  selectedParent: Pick<UserCategorieScoreDto, 'userId' | 'userNom' | 'userEmail'> | null = null;

  loading = true;
  detailLoading = false;
  errorMsg = '';
  detailErrorMsg = '';
  searchQuery = '';
  sortDirection: SortDirection = 'desc';

  ngOnInit(): void {
    this.loadScores();
  }

  get filteredScores(): UserCategorieScoreDto[] {
    const query = this.searchQuery.trim().toLowerCase();
    const filtered = query
      ? this.scores.filter((score) => score.userNom.toLowerCase().includes(query))
      : this.scores;

    return [...filtered].sort((a, b) => {
      if (a.score !== b.score) {
        return this.sortDirection === 'desc' ? b.score - a.score : a.score - b.score;
      }

      return a.userNom.localeCompare(b.userNom, 'fr');
    });
  }

  get hasData(): boolean {
    return this.scores.length > 0;
  }

  get stats(): AffiniteStats {
    if (!this.scores.length) {
      return {
        totalInteractions: 0,
        parentsWithProfile: 0,
        topCategorie: 'Aucune donnee',
        topCategorieInteractions: 0,
        topParent: 'Aucun parent',
        topParentInteractions: 0
      };
    }

    const totalInteractions = this.scores.reduce((sum, score) => sum + score.nbInteractions, 0);
    const parentsWithProfile = new Set(this.scores.filter((score) => score.score > 0).map((score) => score.userId)).size;

    const categoryMap = new Map<string, number>();
    const parentMap = new Map<string, number>();

    for (const score of this.scores) {
      categoryMap.set(score.categorieNom, (categoryMap.get(score.categorieNom) ?? 0) + score.nbInteractions);
      parentMap.set(score.userNom, (parentMap.get(score.userNom) ?? 0) + score.nbInteractions);
    }

    const [topCategorie, topCategorieInteractions] = this.getTopEntry(categoryMap, 'Aucune donnee');
    const [topParent, topParentInteractions] = this.getTopEntry(parentMap, 'Aucun parent');

    return {
      totalInteractions,
      parentsWithProfile,
      topCategorie,
      topCategorieInteractions,
      topParent,
      topParentInteractions
    };
  }

  get selectedParentMaxScore(): number {
    if (!this.selectedParentScores.length) {
      return 1;
    }

    return Math.max(...this.selectedParentScores.map((score) => score.score), 1);
  }

  loadScores(): void {
    this.loading = true;
    this.errorMsg = '';

    this.affiniteAdminService.getAllScores().subscribe({
      next: (scores) => {
        this.scores = scores.map((score) => this.normalizeScore(score));
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.scores = [];
        this.loading = false;
        this.errorMsg = this.buildErrorMessage(error, 'des profils d affinite');
        this.cdr.detectChanges();
      }
    });
  }

  toggleSortDirection(): void {
    this.sortDirection = this.sortDirection === 'desc' ? 'asc' : 'desc';
  }

  selectParent(score: UserCategorieScoreDto): void {
    this.selectedParent = {
      userId: score.userId,
      userNom: score.userNom,
      userEmail: score.userEmail
    };
    this.selectedParentScores = [];
    this.detailLoading = true;
    this.detailErrorMsg = '';

    this.affiniteAdminService.getScoresByUser(score.userId).subscribe({
      next: (scores) => {
        this.selectedParentScores = scores
          .map((item) => this.normalizeScore(item))
          .sort((a, b) => b.score - a.score || a.categorieNom.localeCompare(b.categorieNom, 'fr'));
        this.detailLoading = false;
        this.cdr.detectChanges();
      },
      error: (error: HttpErrorResponse) => {
        this.selectedParentScores = [];
        this.detailLoading = false;
        this.detailErrorMsg = this.buildErrorMessage(error, 'du parent selectionne');
        this.cdr.detectChanges();
      }
    });
  }

  closeSelectedParent(): void {
    this.selectedParent = null;
    this.selectedParentScores = [];
    this.detailLoading = false;
    this.detailErrorMsg = '';
  }

  getScoreBadgeClass(score: number): string {
    if (score < 20) {
      return 'b-badge--gray';
    }
    if (score < 100) {
      return 'b-badge--blue';
    }
    return 'b-badge--green';
  }

  getScoreBadgeLabel(score: number): string {
    if (score < 20) {
      return 'Faible';
    }
    if (score < 100) {
      return 'Moyen';
    }
    return 'Eleve';
  }

  getProgressWidth(score: number): number {
    return Math.max(4, Math.round((score / this.selectedParentMaxScore) * 100));
  }

  isSelectedUser(userId: number): boolean {
    return this.selectedParent?.userId === userId;
  }

  trackScore(_index: number, score: UserCategorieScoreDto): number {
    return score.id;
  }

  private normalizeScore(score: UserCategorieScoreDto): UserCategorieScoreDto {
    return {
      ...score,
      id: Number(score.id),
      userId: Number(score.userId),
      categorieId: Number(score.categorieId),
      score: Number(score.score ?? 0),
      nbInteractions: Number(score.nbInteractions ?? 0)
    };
  }

  private getTopEntry(values: Map<string, number>, fallbackLabel: string): [string, number] {
    if (!values.size) {
      return [fallbackLabel, 0];
    }

    return [...values.entries()].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0], 'fr'))[0];
  }

  private buildErrorMessage(error: HttpErrorResponse, context: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error.trim();
    }

    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message.trim();
    }

    if (error.status === 0) {
      return `Chargement ${context} impossible : source de donnees inaccessible.`;
    }

    return `Chargement ${context} impossible (${error.status || 'inconnu'}).`;
  }
}
