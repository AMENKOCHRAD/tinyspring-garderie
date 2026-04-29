import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import {
  WeeklyMenu,
  WeeklyMenuDailyRequest,
  WeeklyMenuDishRequest,
  WeeklyMenuRequest,
  WeeklyMenuStatus
} from '../../models/weekly-menu.model';
import { EventNotificationService } from '../../services/event-notification.service';
import { WeeklyMenuService } from '../../services/weekly-menu.service';

interface WeeklyMenuListVm {
  loading: boolean;
  menus: WeeklyMenu[];
  errorMessage: string;
}

@Component({
  selector: 'app-weekly-menu-list',
  standalone: true,
  imports: [SharedModule, RouterModule, AsyncPipe, DatePipe, NgClass, EventsModuleSwitcherComponent],
  templateUrl: './weekly-menu-list.component.html',
  styleUrls: ['./weekly-menu-list.component.scss']
})
export class WeeklyMenuListComponent {
  private readonly weeklyMenuService = inject(WeeklyMenuService);
  private readonly notificationService = inject(EventNotificationService);
  private readonly router = inject(Router);
  private readonly refreshSubject = new BehaviorSubject<void>(void 0);

  private menusState: WeeklyMenu[] | null = null;
  private readonly publishingIds = new Set<number>();

  readonly toast$ = this.notificationService.message$;

