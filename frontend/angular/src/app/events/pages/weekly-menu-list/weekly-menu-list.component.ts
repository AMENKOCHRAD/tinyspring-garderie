import { AsyncPipe, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { BehaviorSubject, combineLatest, of } from 'rxjs';
import { catchError, map, shareReplay, startWith, switchMap } from 'rxjs/operators';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { EventsModuleSwitcherComponent } from '../../components/events-module-switcher/events-module-switcher.component';
import { WeeklyMenu, WeeklyMenuStatus } from '../../models/weekly-menu.model';
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

  readonly toast$ = this.notificationService.message$;

  readonly vm$ = this.refreshSubject.pipe(
    switchMap(() =>
      this.weeklyMenuService.getAll().pipe(
        map(
          (menus) =>
            ({
              loading: false,
              menus,
              errorMessage: ''
            }) satisfies WeeklyMenuListVm
        ),
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

  goToCreate(): void {
    this.router.navigate(['/events/menus/create']);
  }

  goToDetails(id: number): void {
    this.router.navigate(['/events/menus', id]);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/events/menus', id, 'edit']);
  }

  duplicateMenu(menu: WeeklyMenu): void {
    this.weeklyMenuService.duplicate(menu.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('Le menu modèle a été dupliqué.');
        this.refreshSubject.next();
      },
      error: (error: HttpErrorResponse) => {
        this.notificationService.showError(this.getErrorMessage(error, 'La duplication a échoué.'));
      }
    });
  }

  deleteMenu(menu: WeeklyMenu): void {
    if (!window.confirm(`Supprimer le menu "${menu.title}" ?`)) {
      return;
    }

    this.weeklyMenuService.delete(menu.id).subscribe({
      next: () => {
        this.notificationService.showSuccess('Le menu a été supprimé.');
        this.refreshSubject.next();
      },
      error: (error: HttpErrorResponse) => {
        this.notificationService.showError(this.getErrorMessage(error, 'La suppression a échoué.'));
      }
    });
  }

  getStatusClass(status: WeeklyMenuStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'status-published';
      case 'TEMPLATE':
        return 'status-template';
      default:
        return 'status-draft';
    }
  }

  formatStatus(status: WeeklyMenuStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'Publié';
      case 'TEMPLATE':
        return 'Template';
      default:
        return 'Brouillon';
    }
  }

  getDaysLabel(menu: WeeklyMenu): string {
    return `${menu.dailyMenus.length} jour${menu.dailyMenus.length > 1 ? 's' : ''} configuré${menu.dailyMenus.length > 1 ? 's' : ''}`;
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
