import { AsyncPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { DailyMenu } from '../../models/daily-menu.model';
import { Dish, MealCategory } from '../../models/dish.model';
import { WeeklyMenu } from '../../models/weekly-menu.model';
import { WeeklyMenuService } from '../../services/weekly-menu.service';
import { getImageUrl } from '../../utils/photo-url.util';

interface WeeklyMenuDetailVm {
  loading: boolean;
  menu: WeeklyMenu | null;
  errorMessage: string;
}

@Component({
  selector: 'app-weekly-menu-detail',
  standalone: true,
  imports: [SharedModule, RouterModule, AsyncPipe, DatePipe, EventsModuleSwitcherComponent],
  templateUrl: './weekly-menu-detail.component.html',
  styleUrls: ['./weekly-menu-detail.component.scss']
})
export class WeeklyMenuDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly weeklyMenuService = inject(WeeklyMenuService);

  readonly vm$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id'))),
    switchMap((id) => {
      if (!id || Number.isNaN(id)) {
        return of<WeeklyMenuDetailVm>({
          loading: false,
          menu: null,
          errorMessage: 'Identifiant du menu invalide.'
        });
      }

      return this.weeklyMenuService.getById(id).pipe(
        map(
          (menu) =>
            ({
              loading: false,
              menu,
              errorMessage: ''
            }) satisfies WeeklyMenuDetailVm
        ),
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
    { key: 'STARTER', label: 'Entrée' },
    { key: 'MAIN', label: 'Plat principal' },
    { key: 'SIDE', label: 'Accompagnement' },
    { key: 'DESSERT', label: 'Dessert' },
    { key: 'SNACK', label: 'Goûter' }
  ];

  goToEdit(id: number): void {
    this.router.navigate(['/events/menus', id, 'edit']);
  }

  getDishesByCategory(day: DailyMenu, category: MealCategory): Dish[] {
    return day.dishes.filter((dish) => dish.mealType === category);
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
        return 'Publié';
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

  getAllergenBadges(value: string | undefined): string[] {
    return (value ?? '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }

  getDishPhotoUrl(photoUrl: string | undefined): string | null {
    return getImageUrl(photoUrl);
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
