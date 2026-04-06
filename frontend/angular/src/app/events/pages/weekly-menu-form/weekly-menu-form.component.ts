import { AsyncPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

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
  private readonly selectedDishFiles = new Map<string, File>();
  private readonly previewObjectUrls = new Map<string, string>();

  saving = false;
  currentStep = 1;
  expandedDayIndex = 0;
  private hasGeneratedInitialDays = false;

  readonly statusOptions: { value: WeeklyMenuStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'PUBLISHED', label: 'Publie' },
    { value: 'TEMPLATE', label: 'Template' }
  ];

  readonly mealOptions: { value: MealCategory; label: string }[] = [
    { value: 'STARTER', label: 'Entree' },
    { value: 'MAIN', label: 'Plat principal' },
    { value: 'SIDE', label: 'Accompagnement' },
    { value: 'DESSERT', label: 'Dessert' },
    { value: 'SNACK', label: 'Gouter' }
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
      if (!this.originalMenu && this.hasGeneratedInitialDays) {
        this.regenerateInitialDaysFromStartDate();
      }
    });

    this.form.get('status')?.valueChanges.subscribe((status) => {
      if (status !== 'TEMPLATE') {
        this.form.get('templateName')?.setValue('');
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
        mealType: [dish?.mealType ?? 'STARTER', Validators.required],
        name: [dish?.name ?? '', Validators.required],
        description: [dish?.description ?? ''],
        allergens: [dish?.allergens ?? ''],
        photoUrl: [dish?.photoUrl ?? ''],
        previewUrl: [dish?.photoUrl ?? ''],
        clientKey: [this.createClientKey()]
      })
    );
    this.expandedDayIndex = dayIndex;
  }

  removeDish(dayIndex: number, dishIndex: number): void {
    const control = this.dayDishes(dayIndex).at(dishIndex);
    const clientKey = control.get('clientKey')?.value as string;
    this.selectedDishFiles.delete(clientKey);
    this.revokePreviewUrl(clientKey);
    this.dayDishes(dayIndex).removeAt(dishIndex);
  }

  toggleDayPanel(index: number): void {
    this.expandedDayIndex = this.expandedDayIndex === index ? -1 : index;
  }

  isDayExpanded(index: number): boolean {
    return this.expandedDayIndex === index;
  }

  onDishFileSelected(dayIndex: number, dishIndex: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    const dishGroup = this.dayDishes(dayIndex).at(dishIndex);
    const clientKey = dishGroup.get('clientKey')?.value as string;
    this.selectedDishFiles.set(clientKey, file);
    this.revokePreviewUrl(clientKey);

    const previewUrl = URL.createObjectURL(file);
    this.previewObjectUrls.set(clientKey, previewUrl);
    dishGroup.patchValue({ previewUrl });
    input.value = '';
  }

  getDishPreview(dayIndex: number, dishIndex: number): string | null {
    return (this.dayDishes(dayIndex).at(dishIndex).get('previewUrl')?.value as string) || null;
  }

  getStatusPreviewClass(): string {
    const status = this.form.get('status')?.value as WeeklyMenuStatus;
    switch (status) {
      case 'PUBLISHED':
        return 'status-published';
      case 'TEMPLATE':
        return 'status-template';
      default:
        return 'status-draft';
    }
  }

  getStatusLabel(status: WeeklyMenuStatus | null | undefined): string {
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
      const photoUploadWarnings: string[] = [];
      const raw = this.form.getRawValue();
      const weeklyPayload: WeeklyMenuRequest = {
        title: raw.title ?? '',
        weekStartDate: raw.weekStartDate ?? null,
        weekEndDate: raw.weekEndDate ?? null,
        status: raw.status as WeeklyMenuStatus,
        isTemplate: raw.status === 'TEMPLATE',
        templateName: raw.status === 'TEMPLATE' ? raw.templateName || null : null
      };

      const savedWeekly =
        isEdit && this.originalMenu
          ? await firstValueFrom(this.weeklyMenuService.update(this.originalMenu.id, weeklyPayload))
          : await firstValueFrom(this.weeklyMenuService.create(weeklyPayload));

      await this.syncDailyMenus(savedWeekly.id, photoUploadWarnings);

      if (photoUploadWarnings.length) {
        this.notificationService.showError(
          `Le menu a ete enregistre, mais ${photoUploadWarnings.length} photo(s) n'ont pas pu etre envoyee(s).`
        );
      } else {
        this.notificationService.showSuccess('Le menu a ete enregistre avec succes.');
      }
      await this.router.navigate(['/events/menus', savedWeekly.id]);
    } catch (error) {
      this.notificationService.showError(
        this.getErrorMessage(error as HttpErrorResponse, "L'enregistrement du menu a echoue.")
      );
    } finally {
      this.saving = false;
    }
  }

  private async syncDailyMenus(weeklyMenuId: number, photoUploadWarnings: string[]): Promise<void> {
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

      await this.syncDishes(savedDay.id, dayControl.get('dishes') as FormArray, photoUploadWarnings);
    }
  }

  private async syncDishes(dailyMenuId: number, dishesArray: FormArray, photoUploadWarnings: string[]): Promise<void> {
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
      const clientKey = dishControl.get('clientKey')?.value as string;

      const dishPayload: DishRequest = {
        dailyMenuId,
        mealType: dishControl.get('mealType')?.value as MealCategory,
        name: dishControl.get('name')?.value ?? '',
        description: dishControl.get('description')?.value ?? '',
        photoUrl: dishControl.get('photoUrl')?.value ?? '',
        allergens: dishControl.get('allergens')?.value ?? ''
      };

      const savedDish = dishId
        ? await firstValueFrom(this.dishService.update(dishId, dishPayload))
        : await firstValueFrom(this.dishService.create(dishPayload));

      dishControl.patchValue({
        id: savedDish.id,
        photoUrl: savedDish.photoUrl ?? '',
        previewUrl: savedDish.photoUrl ?? ''
      });

      const pendingFile = this.selectedDishFiles.get(clientKey);
      if (pendingFile) {
        try {
          const uploadedDish = await firstValueFrom(this.dishService.uploadPhoto(savedDish.id, pendingFile));
          dishControl.patchValue({
            photoUrl: uploadedDish.photoUrl ?? '',
            previewUrl: uploadedDish.photoUrl ?? ''
          });
          this.selectedDishFiles.delete(clientKey);
          this.revokePreviewUrl(clientKey);
        } catch {
          photoUploadWarnings.push(dishPayload.name || 'Plat sans nom');
        }
      }
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
      status: menu.status,
      templateName: menu.templateName ?? ''
    });

    menu.dailyMenus.forEach((day) => this.addDay(day));
    this.expandedDayIndex = 0;
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

  private regenerateInitialDaysFromStartDate(): void {
    if (this.originalMenu || !this.dailyMenusArray.length) {
      return;
    }

    const startDate = this.form.get('weekStartDate')?.value;
    if (!startDate) {
      return;
    }

    const start = new Date(`${startDate}T00:00:00`);
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

  private revokePreviewUrl(clientKey: string): void {
    const previewUrl = this.previewObjectUrls.get(clientKey);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      this.previewObjectUrls.delete(clientKey);
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
