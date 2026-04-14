import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ParentMenusService } from './parent-menus.service';
import { DecoratedDailyMenu, DecoratedWeeklyMenu, MenuDish, MenuSectionKey, ParentMenusData } from './parent-menus.models';

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
  protected readonly detailOpen = signal(false);
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

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
    if (selectedId != null) {
      return week.visibleDailyMenus.find((menu) => menu.id === selectedId) ?? week.todayMenu ?? week.visibleDailyMenus[0] ?? null;
    }

    return week.todayMenu ?? week.visibleDailyMenus[0] ?? null;
  });

  protected readonly archivedWeeks = computed(() => {
    const currentIndex = this.selectedWeekIndex();
    return (this.menusData()?.weeklyMenus ?? []).filter((_, index) => index !== currentIndex);
  });

  protected readonly mealOrder: MenuSectionKey[] = ['ENTREE', 'PLAT_PRINCIPAL', 'ACCOMPAGNEMENT', 'DESSERT', 'GOUTER'];

  public constructor() {
    this.menusService
      .getMenus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.menusData.set(data);
          this.selectedWeekIndex.set(data.currentWeekIndex);
          this.loading.set(false);
          this.errorMessage.set('');

          const week = data.weeklyMenus[data.currentWeekIndex] ?? data.weeklyMenus[0] ?? null;
          this.selectedDailyMenuId.set(week?.todayMenu?.id ?? week?.visibleDailyMenus[0]?.id ?? null);
        },
        error: (error: HttpErrorResponse | Error) => {
          this.loading.set(false);
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  protected goToPreviousWeek(): void {
    const nextIndex = this.selectedWeekIndex() + 1;
    if (nextIndex >= (this.menusData()?.weeklyMenus.length ?? 0)) {
      return;
    }

    this.detailOpen.set(false);
    this.selectWeek(nextIndex);
  }

  protected goToNextWeek(): void {
    const nextIndex = this.selectedWeekIndex() - 1;
    if (nextIndex < 0) {
      return;
    }

    this.detailOpen.set(false);
    this.selectWeek(nextIndex);
  }

  protected openDayDetails(menu: DecoratedDailyMenu): void {
    this.selectedDailyMenuId.set(menu.id);
    this.detailOpen.set(true);
  }

  protected closeDetails(): void {
    this.detailOpen.set(false);
  }

  protected selectDetailTab(menu: DecoratedDailyMenu): void {
    this.selectedDailyMenuId.set(menu.id);
  }

  protected openArchivedWeek(indexInArchive: number): void {
    const archive = this.archivedWeeks()[indexInArchive];
    if (!archive) {
      return;
    }

    const targetIndex = (this.menusData()?.weeklyMenus ?? []).findIndex((week) => week.id === archive.id);
    if (targetIndex === -1) {
      return;
    }

    this.detailOpen.set(false);
    this.selectWeek(targetIndex);
  }

  protected getMealTitle(type: MenuSectionKey): string {
    switch (type) {
      case 'ENTREE':
        return 'Entree';
      case 'PLAT_PRINCIPAL':
        return 'Plat principal';
      case 'ACCOMPAGNEMENT':
        return 'Accompagnement';
      case 'DESSERT':
        return 'Dessert';
      case 'GOUTER':
        return 'Gouter';
      default:
        return 'Plat';
    }
  }

  protected getMealValue(menu: DecoratedDailyMenu, type: MenuSectionKey): string {
    return menu.sections[type][0]?.name || 'Non renseigne';
  }

  protected getAllergens(dishes: MenuDish[]): string[] {
    return Array.from(new Set(dishes.flatMap((dish) => this.splitCsv(dish.allergens))));
  }

  protected getAllergenCount(menu: DecoratedDailyMenu): number {
    return menu.allergens.length;
  }

  protected getConflictFlags(menu: DecoratedDailyMenu): string[] {
    return menu.conflictFlags;
  }

  protected hasConflict(menu: DecoratedDailyMenu): boolean {
    return this.getConflictFlags(menu).length > 0;
  }

  protected getConflictSummary(menu: DecoratedDailyMenu): string {
    const flags = this.getConflictFlags(menu);
    return flags.length ? flags.join(', ') : 'Aucun conflit detecte';
  }

  protected getSummaryBadge(menu: DecoratedDailyMenu): string {
    return menu.isToday ? "Aujourd'hui" : menu.displayDayLong;
  }

  protected getDayBulletStyle(menu: DecoratedDailyMenu): string {
    return menu.dayColor;
  }

  private selectWeek(index: number): void {
    this.selectedWeekIndex.set(index);
    const week = this.menusData()?.weeklyMenus[index] ?? null;
    this.selectedDailyMenuId.set(week?.todayMenu?.id ?? week?.visibleDailyMenus[0]?.id ?? null);
  }

  private splitCsv(value: string | null | undefined): string[] {
    return `${value || ''}`
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean);
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
