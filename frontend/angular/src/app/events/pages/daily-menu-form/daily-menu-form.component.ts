import { AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap, take } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { DailyMenu, MenuDayOfWeek } from '../../models/daily-menu.model';
import { Dish, MealCategory } from '../../models/dish.model';
import { WeeklyMenu } from '../../models/weekly-menu.model';
import { DailyMenuRequest, DailyMenuService } from '../../services/daily-menu.service';
import { DishRequest, DishService } from '../../services/dish.service';
import { EventNotificationService } from '../../services/event-notification.service';
import { WeeklyMenuService } from '../../services/weekly-menu.service';

interface DailyMenuFormVm {
  loading: boolean;
  menu: WeeklyMenu | null;
  day: DailyMenu | null;
  errorMessage: string;
}

@Component({
  selector: 'app-daily-menu-form',
  standalone: true,
  imports: [SharedModule, RouterModule, ReactiveFormsModule, AsyncPipe, EventsModuleSwitcherComponent],
  templateUrl: './daily-menu-form.component.html',
  styleUrls: ['./daily-menu-form.component.scss']
})
export class DailyMenuFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly weeklyMenuService = inject(WeeklyMenuService);
  private readonly dailyMenuService = inject(DailyMenuService);
  private readonly dishService = inject(DishService);
  private readonly notificationService = inject(EventNotificationService);

  private originalDay: DailyMenu | null = null;
  private weeklyMenuId: number | null = null;
  saving = false;

  readonly dayOptions: { value: MenuDayOfWeek; label: string }[] = [
    { value: 'MONDAY', label: 'Lundi' },
    { value: 'TUESDAY', label: 'Mardi' },
    { value: 'WEDNESDAY', label: 'Mercredi' },
    { value: 'THURSDAY', label: 'Jeudi' },
    { value: 'FRIDAY', label: 'Vendredi' },
    { value: 'SATURDAY', label: 'Samedi' }
  ];

  readonly mealOptions: { value: MealCategory; label: string }[] = [
    { value: 'ENTREE', label: 'Entree' },
    { value: 'PLAT_PRINCIPAL', label: 'Plat principal' },
    { value: 'DESSERT', label: 'Dessert' },
    { value: 'GOUTER', label: 'Gouter' }
  ];

  readonly form = this.fb.group({
    dayOfWeek: ['MONDAY' as MenuDayOfWeek, Validators.required],
    menuDate: ['', Validators.required],
    isVisibleToParents: [true],
    dishes: this.fb.array([])
  });

  readonly vm$ = this.route.paramMap.pipe(
    switchMap((params) => {
      const menuId = Number(params.get('menuId'));
      const dayId = Number(params.get('dayId'));
      this.weeklyMenuId = menuId;

      if (!menuId || !dayId || Number.isNaN(menuId) || Number.isNaN(dayId)) {
        return of<DailyMenuFormVm>({
          loading: false,
          menu: null,
          day: null,
          errorMessage: 'Identifiant invalide.'
        });
      }

      return this.weeklyMenuService.getById(menuId).pipe(
        map((menu) => {
          const day = menu.dailyMenus.find((item) => item.id === dayId) ?? null;
          if (!day) {
            return {
              loading: false,
              menu,
              day: null,
              errorMessage: 'Jour introuvable.'
            } satisfies DailyMenuFormVm;
          }

          this.patchForm(day);
          return {
            loading: false,
            menu,
            day,
            errorMessage: ''
          } satisfies DailyMenuFormVm;
        }),
        catchError((error: HttpErrorResponse) =>
          of<DailyMenuFormVm>({
            loading: false,
            menu: null,
            day: null,
            errorMessage: this.getErrorMessage(error, 'Impossible de charger ce jour.')
          })
        ),
        startWith<DailyMenuFormVm>({
          loading: true,
          menu: null,
          day: null,
          errorMessage: ''
        })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor() {
    this.route.queryParamMap.pipe(take(1)).subscribe((params) => {
      if (params.get('addDish') === '1') {
        queueMicrotask(() => this.addDish());
      }
    });
  }

  get dishesArray(): FormArray {
    return this.form.get('dishes') as FormArray;
  }

  addDish(dish?: Partial<Dish>): void {
    this.dishesArray.push(
      this.fb.group({
        id: [dish?.id ?? null],
        clientKey: [this.createClientKey()],
        mealType: [this.normalizeIncomingMealType(dish?.mealType ?? 'ENTREE'), Validators.required],
        name: [dish?.name ?? '', Validators.required],
        description: [dish?.description ?? ''],
        allergens: [dish?.allergens ?? '']
      })
    );
  }

  removeDish(index: number): void {
    this.dishesArray.removeAt(index);
  }

  async save(): Promise<void> {
    if (!this.originalDay || !this.weeklyMenuId) {
      return;
    }

    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.notificationService.showError('Veuillez verifier les informations du jour.');
      return;
    }

    this.saving = true;

    try {
      const raw = this.form.getRawValue();
      const payload: DailyMenuRequest = {
        weeklyMenuId: this.weeklyMenuId,
        dayOfWeek: raw.dayOfWeek as MenuDayOfWeek,
        menuDate: raw.menuDate ?? null,
        isVisibleToParents: Boolean(raw.isVisibleToParents),
        publishedAt: this.originalDay.publishedAt ?? null
      };

      const updatedDay = await firstValueFrom(this.dailyMenuService.update(this.originalDay.id, payload));
      await this.syncDishes(updatedDay.id);
      this.notificationService.showSuccess('Le jour a ete enregistre.');
      await this.router.navigate(['/events/menus', this.weeklyMenuId]);
    } catch (error) {
      this.notificationService.showError(this.getErrorMessage(error as HttpErrorResponse, "L'enregistrement du jour a echoue."));
    } finally {
      this.saving = false;
    }
  }

  deleteDay(): void {
    if (!this.originalDay || !this.weeklyMenuId) {
      return;
    }

    if (!window.confirm('Supprimer ce jour du menu ?')) {
      return;
    }

    this.dailyMenuService.delete(this.originalDay.id).subscribe({
      next: async () => {
        this.notificationService.showSuccess('Le jour a ete supprime.');
        await this.router.navigate(['/events/menus', this.weeklyMenuId]);
      },
      error: (error: HttpErrorResponse) => {
        this.notificationService.showError(this.getErrorMessage(error, 'La suppression du jour a echoue.'));
      }
    });
  }

  backToMenu(): void {
    if (this.weeklyMenuId) {
      this.router.navigate(['/events/menus', this.weeklyMenuId]);
    }
  }

  private async syncDishes(dailyMenuId: number): Promise<void> {
    const originalDishes = this.originalDay?.dishes ?? [];
    const currentIds = this.dishesArray.controls
      .map((control) => control.get('id')?.value as number | null)
      .filter((value): value is number => Boolean(value));

    const removedIds = originalDishes.map((dish) => dish.id).filter((id) => !currentIds.includes(id));
    for (const removedId of removedIds) {
      await firstValueFrom(this.dishService.delete(removedId));
    }

    for (const dishControl of this.dishesArray.controls) {
      const dishId = dishControl.get('id')?.value as number | null;
      const payload: DishRequest = {
        dailyMenuId,
        mealType: this.normalizeIncomingMealType(dishControl.get('mealType')?.value as MealCategory),
        name: dishControl.get('name')?.value ?? '',
        description: dishControl.get('description')?.value ?? '',
        allergens: dishControl.get('allergens')?.value ?? ''
      };

      const savedDish = dishId
        ? await firstValueFrom(this.dishService.update(dishId, payload))
        : await firstValueFrom(this.dishService.create(payload));

      dishControl.patchValue({
        id: savedDish.id
      });
    }
  }

  private patchForm(day: DailyMenu): void {
    this.originalDay = day;
    this.dishesArray.clear();

    this.form.patchValue({
      dayOfWeek: day.dayOfWeek,
      menuDate: day.menuDate ?? '',
      isVisibleToParents: day.isVisibleToParents
    });

    day.dishes.forEach((dish) => this.addDish(dish));
  }

  private normalizeIncomingMealType(value: MealCategory): MealCategory {
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

  private createClientKey(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }

}
