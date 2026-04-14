import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DailyMenu, DecoratedDailyMenu, ParentMenusData, WeeklyMenu } from './parent-menus.models';

@Injectable({
  providedIn: 'root'
})
export class ParentMenusService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/menus/weekly';

  getMenus(): Observable<ParentMenusData> {
    return this.http.get<WeeklyMenu[]>(this.apiUrl).pipe(
      map((weeklyMenus) => {
        const publishedMenus = weeklyMenus
          .filter((menu) => menu.status === 'PUBLISHED')
          .sort((left, right) => this.toTime(right.weekStartDate) - this.toTime(left.weekStartDate));

        const visibleDailyMenus = publishedMenus
          .flatMap((menu) => menu.dailyMenus ?? [])
          .filter((dailyMenu) => dailyMenu.isVisibleToParents !== false)
          .map((dailyMenu) => this.decorateDailyMenu(dailyMenu))
          .sort((left, right) => this.toTime(left.menuDate) - this.toTime(right.menuDate));

        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const todayMenu =
          visibleDailyMenus.find((menu) => this.isSameDay(menu.menuDate, today)) ??
          visibleDailyMenus.find((menu) => this.toTime(menu.menuDate) >= today.getTime()) ??
          visibleDailyMenus[0] ??
          null;

        const currentWeekMenu =
          publishedMenus.find((menu) => this.isDateWithinWeek(today, menu.weekStartDate, menu.weekEndDate)) ??
          publishedMenus[0] ??
          null;

        return {
          weeklyMenus: publishedMenus,
          visibleDailyMenus,
          todayMenu,
          currentWeekMenu
        };
      })
    );
  }

  private decorateDailyMenu(dailyMenu: DailyMenu): DecoratedDailyMenu {
    const date = dailyMenu.menuDate ? new Date(dailyMenu.menuDate) : null;
    const dishesByMealType: Record<string, typeof dailyMenu.dishes> = {
      ENTREE: [],
      PLAT_PRINCIPAL: [],
      DESSERT: [],
      GOUTER: []
    };

    for (const dish of dailyMenu.dishes ?? []) {
      const bucket = dishesByMealType[dish.mealType] ?? [];
      bucket.push(dish);
      dishesByMealType[dish.mealType] = bucket;
    }

    return {
      ...dailyMenu,
      displayDay: date
        ? new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(date)
        : this.getDayLabel(dailyMenu.dayOfWeek),
      displayDate: date
        ? new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' }).format(date)
        : '',
      dishesByMealType
    };
  }

  private getDayLabel(dayOfWeek: string | null): string {
    switch ((dayOfWeek ?? '').toUpperCase()) {
      case 'MONDAY':
        return 'Lun';
      case 'TUESDAY':
        return 'Mar';
      case 'WEDNESDAY':
        return 'Mer';
      case 'THURSDAY':
        return 'Jeu';
      case 'FRIDAY':
        return 'Ven';
      case 'SATURDAY':
        return 'Sam';
      case 'SUNDAY':
        return 'Dim';
      default:
        return 'Jour';
    }
  }

  private isSameDay(value: string | null, target: Date): boolean {
    if (!value) {
      return false;
    }

    const date = new Date(value);
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

    const start = new Date(weekStartDate);
    const end = new Date(weekEndDate);
    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999);

    return date.getTime() >= start.getTime() && date.getTime() <= end.getTime();
  }

  private toTime(value: string | null): number {
    if (!value) {
      return Number.MIN_SAFE_INTEGER;
    }

    const timestamp = new Date(value).getTime();
    return Number.isNaN(timestamp) ? Number.MIN_SAFE_INTEGER : timestamp;
  }
}