  readonly vm$ = this.refreshSubject.pipe(
    switchMap(() =>
      this.weeklyMenuService.getAll().pipe(
        map((menus) => {
          this.menusState = menus;
          return {
            loading: false,
            menus,
            errorMessage: ''
          } satisfies WeeklyMenuListVm;
        }),
        catchError((error: HttpErrorResponse) =>
          of<WeeklyMenuListVm>({
            loading: false,
            menus: [],
            errorMessage: this.getErrorMessage(error, 'Impossible de charger les menus.')
          })
        ),
        startWith<WeeklyMenuListVm>({
          loading: true,
          menus: [],
          errorMessage: ''
        })
      )
    ),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  dismissToast(): void {
    this.notificationService.clear();
  }

  getCurrentMenus(fallback: WeeklyMenu[]): WeeklyMenu[] {
    return this.menusState ?? fallback;
  }

  goToCreate(): void {
    this.router.navigate(['/events/menus/create']);
  }

  goToDetails(id: number): void {
    this.router.navigate(['/events/menus', id]);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/events/menus', id, 'edit']);
  }

  publishMenu(menu: WeeklyMenu): void {
    if (this.publishingIds.has(menu.id) || this.isTemplateMenu(menu) || menu.status !== 'DRAFT') {
      return;
    }

    const previousStatus = menu.status;
    this.patchMenu(menu.id, { status: 'PUBLISHED' });
    this.publishingIds.add(menu.id);

    const payload = this.buildWeeklyPayload(menu, {
      status: 'PUBLISHED'
    });

    this.weeklyMenuService.update(menu.id, payload).subscribe({
      next: (updatedMenu) => {
        this.publishingIds.delete(menu.id);
        this.replaceMenu(updatedMenu);
        this.notificationService.showSuccess('Le menu a ete publie.');
      },
      error: (error: HttpErrorResponse) => {
        this.publishingIds.delete(menu.id);
        this.patchMenu(menu.id, { status: previousStatus });
        this.notificationService.showError(this.getErrorMessage(error, 'La publication a echoue.'));
      }
    });
  }

  isPublishing(menuId: number): boolean {
    return this.publishingIds.has(menuId);
  }

  markAsTemplate(menu: WeeklyMenu): void {
    if (this.isTemplateMenu(menu)) {
      return;
    }

    const suggestedName = menu.templateName?.trim() || menu.title;
    const templateNameInput = window.prompt('Nom du template :', suggestedName);
    if (templateNameInput === null) {
      return;
    }
    const templateName = templateNameInput.trim();

    const previousTemplate = menu.isTemplate;
    const previousName = menu.templateName ?? null;
    this.patchMenu(menu.id, {
      isTemplate: true,
      templateName: templateName || suggestedName
    });

    const payload = this.buildWeeklyPayload(menu, {
      isTemplate: true,
      status: menu.status === 'TEMPLATE' ? 'DRAFT' : menu.status,
      templateName: templateName || suggestedName
    });

    this.weeklyMenuService.update(menu.id, payload).subscribe({
      next: (updatedMenu) => {
        this.replaceMenu(updatedMenu);
        this.notificationService.showSuccess('Le menu est maintenant reutilisable comme template.');
      },
      error: (error: HttpErrorResponse) => {
        this.patchMenu(menu.id, {
          isTemplate: previousTemplate,
          status: menu.status,
          templateName: previousName
        });
        this.notificationService.showError(this.getErrorMessage(error, 'Impossible de rendre ce menu reutilisable.'));
      }
    });
  }

  duplicateMenu(menu: WeeklyMenu): void {
    this.weeklyMenuService.duplicate(menu.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('Le menu modele a ete duplique.');
        this.refreshSubject.next();
      },
      error: (error: HttpErrorResponse) => {
        this.notificationService.showError(this.getErrorMessage(error, 'La duplication a echoue.'));
      }
    });
  }

  deleteMenu(menu: WeeklyMenu): void {
    if (!window.confirm(`Supprimer le menu "${menu.title}" ?`)) {
      return;
    }

    this.weeklyMenuService.delete(menu.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('Le menu a ete supprime.');
        this.menusState = (this.menusState ?? []).filter((item) => item.id !== menu.id);
      },
      error: (error: HttpErrorResponse) => {
        this.notificationService.showError(this.getErrorMessage(error, 'La suppression a echoue.'));
      }
    });
  }

  getStatusClass(menu: WeeklyMenu): string {
    if (this.isTemplateMenu(menu)) {
      return 'status-template';
    }
    switch (menu.status) {
      case 'PUBLISHED':
        return 'status-published';
      default:
        return 'status-draft';
    }
  }

  formatStatus(menu: WeeklyMenu): string {
    if (this.isTemplateMenu(menu)) {
      return 'Template';
    }
    switch (menu.status) {
      case 'PUBLISHED':
        return 'Publie';
      default:
        return 'Brouillon';
    }
  }

  getDaysLabel(menu: WeeklyMenu): string {
    return `${menu.dailyMenus.length} jour${menu.dailyMenus.length > 1 ? 's' : ''}`;
  }

  getDishCount(menu: WeeklyMenu): number {
    return menu.dailyMenus.reduce((total, day) => total + day.dishes.length, 0);
  }

  getDishLabel(menu: WeeklyMenu): string {
    const count = this.getDishCount(menu);
    return `${count} plat${count > 1 ? 's' : ''}`;
  }

  getTemplateSubtitle(menu: WeeklyMenu): string {
    if (!this.isTemplateMenu(menu)) {
      return '';
    }

    return menu.templateName?.trim() ? menu.templateName : 'Template reutilisable';
  }

  private replaceMenu(updatedMenu: WeeklyMenu): void {
    this.menusState = (this.menusState ?? []).map((menu) => (menu.id === updatedMenu.id ? updatedMenu : menu));
  }

  private patchMenu(menuId: number, patch: Partial<WeeklyMenu>): void {
    this.menusState = (this.menusState ?? []).map((menu) => (menu.id === menuId ? { ...menu, ...patch } : menu));
  }

  private buildWeeklyPayload(menu: WeeklyMenu, overrides?: Partial<WeeklyMenuRequest>): WeeklyMenuRequest {
    const dailyMenus: WeeklyMenuDailyRequest[] = menu.dailyMenus.map((day) => ({
      weeklyMenuId: day.weeklyMenuId,
      menuDate: day.menuDate,
      dayOfWeek: day.dayOfWeek,
      isVisibleToParents: day.isVisibleToParents,
      publishedAt: day.publishedAt ?? null,
      dishes: day.dishes.map(
        (dish): WeeklyMenuDishRequest => ({
          dailyMenuId: dish.dailyMenuId,
          mealType: this.normalizeMealTypeForBackend(dish.mealType),
          name: dish.name,
          description: dish.description ?? '',
          allergens: dish.allergens ?? ''
        })
      )
    }));

    return {
      title: menu.title,
      weekStartDate: menu.weekStartDate,
      weekEndDate: menu.weekEndDate,
      status: menu.status === 'TEMPLATE' ? 'DRAFT' : menu.status,
      isTemplate: this.isTemplateMenu(menu),
      templateName: menu.templateName ?? null,
      dailyMenus,
      ...overrides
    };
  }

  isTemplateMenu(menu: WeeklyMenu): boolean {
    return Boolean(menu.isTemplate) || menu.status === 'TEMPLATE';
  }

  private normalizeMealTypeForBackend(mealType: string): WeeklyMenuDishRequest['mealType'] {
    switch (mealType) {
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
