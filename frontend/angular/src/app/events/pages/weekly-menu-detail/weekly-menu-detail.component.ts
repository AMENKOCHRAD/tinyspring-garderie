import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { DailyMenu } from '../../models/daily-menu.model';
import { Dish, MealCategory } from '../../models/dish.model';
import { WeeklyMenu, WeeklyMenuRequest } from '../../models/weekly-menu.model';
import { DailyMenuRequest, DailyMenuService } from '../../services/daily-menu.service';
import { EventNotificationService } from '../../services/event-notification.service';
import { WeeklyMenuService } from '../../services/weekly-menu.service';

interface WeeklyMenuDetailVm {
  loading: boolean;
  menu: WeeklyMenu | null;
  errorMessage: string;
}

@Component({
  selector: 'app-weekly-menu-detail',
  standalone: true,
  imports: [SharedModule, RouterModule, AsyncPipe, DatePipe, NgClass, EventsModuleSwitcherComponent],
  templateUrl: './weekly-menu-detail.component.html',
  styleUrls: ['./weekly-menu-detail.component.scss']
})
export class WeeklyMenuDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly weeklyMenuService = inject(WeeklyMenuService);
  private readonly dailyMenuService = inject(DailyMenuService);
  private readonly notificationService = inject(EventNotificationService);

  private menuState: WeeklyMenu | null = null;
  private readonly pendingVisibilityIds = new Set<number>();

  readonly vm$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id'))),
    switchMap((id) => {
      if (!id || Number.isNaN(id)) {
        this.menuState = null;
        return of<WeeklyMenuDetailVm>({
          loading: false,
          menu: null,
          errorMessage: 'Identifiant du menu invalide.'
        });
      }

      return this.weeklyMenuService.getById(id).pipe(
        map((menu) => {
          this.menuState = menu;
          return {
            loading: false,
            menu,
            errorMessage: ''
          } satisfies WeeklyMenuDetailVm;
        }),
        catchError((error: HttpErrorResponse) =>
          of<WeeklyMenuDetailVm>({
            loading: false,
            menu: null,
            errorMessage: this.getErrorMessage(error, 'Impossible de charger le menu.')
          })
        ),
        startWith<WeeklyMenuDetailVm>({
          loading: true,
          menu: null,
          errorMessage: ''
        })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly mealSections: { key: MealCategory; label: string }[] = [
    { key: 'ENTREE', label: 'Entree' },
    { key: 'PLAT_PRINCIPAL', label: 'Plat principal' },
    { key: 'DESSERT', label: 'Dessert' },
    { key: 'GOUTER', label: 'Gouter' }
  ];

  getCurrentMenu(fallback: WeeklyMenu | null): WeeklyMenu | null {
    return this.menuState ?? fallback;
  }

  goToEdit(id: number): void {
    this.router.navigate(['/events/menus', id, 'edit']);
  }

  goToAddDay(id: number): void {
    this.router.navigate(['/events/menus', id, 'edit'], { queryParams: { addDay: 1 } });
  }

  goToEditDay(menuId: number, dayId: number, addDish = false): void {
    this.router.navigate(['/events/menus', menuId, 'days', dayId, 'edit'], {
      queryParams: addDish ? { addDish: 1 } : undefined
    });
  }

  toggleVisibility(menu: WeeklyMenu, day: DailyMenu): void {
    if (this.pendingVisibilityIds.has(day.id)) {
      return;
    }

    const previous = day.isVisibleToParents;
    this.patchDayVisibility(menu.id, day.id, !previous);
    this.pendingVisibilityIds.add(day.id);

    const payload: DailyMenuRequest = {
      weeklyMenuId: day.weeklyMenuId,
      menuDate: day.menuDate,
      dayOfWeek: day.dayOfWeek,
      isVisibleToParents: !previous,
      publishedAt: day.publishedAt ?? null
    };

    this.dailyMenuService.update(day.id, payload).subscribe({
      next: (updatedDay) => {
        this.pendingVisibilityIds.delete(day.id);
        this.replaceDay(menu.id, updatedDay);
      },
      error: (error: HttpErrorResponse) => {
        this.pendingVisibilityIds.delete(day.id);
        this.patchDayVisibility(menu.id, day.id, previous);
        this.notificationService.showError(this.getErrorMessage(error, 'La mise a jour de la visibilite a echoue.'));
      }
    });
  }

  isVisibilityPending(dayId: number): boolean {
    return this.pendingVisibilityIds.has(dayId);
  }

  getVisibilityTooltip(day: DailyMenu): string {
    return day.isVisibleToParents ? 'Masquer pour les parents' : 'Rendre visible pour les parents';
  }

  getTotalDishes(menu: WeeklyMenu): number {
    return menu.dailyMenus.reduce((total, day) => total + day.dishes.length, 0);
  }

  getDishesByCategory(day: DailyMenu, category: MealCategory): Dish[] {
    return day.dishes.filter((dish) => this.normalizeMealType(dish.mealType) === category);
  }

  formatDayOfWeek(value: string): string {
    switch (value) {
      case 'MONDAY':
        return 'Lundi';
      case 'TUESDAY':
        return 'Mardi';
      case 'WEDNESDAY':
        return 'Mercredi';
      case 'THURSDAY':
        return 'Jeudi';
      case 'FRIDAY':
        return 'Vendredi';
      case 'SATURDAY':
        return 'Samedi';
      default:
        return value;
    }
  }

  formatStatus(value: string): string {
    switch (value) {
      case 'PUBLISHED':
        return 'Publie';
      case 'TEMPLATE':
        return 'Template';
      default:
        return 'Brouillon';
    }
  }

  getStatusClass(value: string): string {
    switch (value) {
      case 'PUBLISHED':
        return 'status-published';
      case 'TEMPLATE':
        return 'status-template';
      default:
        return 'status-draft';
    }
  }

  getVisibilityLabel(day: DailyMenu): string {
    return day.isVisibleToParents ? 'Visible' : 'Masque';
  }

  getVisibilityClass(day: DailyMenu): string {
    return day.isVisibleToParents ? 'visible' : 'hidden';
  }

  getAllergenBadges(value: string | undefined): string[] {
    return (value ?? '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  private replaceDay(menuId: number, updatedDay: DailyMenu): void {
    if (!this.menuState || this.menuState.id !== menuId) {
      return;
    }

    this.menuState = {
      ...this.menuState,
      dailyMenus: this.menuState.dailyMenus.map((day) => (day.id === updatedDay.id ? updatedDay : day))
    };
  }

  private patchDayVisibility(menuId: number, dayId: number, visible: boolean): void {
    if (!this.menuState || this.menuState.id !== menuId) {
      return;
    }

    this.menuState = {
      ...this.menuState,
      dailyMenus: this.menuState.dailyMenus.map((day) =>
        day.id === dayId ? { ...day, isVisibleToParents: visible } : day
      )
    };
  }

  private normalizeMealType(value: MealCategory): MealCategory {
    switch (value) {
      case 'ENTREE':
        return 'ENTREE';
      case 'PLAT_PRINCIPAL':
        return 'PLAT_PRINCIPAL';
      case 'GOUTER':
        return 'GOUTER';
      default:
        return 'DESSERT';
    }
  }

  private getErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return fallback;
  }
}
