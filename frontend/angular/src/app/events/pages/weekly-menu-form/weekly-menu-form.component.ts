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
import { WeeklyMenu, WeeklyMenuRequest, WeeklyMenuStatus } from '../../models/weekly-menu.model';
import { DailyMenuRequest, DailyMenuService } from '../../services/daily-menu.service';
import { DishRequest, DishService } from '../../services/dish.service';
import { EventNotificationService } from '../../services/event-notification.service';
import { WeeklyMenuService } from '../../services/weekly-menu.service';

interface WeeklyMenuFormVm {
  loading: boolean;
  isEdit: boolean;
  errorMessage: string;
}

@Component({
  selector: 'app-weekly-menu-form',
  standalone: true,
  imports: [SharedModule, RouterModule, ReactiveFormsModule, AsyncPipe, EventsModuleSwitcherComponent],
  templateUrl: './weekly-menu-form.component.html',
  styleUrls: ['./weekly-menu-form.component.scss']
})
export class WeeklyMenuFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly weeklyMenuService = inject(WeeklyMenuService);
  private readonly dailyMenuService = inject(DailyMenuService);
  private readonly dishService = inject(DishService);
  private readonly notificationService = inject(EventNotificationService);

  private originalMenu: WeeklyMenu | null = null;

  saving = false;
  currentStep = 1;
  expandedDayIndex = 0;
  private hasGeneratedInitialDays = false;
  private pendingAddDayFromQuery = false;

  readonly statusOptions: { value: WeeklyMenuStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'PUBLISHED', label: 'Publie' },
    { value: 'TEMPLATE', label: 'Template' }
  ];

  readonly mealOptions: { value: MealCategory; label: string }[] = [
    { value: 'ENTREE', label: 'Entree' },
    { value: 'PLAT_PRINCIPAL', label: 'Plat principal' },
    { value: 'DESSERT', label: 'Dessert' },
    { value: 'GOUTER', label: 'Gouter' }
  ];

  readonly dayOptions: { value: MenuDayOfWeek; label: string }[] = [
    { value: 'MONDAY', label: 'Lundi' },
    { value: 'TUESDAY', label: 'Mardi' },
    { value: 'WEDNESDAY', label: 'Mercredi' },
    { value: 'THURSDAY', label: 'Jeudi' },
    { value: 'FRIDAY', label: 'Vendredi' },
    { value: 'SATURDAY', label: 'Samedi' }
  ];

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    weekStartDate: ['', Validators.required],
    weekEndDate: [{ value: '', disabled: true }, Validators.required],
    status: ['DRAFT' as WeeklyMenuStatus, Validators.required],
    isTemplate: [false],
    templateName: [''],
    dailyMenus: this.fb.array([])
  });

  readonly vm$ = this.route.paramMap.pipe(
    map((params) => Number(params.get('id'))),
    switchMap((id) => {
      if (!id || Number.isNaN(id)) {
        this.resetToDefaults();
        return of<WeeklyMenuFormVm>({
          loading: false,
          isEdit: false,
          errorMessage: ''
        });
      }

      return this.weeklyMenuService.getById(id).pipe(
        map((menu) => {
          this.patchForm(menu);
          return {
            loading: false,
            isEdit: true,
            errorMessage: ''
          } satisfies WeeklyMenuFormVm;
        }),
        catchError((error: HttpErrorResponse) =>
          of<WeeklyMenuFormVm>({
            loading: false,
            isEdit: true,
            errorMessage: this.getErrorMessage(error, 'Impossible de charger le menu.')
          })
        ),
        startWith<WeeklyMenuFormVm>({
          loading: true,
          isEdit: true,
          errorMessage: ''
        })
      );
    }),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  constructor() {
    this.form.get('weekStartDate')?.valueChanges.subscribe((value) => {
      this.updateComputedEndDate(value);
      this.syncDaysWithWeekStartDate(value);
    });

    this.form.get('status')?.valueChanges.subscribe((status) => {
      if (status !== 'DRAFT' && this.form.get('isTemplate')?.value) {
        this.form.get('isTemplate')?.setValue(false, { emitEvent: false });
      }
      if (!this.form.get('isTemplate')?.value) {
        this.form.get('templateName')?.setValue('');
      }
    });

    this.form.get('isTemplate')?.valueChanges.subscribe((isTemplate) => {
      const status = this.form.get('status')?.value as WeeklyMenuStatus;
      if (isTemplate && status === 'PUBLISHED') {
        this.form.get('status')?.setValue('DRAFT');
      }
      if (!isTemplate) {
        this.form.get('templateName')?.setValue('');
      }
    });

    this.route.queryParamMap.pipe(take(1)).subscribe((params) => {
      if (params.get('addDay') === '1') {
        this.pendingAddDayFromQuery = true;
        if (this.route.snapshot.paramMap.get('id')) {
          this.currentStep = 2;
          return;
        }

        this.currentStep = 2;
        if (!this.dailyMenusArray.length) {
          this.generateInitialWeekDays();
          this.hasGeneratedInitialDays = true;
        }
        this.addDay();
        this.clearAddDayQueryParam();
      }
    });
  }

  get dailyMenusArray(): FormArray {
    return this.form.get('dailyMenus') as FormArray;
  }

  dayDishes(dayIndex: number): FormArray {
    return this.dailyMenusArray.at(dayIndex).get('dishes') as FormArray;
  }

  continueToStep2(): void {
    this.form.get('title')?.markAsTouched();
    this.form.get('weekStartDate')?.markAsTouched();
    this.form.get('status')?.markAsTouched();

    if (this.hasGeneralInfoErrors()) {
      this.notificationService.showError('Veuillez completer les informations generales.');
      return;
    }

    if (!this.hasGeneratedInitialDays && !this.dailyMenusArray.length) {
      this.generateInitialWeekDays();
      this.hasGeneratedInitialDays = true;
    }

    this.currentStep = 2;
    this.expandedDayIndex = 0;
  }

  backToStep1(): void {
    this.currentStep = 1;
  }

  addDay(day?: Partial<DailyMenu>): void {
    const defaultDay = day ?? this.buildDefaultDay();
    const group = this.fb.group({
      id: [defaultDay.id ?? null],
      dayOfWeek: [defaultDay.dayOfWeek ?? 'MONDAY', Validators.required],
      menuDate: [defaultDay.menuDate ?? '', Validators.required],
      isVisibleToParents: [defaultDay.isVisibleToParents ?? true],
      dishes: this.fb.array([])
    });

    this.dailyMenusArray.push(group);
    this.expandedDayIndex = this.dailyMenusArray.length - 1;

    if (defaultDay.dishes?.length) {
      defaultDay.dishes.forEach((dish) => this.addDish(this.dailyMenusArray.length - 1, dish));
    }
  }

  removeDay(index: number): void {
    this.dailyMenusArray.removeAt(index);
    if (!this.dailyMenusArray.length) {
      this.expandedDayIndex = 0;
      return;
    }

    if (this.expandedDayIndex >= this.dailyMenusArray.length) {
      this.expandedDayIndex = this.dailyMenusArray.length - 1;
    }
  }

  addDish(dayIndex: number, dish?: Partial<Dish>): void {
    this.dayDishes(dayIndex).push(
      this.fb.group({
        id: [dish?.id ?? null],
        mealType: [this.normalizeMealType(dish?.mealType ?? 'ENTREE'), Validators.required],
        name: [dish?.name ?? '', Validators.required],
        description: [dish?.description ?? ''],
        allergens: [dish?.allergens ?? ''],
        clientKey: [this.createClientKey()]
      })
    );
    this.expandedDayIndex = dayIndex;
  }

  removeDish(dayIndex: number, dishIndex: number): void {
    this.dayDishes(dayIndex).removeAt(dishIndex);
  }

  toggleDayPanel(index: number): void {
    this.expandedDayIndex = this.expandedDayIndex === index ? -1 : index;
  }

  isDayExpanded(index: number): boolean {
    return this.expandedDayIndex === index;
  }

  getStatusPreviewClass(): string {
    const status = this.form.get('status')?.value as WeeklyMenuStatus;
    const isTemplate = Boolean(this.form.get('isTemplate')?.value);
    if (isTemplate) {
      return 'status-template';
    }
    switch (status) {
      case 'PUBLISHED':
        return 'status-published';
      default:
        return 'status-draft';
    }
  }

  getStatusLabel(status: WeeklyMenuStatus | null | undefined): string {
    if (this.form.get('isTemplate')?.value) {
      return 'Template';
    }
    return this.statusOptions.find((option) => option.value === status)?.label ?? 'Brouillon';
  }

  getMealLabel(value: MealCategory | null | undefined): string {
    return this.mealOptions.find((option) => option.value === value)?.label ?? 'Entree';
  }

  getDayLabel(value: MenuDayOfWeek | null | undefined): string {
    return this.dayOptions.find((option) => option.value === value)?.label ?? 'Lundi';
  }

  getDishCountLabel(count: number): string {
    return `${count} plat${count > 1 ? 's' : ''}`;
  }

  getFormattedShortDate(value: string | null | undefined): string {
    if (!value) {
      return 'Date non definie';
    }

    const date = new Date(`${value}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(date);
  }

  getWeekRangeLabel(): string {
    const raw = this.form.getRawValue();
    if (!raw.weekStartDate || !raw.weekEndDate) {
      return 'Semaine a definir';
    }

    return `Semaine du ${this.getFormattedShortDate(raw.weekStartDate)} au ${this.getFormattedShortDate(
      raw.weekEndDate
    )}`;
  }

  async submit(isEdit: boolean): Promise<void> {
    this.syncDaysWithWeekStartDate(this.form.get('weekStartDate')?.value);
    this.form.markAllAsTouched();

    if (this.hasGeneralInfoErrors()) {
      this.currentStep = 1;
      this.notificationService.showError('Veuillez corriger les informations generales du menu.');
      return;
    }

    if (this.hasPlanningErrors()) {
      this.currentStep = 2;
      this.notificationService.showError('Veuillez verifier les jours et les plats renseignes.');
      return;
    }

    if (!this.hasValidDateRange()) {
      this.currentStep = 1;
      this.notificationService.showError('La plage de dates du menu est invalide.');
      return;
    }

    this.saving = true;

    try {
      const raw = this.form.getRawValue();
      const weeklyPayload: WeeklyMenuRequest = {
        title: raw.title ?? '',
        weekStartDate: raw.weekStartDate ?? null,
        weekEndDate: raw.weekEndDate ?? null,
        status: ((raw.status as WeeklyMenuStatus) === 'TEMPLATE' ? 'DRAFT' : raw.status) as WeeklyMenuStatus,
        isTemplate: Boolean(raw.isTemplate),
        templateName: raw.isTemplate ? raw.templateName || null : null
      };

      const savedWeekly =
        isEdit && this.originalMenu
          ? await firstValueFrom(this.weeklyMenuService.update(this.originalMenu.id, weeklyPayload))
          : await firstValueFrom(this.weeklyMenuService.create(weeklyPayload));

      await this.syncDailyMenus(savedWeekly.id);
      this.notificationService.showSuccess('Le menu a ete enregistre avec succes.');
      await this.router.navigate(['/events/menus', savedWeekly.id]);
    } catch (error) {
      this.notificationService.showError(
        this.getErrorMessage(error as HttpErrorResponse, "L'enregistrement du menu a echoue.")
      );
    } finally {
      this.saving = false;
    }
  }

  private async syncDailyMenus(weeklyMenuId: number): Promise<void> {
    const originalDays = this.originalMenu?.dailyMenus ?? [];
    const currentDayIds = this.dailyMenusArray.controls
      .map((control) => control.get('id')?.value as number | null)
      .filter((value): value is number => Boolean(value));

    const removedDayIds = originalDays.map((day) => day.id).filter((id) => !currentDayIds.includes(id));
    for (const removedDayId of removedDayIds) {
      await firstValueFrom(this.dailyMenuService.delete(removedDayId));
    }

    for (const dayControl of this.dailyMenusArray.controls) {
      const dayId = dayControl.get('id')?.value as number | null;
      const dayPayload: DailyMenuRequest = {
        weeklyMenuId,
        menuDate: dayControl.get('menuDate')?.value || null,
        dayOfWeek: dayControl.get('dayOfWeek')?.value as MenuDayOfWeek,
        isVisibleToParents: Boolean(dayControl.get('isVisibleToParents')?.value),
        publishedAt: null
      };

      const savedDay = dayId
        ? await firstValueFrom(this.dailyMenuService.update(dayId, dayPayload))
        : await firstValueFrom(this.dailyMenuService.create(dayPayload));

      await this.syncDishes(savedDay.id, dayControl.get('dishes') as FormArray);
    }
  }

  private async syncDishes(dailyMenuId: number, dishesArray: FormArray): Promise<void> {
    const originalDay = this.originalMenu?.dailyMenus.find((day) => day.id === dailyMenuId);
    const originalDishes = originalDay?.dishes ?? [];
    const currentDishIds = dishesArray.controls
      .map((control) => control.get('id')?.value as number | null)
      .filter((value): value is number => Boolean(value));

    const removedDishIds = originalDishes.map((dish) => dish.id).filter((id) => !currentDishIds.includes(id));
    for (const removedDishId of removedDishIds) {
      await firstValueFrom(this.dishService.delete(removedDishId));
    }

    for (const dishControl of dishesArray.controls) {
      const dishId = dishControl.get('id')?.value as number | null;

      const dishPayload: DishRequest = {
        dailyMenuId,
        mealType: this.normalizeMealType(dishControl.get('mealType')?.value as MealCategory),
        name: dishControl.get('name')?.value ?? '',
        description: dishControl.get('description')?.value ?? '',
        allergens: dishControl.get('allergens')?.value ?? ''
      };

      const savedDish = dishId
        ? await firstValueFrom(this.dishService.update(dishId, dishPayload))
        : await firstValueFrom(this.dishService.create(dishPayload));

      dishControl.patchValue({
        id: savedDish.id
      });
    }
  }

  private patchForm(menu: WeeklyMenu): void {
    this.originalMenu = menu;
    this.hasGeneratedInitialDays = true;
    this.currentStep = 1;
    this.expandedDayIndex = 0;
    this.dailyMenusArray.clear();

    this.form.patchValue({
      title: menu.title,
      weekStartDate: menu.weekStartDate ?? '',
      weekEndDate: menu.weekEndDate ?? '',
      status: (menu.status === 'TEMPLATE' ? 'DRAFT' : menu.status),
      isTemplate: Boolean(menu.isTemplate || menu.status === 'TEMPLATE'),
      templateName: menu.templateName ?? ''
    });

    menu.dailyMenus.forEach((day) => this.addDay(day));
    this.expandedDayIndex = 0;

    if (this.pendingAddDayFromQuery) {
      this.currentStep = 2;
      this.addDay();
      this.pendingAddDayFromQuery = false;
      this.clearAddDayQueryParam();
    }
  }

  private resetToDefaults(): void {
    this.originalMenu = null;
    this.currentStep = 1;
    this.expandedDayIndex = 0;
    this.hasGeneratedInitialDays = false;
    this.dailyMenusArray.clear();
    this.form.reset({
      title: '',
      weekStartDate: '',
      weekEndDate: '',
      status: 'DRAFT',
      isTemplate: false,
      templateName: ''
    });
  }

  private generateInitialWeekDays(): void {
    this.dailyMenusArray.clear();
    const startDate = this.form.get('weekStartDate')?.value;
    const start = startDate ? new Date(`${startDate}T00:00:00`) : new Date();
    const defaultDays: MenuDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

    defaultDays.forEach((dayOfWeek, index) => {
      const currentDate = new Date(start);
      currentDate.setDate(start.getDate() + index);
      this.addDay({
        dayOfWeek,
        menuDate: this.toDateInputValue(currentDate),
        isVisibleToParents: true,
        dishes: []
      });
    });
    this.expandedDayIndex = 0;
  }

  private syncDaysWithWeekStartDate(startDateValue: string | null | undefined): void {
    if (!this.dailyMenusArray.length || !startDateValue) {
      return;
    }

    const start = new Date(`${startDateValue}T00:00:00`);
    this.dailyMenusArray.controls.forEach((control, index) => {
      const currentDate = new Date(start);
      currentDate.setDate(start.getDate() + index);
      control.get('menuDate')?.setValue(this.toDateInputValue(currentDate));
      const orderedDays: MenuDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
      control.get('dayOfWeek')?.setValue(orderedDays[Math.min(index, orderedDays.length - 1)]);
    });
  }

  private updateComputedEndDate(startDateValue: string | null): void {
    if (!startDateValue) {
      this.form.get('weekEndDate')?.setValue('');
      return;
    }

    const start = new Date(`${startDateValue}T00:00:00`);
    start.setDate(start.getDate() + 6);
    this.form.get('weekEndDate')?.setValue(this.toDateInputValue(start));
  }

  private hasValidDateRange(): boolean {
    const raw = this.form.getRawValue();
    if (!raw.weekStartDate || !raw.weekEndDate) {
      return true;
    }
    return new Date(raw.weekStartDate).getTime() <= new Date(raw.weekEndDate).getTime();
  }

  private hasGeneralInfoErrors(): boolean {
    return Boolean(this.form.get('title')?.invalid || this.form.get('weekStartDate')?.invalid || this.form.get('status')?.invalid);
  }

  private hasPlanningErrors(): boolean {
    if (!this.dailyMenusArray.length) {
      return true;
    }

    return this.dailyMenusArray.controls.some((dayControl) => {
      const menuDateInvalid = dayControl.get('menuDate')?.invalid;
      const dayInvalid = dayControl.get('dayOfWeek')?.invalid;
      const dishesArray = dayControl.get('dishes') as FormArray;
      const hasInvalidDish = dishesArray.controls.some(
        (dishControl) => Boolean(dishControl.get('name')?.invalid || dishControl.get('mealType')?.invalid)
      );

      return Boolean(menuDateInvalid || dayInvalid || hasInvalidDish);
    });
  }

  private buildDefaultDay(): Partial<DailyMenu> {
    const startDate = this.form.get('weekStartDate')?.value;
    const start = startDate ? new Date(`${startDate}T00:00:00`) : new Date();
    const nextIndex = this.dailyMenusArray.length;
    const currentDate = new Date(start);
    currentDate.setDate(start.getDate() + nextIndex);
    const orderedDays: MenuDayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

    return {
      dayOfWeek: orderedDays[Math.min(nextIndex, orderedDays.length - 1)],
      menuDate: this.toDateInputValue(currentDate),
      isVisibleToParents: true,
      dishes: []
    };
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private createClientKey(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
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

  private clearAddDayQueryParam(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { addDay: null },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
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
}
