import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ParentMenusService } from './parent-menus.service';
import { DecoratedDailyMenu, MenuDish, ParentMenusData } from './parent-menus.models';

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
  protected readonly selectedDailyMenuId = signal<number | null>(null);
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly selectedDailyMenu = computed(() => {
    const data = this.menusData();
    const selectedId = this.selectedDailyMenuId();

    if (!data) {
      return null;
    }

    if (selectedId != null) {
      return data.visibleDailyMenus.find((menu) => menu.id === selectedId) ?? data.todayMenu;
    }

    return data.todayMenu;
  });

  public constructor() {
    this.menusService
      .getMenus()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.menusData.set(data);
          this.loading.set(false);
          this.errorMessage.set('');
          this.selectedDailyMenuId.set(data.todayMenu?.id ?? data.visibleDailyMenus[0]?.id ?? null);
        },
        error: (error: HttpErrorResponse | Error) => {
          this.loading.set(false);
          this.errorMessage.set(this.getErrorMessage(error));
        }
      });
  }

  protected selectDay(menu: DecoratedDailyMenu): void {
    this.selectedDailyMenuId.set(menu.id);
  }

  protected getMealTitle(type: string): string {
    switch ((type || '').toUpperCase()) {
      case 'ENTREE':
        return 'Entree';
      case 'PLAT_PRINCIPAL':
        return 'Plat du jour';
      case 'DESSERT':
        return 'Dessert';
      case 'GOUTER':
        return 'Gouter';
      default:
        return 'Plat';
    }
  }

  protected getDishSummary(dishes: MenuDish[]): string {
    if (!dishes.length) {
      return 'Aucun plat renseigne';
    }

    return dishes.map((dish) => dish.name).join(', ');
  }

  protected formatWeekRange(start: string | null, end: string | null): string {
    if (!start || !end) {
      return 'Semaine publiee';
    }

    const startDate = new Date(start);
    const endDate = new Date(end);

    const startLabel = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' }).format(startDate);
    const endLabel = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long' }).format(endDate);

    return `${startLabel} au ${endLabel}`;
  }

  protected getAllergens(dishes: MenuDish[]): string[] {
    const values = dishes
      .flatMap((dish) => `${dish.allergens || ''},${dish.allergenConflictFlags || ''}`.split(','))
      .map((value) => value.trim())
      .filter(Boolean);

    return Array.from(new Set(values));
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

    return "Impossible de charger les menus pour le moment.";
  }
}
