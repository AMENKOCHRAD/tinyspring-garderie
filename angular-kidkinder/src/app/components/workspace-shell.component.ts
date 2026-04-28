import { CommonModule } from '@angular/common';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';
import { WorkspaceNavGroup, workspaceNavByRole } from '../shared/tinyspring-data';
import { interval } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import {
  ParentNotification,
  ParentNotificationService
} from '../menus/parent-notification.service';

@Component({
  selector: 'app-workspace-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './workspace-shell.component.html',
  styleUrl: './workspace-shell.component.css'
})
export class WorkspaceShellComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
private readonly notificationService = inject(ParentNotificationService);

  protected readonly currentUser = this.authService.currentUser;
  protected readonly isScrolled = signal(false);
  protected readonly menuOpen = signal(false);
  protected readonly role = signal<UserRole>(this.route.snapshot.data['role'] as UserRole);
  protected readonly currentPage = signal(this.getPageFromUrl(this.router.url));
  protected readonly navGroups = computed(() => workspaceNavByRole[this.role()]);
  protected readonly activeGroup = computed(() => this.getActiveGroup(this.navGroups(), this.currentPage()));
  protected readonly subnavItems = computed(() => this.activeGroup()?.children ?? []);
  protected readonly notifications = signal<ParentNotification[]>([]);
protected readonly notificationPanelOpen = signal(false);
protected readonly toastNotification = signal<ParentNotification | null>(null);

protected readonly notificationCount = computed(() => {
  return this.notifications().filter((notification) => !notification.seen).length;
});
  protected readonly roleLabel = computed(() => (this.role() === 'PARENT' ? 'Espace parent' : 'Espace animateur'));

  public constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.menuOpen.set(false);
        this.currentPage.set(this.getPageFromUrl(event.urlAfterRedirects));
      }
    });
    if (this.role() === 'PARENT') {
  this.loadNotifications();

  interval(30000)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe(() => this.loadNotifications());
}
  }


  protected toggleNotifications(): void {
  this.notificationPanelOpen.update((value) => !value);
}

protected markNotificationAsSeen(notification: ParentNotification): void {
  if (notification.seen) {
    return;
  }

  this.notificationService.markAsSeen(notification.id).subscribe({
    next: (updated) => {
      this.notifications.update((items) =>
        items.map((item) => item.id === updated.id ? updated : item)
      );
    }
  });
}

protected closeToast(): void {
  this.toastNotification.set(null);
}

private loadNotifications(): void {const parentId = Number(this.currentUser()?.id ?? 4);

  this.notificationService.getNotifications(parentId).subscribe({
    next: (notifications) => {
      const previousIds = new Set(this.notifications().map((item) => item.id));
      const newestUnread = notifications.find(
        (item) => !item.seen && !previousIds.has(item.id)
      );

      this.notifications.set(notifications);

      if (newestUnread) {
        this.toastNotification.set(newestUnread);

        setTimeout(() => {
          if (this.toastNotification()?.id === newestUnread.id) {
            this.toastNotification.set(null);
          }
        }, 6000);
      }
    }
  });
}

  @HostListener('window:scroll')
  protected onWindowScroll(): void {
    this.isScrolled.set(window.scrollY > 8);
  }

  protected toggleMenu(): void {
    this.menuOpen.update((value) => !value);
  }

  protected logout(): void {
    this.menuOpen.set(false);
    this.authService.logout();
  }

  protected isGroupActive(group: WorkspaceNavGroup): boolean {
    return this.activeGroup()?.key === group.key;
  }

  private getPageFromUrl(url: string): string {
    const parts = url.split('?')[0].split('#')[0].split('/').filter(Boolean);
    return parts[1] || 'tableau-de-bord';
  }

  private getActiveGroup(groups: WorkspaceNavGroup[], currentPage: string): WorkspaceNavGroup | undefined {
    return groups.find((group) => {
      if (group.children?.some((child) => this.getPageFromUrl(child.route) === currentPage)) {
        return true;
      }

      return this.getPageFromUrl(group.route) === currentPage;
    });
  }
}
