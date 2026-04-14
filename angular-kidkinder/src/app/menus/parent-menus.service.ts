import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  DecoratedDailyMenu,
  DecoratedWeeklyMenu,
  DailyMenu,
  MenuDish,
  MenuSectionKey,
  ParentMenusData,
  WeeklyMenu
} from './parent-menus.models';

@Injectable({
  providedIn: 'root'
})
export class ParentMenusService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/menus/weekly';

  getMenus(): Observable<ParentMenusData> {
    return this.http.get<WeeklyMenu[]>(this.apiUrl).pipe(
      map((menus) => {
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        const weeklyMenus = menus
          .filter((menu) => menu.status === 'PUBLISHED')
          .map((menu) => this.decorateWeeklyMenu(menu, today))
          .filter((menu) => menu.visibleDailyMenus.length > 0)
          .sort((left, right) => this.toTime(right.weekStartDate) - this.toTime(left.weekStartDate));

        const currentWeekIndex = Math.max(
          0,
          weeklyMenus.findIndex((menu) => menu.isCurrentWeek)
        );

        return {
          weeklyMenus,
          currentWeekIndex
        };
      })
    );
  }

  private decorateWeeklyMenu(menu: WeeklyMenu, today: Date): DecoratedWeeklyMenu {
    const visibleDailyMenus = (menu.dailyMenus ?? [])
      .filter((dailyMenu) => dailyMenu.isVisibleToParents !== false)
      .map((dailyMenu) => this.decorateDailyMenu(dailyMenu, today))
      .sort((left, right) => this.toTime(left.menuDate) - this.toTime(right.menuDate));

    const isCurrentWeek = this.isDateWithinWeek(today, menu.weekStartDate, menu.weekEndDate);

    return {
      ...menu,
      weekLabel: this.buildWeekLabel(menu.weekStartDate, menu.weekEndDate),
      weekRangeLabel: this.buildWeekRangeLabel(menu.weekStartDate, menu.weekEndDate),
      isCurrentWeek,
      visibleDailyMenus,
      todayMenu:
        visibleDailyMenus.find((dailyMenu) => dailyMenu.isToday) ??
        visibleDailyMenus[0] ??
        null
    };
  }

  private decorateDailyMenu(dailyMenu: DailyMenu, today: Date): DecoratedDailyMenu {
    const date = this.parseDateValue(dailyMenu.menuDate);
    const sections: Record<MenuSectionKey, MenuDish[]> = {
      ENTREE: [],
      PLAT_PRINCIPAL: [],
      ACCOMPAGNEMENT: [],
      DESSERT: [],
      GOUTER: []
    };

    for (const dish of dailyMenu.dishes ?? []) {
      if (dish.mealType === 'PLAT_PRINCIPAL') {
        if (sections.PLAT_PRINCIPAL.length === 0) {
          sections.PLAT_PRINCIPAL.push(dish);
        } else {
          sections.ACCOMPAGNEMENT.push(dish);
        }
        continue;
      }

      const key = this.resolveSectionKey(dish.mealType);
      sections[key].push(dish);
    }

    const allergens = this.extractDistinctValues((dailyMenu.dishes ?? []).flatMap((dish) => this.splitCsv(dish.allergens)));
    const conflictFlags = this.extractDistinctValues(
      (dailyMenu.dishes ?? []).flatMap((dish) => this.splitCsv(dish.allergenConflictFlags))
    );

    return {
      ...dailyMenu,
      displayDayShort: date
        ? new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(date)
        : this.getDayLabel(dailyMenu.dayOfWeek, 'short'),
      displayDayLong: date
        ? new Intl.DateTimeFormat('fr-FR', { weekday: 'long' }).format(date)
        : this.getDayLabel(dailyMenu.dayOfWeek, 'long'),
      displayDate: date ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(date) : '',
      displayDateLong: date
        ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }).format(date)
        : '',
      dayColor: this.getDayColor(date),
      isToday: this.isSameDay(dailyMenu.menuDate, today),
      sections,
      summaryDish: sections.PLAT_PRINCIPAL[0]?.name || this.getSummaryDish(dailyMenu.dishes ?? []),
      allergens,
      conflictFlags
    };
  }

  private buildWeekLabel(start: string | null, end: string | null): string {
    if (!start || !end) {
      return 'Semaine publiee';
    }

    const startDate = this.parseDateValue(start);
    const endDate = this.parseDateValue(end);
    const sameMonth = startDate.getMonth() === endDate.getMonth();
    const sameYear = startDate.getFullYear() === endDate.getFullYear();
    const endFormat = new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'long',
      ...(sameYear ? {} : { year: 'numeric' })
    });

    const startLabel = new Intl.DateTimeFormat('fr-FR', { day: 'numeric' }).format(startDate);
    const endLabel = sameMonth
      ? endFormat.format(endDate)
      : new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', ...(sameYear ? {} : { year: 'numeric' }) }).format(endDate);

    return `Semaine du ${startLabel} au ${endLabel}`;
  }

  private buildWeekRangeLabel(start: string | null, end: string | null): string {
    if (!start || !end) {
      return '';
    }

    const startDate = this.parseDateValue(start);
    const endDate = this.parseDateValue(end);
    const sameYear = startDate.getFullYear() === endDate.getFullYear();
    const startLabel = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(startDate);
    const endLabel = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(endDate);

    return sameYear ? `${startLabel} au ${endLabel} ${startDate.getFullYear()}` : `${startLabel} au ${endLabel}`;
  }

  private resolveSectionKey(type: string): MenuSectionKey {
    switch ((type || '').toUpperCase()) {
      case 'ENTREE':
        return 'ENTREE';
      case 'DESSERT':
        return 'DESSERT';
      case 'GOUTER':
        return 'GOUTER';
      default:
        return 'PLAT_PRINCIPAL';
    }
  }

  private getSummaryDish(dishes: MenuDish[]): string {
    return dishes[0]?.name || 'Plat a venir';
  }

  private getDayLabel(dayOfWeek: string | null, mode: 'short' | 'long'): string {
    const map = {
      MONDAY: { short: 'Lun', long: 'Lundi' },
      TUESDAY: { short: 'Mar', long: 'Mardi' },
      WEDNESDAY: { short: 'Mer', long: 'Mercredi' },
      THURSDAY: { short: 'Jeu', long: 'Jeudi' },
      FRIDAY: { short: 'Ven', long: 'Vendredi' },
      SATURDAY: { short: 'Sam', long: 'Samedi' },
      SUNDAY: { short: 'Dim', long: 'Dimanche' }
    } as const;

    const label = map[(dayOfWeek ?? '').toUpperCase() as keyof typeof map];
    return label ? label[mode] : mode === 'short' ? 'Jour' : 'Jour';
  }

  private getDayColor(date: Date | null): string {
    const day = date?.getDay();
    switch (day) {
      case 1:
        return '#2d6ecc';
      case 2:
        return '#2da36b';
      case 3:
        return '#6b57c8';
      case 4:
        return '#c67f1e';
      case 5:
        return '#d95989';
      default:
        return '#6f8b93';
    }
  }

  private splitCsv(value: string | null | undefined): string[] {
    return `${value || ''}`
      .split(',')
      .map((part) => part.trim())
      .filter(Boolean);
  }

  private extractDistinctValues(values: string[]): string[] {
    return Array.from(new Set(values));
  }

  private isSameDay(value: string | null, target: Date): boolean {
    if (!value) {
      return false;
    }

    const date = this.parseDateValue(value);
    return (
      date.getFullYear() === target.getFullYear() &&
      date.getMonth() === target.getMonth() &&
      date.getDate() === target.getDate()
    );
  }

  private isDateWithinWeek(date: Date, weekStartDate: string | null, weekEndDate: string | null): boolean {
    if (!weekStartDate || !weekEndDate) {
      return false;
    }

    const start = this.parseDateValue(weekStartDate);
    const end = this.parseDateValue(weekEndDate);
    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999);

    return date.getTime() >= start.getTime() && date.getTime() <= end.getTime();
  }

  private toTime(value: string | null): number {
    if (!value) {
      return Number.MIN_SAFE_INTEGER;
    }

    const timestamp = this.parseDateValue(value).getTime();
    return Number.isNaN(timestamp) ? Number.MIN_SAFE_INTEGER : timestamp;
  }

  private parseDateValue(value: string | null): Date {
    if (!value) {
      return new Date(Number.NaN);
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      const [year, month, day] = value.split('-').map(Number);
      return new Date(year, month - 1, day);
    }

    return new Date(value);
  }
}
