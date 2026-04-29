import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ParentMenusService } from './parent-menus.service';
import {
  DecoratedDailyMenu,
  DecoratedWeeklyMenu,
  MenuDish,
  MenuSectionKey,
  ParentMenusData
} from './parent-menus.models';

@Component({
  selector: 'app-parent-menus',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './parent-menus.component.html',
  styleUrl: './parent-menus.component.css'
})
export class ParentMenusComponent {
  private readonly menusService = inject(ParentMenusService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly menusData = signal<ParentMenusData | null>(null);
  protected readonly selectedWeekIndex = signal(0);
  protected readonly selectedDailyMenuId = signal<number | null>(null);
  protected readonly archiveMode = signal(false);

  protected readonly mealOrder: MenuSectionKey[] = [
    'ENTREE',
    'PLAT_PRINCIPAL',
    'DESSERT',
    'GOUTER'
  ];

  protected readonly currentWeek = computed<DecoratedWeeklyMenu | null>(() => {
    const data = this.menusData();

    if (!data?.weeklyMenus.length) {
      return null;
    }

    return data.weeklyMenus[this.selectedWeekIndex()] ?? data.weeklyMenus[0] ?? null;
  });

  protected readonly selectedDailyMenu = computed<DecoratedDailyMenu | null>(() => {
    const week = this.currentWeek();

    if (!week) {
      return null;
    }

    const selectedId = this.selectedDailyMenuId();

    if (selectedId !== null) {
      return week.visibleDailyMenus.find((menu) => menu.id === selectedId) ?? null;
    }

    return week.todayMenu;
  });

  protected readonly archivedWeeks = computed(() => {
    const data = this.menusData();

    if (!data?.weeklyMenus.length) {
      return [];
    }

    return data.weeklyMenus.filter((_, index) => index !== data.currentWeekIndex);
  });

  public constructor() {
    this.menusService
      .getMenus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.menusData.set(data);
          this.selectedWeekIndex.set(data.currentWeekIndex);
          this.archiveMode.set(false);
          this.loading.set(false);
          this.errorMessage.set('');

          const selectedWeek = data.weeklyMenus[data.currentWeekIndex] ?? data.weeklyMenus[0] ?? null;
          this.selectedDailyMenuId.set(selectedWeek?.todayMenu?.id ?? null);
        },
        error: (error: HttpErrorResponse | Error) => {
          this.loading.set(false);
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  protected getTodayRealLabel(): string {
    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(new Date());
  }

  protected goToPreviousWeek(): void {
    const nextIndex = this.selectedWeekIndex() + 1;

    if (nextIndex >= (this.menusData()?.weeklyMenus.length ?? 0)) {
      return;
    }

    this.archiveMode.set(nextIndex !== this.menusData()?.currentWeekIndex);
    this.selectWeek(nextIndex);
  }

  protected goToNextWeek(): void {
    const nextIndex = this.selectedWeekIndex() - 1;

    if (nextIndex < 0) {
      return;
    }

    this.archiveMode.set(nextIndex !== this.menusData()?.currentWeekIndex);
    this.selectWeek(nextIndex);
  }

  protected openDayDetails(menu: DecoratedDailyMenu): void {
    this.selectedDailyMenuId.set(menu.id);

    setTimeout(() => {
      document.getElementById('day-detail-panel')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    });
  }

  protected selectDetailTab(menu: DecoratedDailyMenu): void {
    this.selectedDailyMenuId.set(menu.id);
  }

  protected openArchivedWeek(indexInArchive: number): void {
    const archive = this.archivedWeeks()[indexInArchive];

    if (!archive) {
      return;
    }

    const targetIndex = (this.menusData()?.weeklyMenus ?? []).findIndex(
      (week) => week.id === archive.id
    );

    if (targetIndex === -1) {
      return;
    }

    this.archiveMode.set(true);
    this.selectWeek(targetIndex);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
  }

  protected backToCurrentWeek(): void {
    const index = this.menusData()?.currentWeekIndex ?? 0;
    this.archiveMode.set(false);
    this.selectWeek(index);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
  }

  protected getMealTitle(type: MenuSectionKey): string {
    switch (type) {
      case 'ENTREE':
        return 'Entrée';
      case 'PLAT_PRINCIPAL':
        return 'Plat';
      case 'ACCOMPAGNEMENT':
        return 'Accomp.';
      case 'DESSERT':
        return 'Dessert';
      case 'GOUTER':
        return 'Goûter';
      default:
        return String(type);
    }
  }

  protected getMealDetailTitle(type: MenuSectionKey): string {
    switch (type) {
      case 'ENTREE':
        return 'ENTRÉE';
      case 'PLAT_PRINCIPAL':
        return 'PLAT PRINCIPAL';
      case 'ACCOMPAGNEMENT':
        return 'ACCOMPAGNEMENT';
      case 'DESSERT':
        return 'DESSERT';
      case 'GOUTER':
        return 'GOÛTER';
      default:
        return String(type);
    }
  }

  protected getMealValue(menu: DecoratedDailyMenu, type: MenuSectionKey): string {
    const dishes = this.getDishesForType(menu, type);
    return dishes[0]?.name || 'Non renseigné';
  }

  protected getDishesForType(menu: DecoratedDailyMenu, type: MenuSectionKey): MenuDish[] {
    return menu.sections?.[type] ?? [];
  }

  protected getAllergens(dishes: MenuDish[] | null | undefined): string[] {
    return Array.from(
      new Set((dishes ?? []).flatMap((dish) => this.splitCsv(dish.allergens)))
    );
  }

  protected getConflictFlags(menu: DecoratedDailyMenu): string[] {
    return menu.allergenConflictFlags ?? menu.conflictFlags ?? [];
  }

  protected hasConflict(menu: DecoratedDailyMenu): boolean {
    return this.getConflictFlags(menu).length > 0
      || (menu.allergenConflictMessages?.length ?? 0) > 0;
  }

  protected getConflictSummary(menu: DecoratedDailyMenu): string {
    return (menu.allergenConflictMessages ?? []).join(' ');
  }

  protected hasWeekConflict(week: DecoratedWeeklyMenu): boolean {
    return (week.visibleDailyMenus ?? []).some((day) => this.hasConflict(day));
  }

  protected getWeekConflictSummary(week: DecoratedWeeklyMenu): string {
    const day = (week.visibleDailyMenus ?? []).find((dailyMenu) => this.hasConflict(dailyMenu));

    if (!day) {
      return '';
    }

    const summary = this.getConflictSummary(day);
    return summary ? `${day.displayDayLong} — ${summary}` : '';
  }

  protected hasDishConflict(menu: DecoratedDailyMenu, allergen: string): boolean {
    const normalizedAllergen = this.normalizeText(allergen);

    return this.getConflictFlags(menu).some((flag) =>
      this.normalizeText(flag).includes(normalizedAllergen)
    );
  }

  protected getDayBulletStyle(menu: DecoratedDailyMenu): string {
    return menu.dayColor;
  }

  protected getChildrenSummary(): string {
    return 'Adam & Lina Ben Ali';
  }

  protected getTotalDishes(week: DecoratedWeeklyMenu): number {
    return week.visibleDailyMenus.reduce((total, day) => {
      return total + this.mealOrder.reduce(
        (count, type) => count + this.getDishesForType(day, type).length,
        0
      );
    }, 0);
  }

  protected getAllergenAlertCount(week: DecoratedWeeklyMenu): number {
    return week.visibleDailyMenus.reduce((total, day) => {
      return total + Math.max(
        this.getConflictFlags(day).length,
        day.allergenConflictMessages?.length ?? 0
      );
    }, 0);
  }

  protected getBalanceScore(week: DecoratedWeeklyMenu): number {
    const days = week.visibleDailyMenus.length || 1;

    const completeDays = week.visibleDailyMenus.filter((day) => {
      return this.mealOrder.every((type) => this.getDishesForType(day, type).length > 0);
    }).length;

    return Math.round((completeDays / days) * 100);
  }

  protected getBalanceClass(score: number): string {
    if (score >= 80) {
      return 'score-good';
    }

    if (score >= 50) {
      return 'score-medium';
    }

    return 'score-bad';
  }

  private selectWeek(index: number): void {
    this.selectedWeekIndex.set(index);

    const selectedWeek = this.menusData()?.weeklyMenus[index] ?? null;
    this.selectedDailyMenuId.set(selectedWeek?.todayMenu?.id ?? null);
  }

  private splitCsv(value: string | null | undefined): string[] {
    return `${value || ''}`
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean);
  }

  private normalizeText(value: string | null | undefined): string {
    return `${value || ''}`
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');
  }

  private getErrorMessage(error: HttpErrorResponse | Error): string {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string' && error.error.trim()) {
        return error.error;
      }

      if (error.error?.message) {
        return error.error.message;
      }
    }

    return 'Impossible de charger les menus pour le moment.';
  }
}