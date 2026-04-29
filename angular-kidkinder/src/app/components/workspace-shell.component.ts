import { CommonModule } from '@angular/common';
<<<<<<< HEAD
<<<<<<< HEAD
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';
import { WorkspaceNavGroup, workspaceNavByRole } from '../shared/tinyspring-data';
import { NotificationsService } from '../services/notifications.service';
=======
import {
  Component,
  DestroyRef,
  HostListener,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { interval } from 'rxjs';

import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';
import { WorkspaceNavGroup, workspaceNavByRole } from '../shared/tinyspring-data';

import {
  ParentNotification,
  ParentNotificationService
} from '../menus/parent-notification.service';
>>>>>>> origin/gestion-evenements
=======
import { Component, DestroyRef, HostListener, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';
import { ToastOutletComponent } from './toast-outlet.component';
import { CartService } from '../shared/cart.service';
import { WorkspaceNavGroup, workspaceNavByRole } from '../shared/tinyspring-data';
>>>>>>> origin/gestion_boutique

@Component({
  selector: 'app-workspace-shell',
  standalone: true,
<<<<<<< HEAD
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
=======
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastOutletComponent],
>>>>>>> origin/gestion_boutique
  templateUrl: './workspace-shell.component.html',
  styleUrl: './workspace-shell.component.css'
})
export class WorkspaceShellComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
<<<<<<< HEAD
<<<<<<< HEAD
  private readonly notificationsService = inject(NotificationsService);
=======
  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);
>>>>>>> origin/gestion_boutique

  protected readonly currentUser = this.authService.currentUser;
  protected readonly isScrolled = signal(false);
  protected readonly menuOpen = signal(false);
  protected readonly role = signal<UserRole>(this.route.snapshot.data['role'] as UserRole);
  protected readonly currentPage = signal(this.getPageFromUrl(this.router.url));
  protected readonly navGroups = computed(() => workspaceNavByRole[this.role()]);
  protected readonly activeGroup = computed(() => this.getActiveGroup(this.navGroups(), this.currentPage()));
  protected readonly subnavItems = computed(() => this.activeGroup()?.children ?? []);
<<<<<<< HEAD
  protected readonly notificationCount = computed(() =>
    this.role() === 'PARENT' ? this.notificationsService.parentUnreadObservationsCount() : 0
  );
  protected readonly roleLabel = computed(() => (this.role() === 'PARENT' ? 'Espace parent' : 'Espace animateur'));

  public constructor() {
    this.notificationsService.start();
=======
  protected readonly cartItemCount = this.cartService.itemCount;
  protected readonly notificationCount = computed(() => (this.role() === 'PARENT' ? 3 : 5));
  protected readonly roleLabel = computed(() => (this.role() === 'PARENT' ? 'Espace parent' : 'Espace animateur'));
  protected readonly cartBadgePulse = signal(false);
  protected readonly cartIconBounce = signal(false);

  public constructor() {
>>>>>>> origin/gestion_boutique
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.menuOpen.set(false);
        this.currentPage.set(this.getPageFromUrl(event.urlAfterRedirects));
      }
    });
<<<<<<< HEAD
=======

    let previousMutation = this.cartService.mutationTick();
    effect(() => {
      const currentMutation = this.cartService.mutationTick();

      if (currentMutation !== previousMutation) {
        previousMutation = currentMutation;
        this.cartBadgePulse.set(true);
        const timeoutId = window.setTimeout(() => this.cartBadgePulse.set(false), 450);
        this.destroyRef.onDestroy(() => window.clearTimeout(timeoutId));
      }
    });

    let previousArrival = this.cartService.arrivalTick();
    effect(() => {
      const currentArrival = this.cartService.arrivalTick();

      if (currentArrival !== previousArrival) {
        previousArrival = currentArrival;
        this.cartIconBounce.set(true);
        const timeoutId = window.setTimeout(() => this.cartIconBounce.set(false), 220);
        this.destroyRef.onDestroy(() => window.clearTimeout(timeoutId));
      }
    });
>>>>>>> origin/gestion_boutique
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

<<<<<<< HEAD
  protected openNotifications(): void {
    if (this.role() === 'PARENT') {
      void this.router.navigate(['/parent/changements']);
    }
  }

=======
>>>>>>> origin/gestion_boutique
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
<<<<<<< HEAD
=======
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificationService = inject(ParentNotificationService);

  protected readonly currentUser = this.authService.currentUser;
  // ===== HEADER STATE =====
protected readonly isScrolled = signal(false);
protected readonly menuOpen = signal(false);

// ===== NAVIGATION =====
protected readonly navGroups = computed(() => workspaceNavByRole[this.role()]);

protected readonly subnavItems = computed(() => {
  const groups = this.navGroups();
  if (!groups?.length) return [];

  return groups[0]?.children ?? [];
});

// ===== ROLE LABEL =====
protected readonly roleLabel = computed(() =>
  this.role() === 'PARENT' ? 'Espace parent' : 'Espace animateur'
);

protected toggleMenu(): void {
  this.menuOpen.update((value: boolean) => !value);
}

protected logout(): void {
  this.authService.logout();
}

protected isGroupActive(group: WorkspaceNavGroup): boolean {
  return false; // simple fallback pour éviter crash
}



  protected readonly role = signal<UserRole>(
    this.route.snapshot.data['role'] as UserRole
  );

  protected readonly notifications = signal<ParentNotification[]>([]);
  protected readonly notificationPanelOpen = signal(false);
  protected readonly toastNotification = signal<ParentNotification | null>(null);

  protected readonly activeNotifTab = signal<'all' | 'ALLERGEN' | 'EVENT' | 'MENU'>('all');

  protected readonly notificationCount = computed(() =>
    this.notifications().filter(n => !n.seen).length
  );

  protected readonly filteredNotifications = computed(() => {
    const tab = this.activeNotifTab();

    if (tab === 'all') return this.notifications();

    if (tab === 'ALLERGEN') {
      return this.notifications().filter(n => n.type.startsWith('ALLERGEN'));
    }

    if (tab === 'EVENT') {
      return this.notifications().filter(n => n.relatedEntityType === 'EVENT');
    }

    return this.notifications().filter(n => n.relatedEntityType === 'MENU');
  });

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.notificationPanelOpen.set(false);
        this.closeNavDropdown();
      }
    });

    if (this.role() === 'PARENT') {
      this.loadNotifications();

      interval(10000) // refresh toutes les 10s
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.loadNotifications());
    }
  }

  // ===============================
  // 🔔 UI ACTIONS
  // ===============================

  protected toggleNotifications(): void {
    this.notificationPanelOpen.update(v => !v);
  }

  protected setNotifTab(tab: 'all' | 'ALLERGEN' | 'EVENT' | 'MENU'): void {
    this.activeNotifTab.set(tab);
  }

  protected closeToast(): void {
    this.toastNotification.set(null);
  }

  protected readonly openedNavKey = signal<string | null>(null);

protected toggleNavDropdown(key: string): void {
  this.openedNavKey.update(current => current === key ? null : key);
}

protected closeNavDropdown(): void {
  this.openedNavKey.set(null);
}

  // ===============================
  // 🔄 LOAD NOTIFICATIONS
  // ===============================

  private loadNotifications(): void {
    const parentId = Number(this.currentUser()?.id);

    if (!parentId) {
      console.warn('❌ parentId manquant');
      return;
    }

    this.notificationService.getNotifications(parentId).subscribe({
      next: (notifications) => {
        console.log('📥 notifications reçues:', notifications);

        // 🔥 IMPORTANT : toast sur première notif non lue
        const firstUnread = notifications.find(n => !n.seen);

        this.notifications.set(notifications);

        if (firstUnread && !this.toastNotification()) {
          this.showToast(firstUnread);
        }
      },
      error: (err) => {
        console.error('❌ erreur notifications', err);
      }
    });
  }

  // ===============================
  // 🔥 TOAST
  // ===============================

  private showToast(notification: ParentNotification): void {
    this.toastNotification.set(notification);

    setTimeout(() => {
      if (this.toastNotification()?.id === notification.id) {
        this.toastNotification.set(null);
      }
    }, 5000);
  }

  protected navigateFromToast(notification: ParentNotification): void {
    this.markNotificationAsSeen(notification);

    if (notification.relatedEntityType === 'EVENT') {
      this.router.navigate(['/parent/activites']);
    }

    if (notification.relatedEntityType === 'MENU') {
      this.router.navigate(['/parent/menus']);
    }

    this.closeToast();
  }

  // ===============================
  // ✅ MARK READ
  // ===============================

  protected markNotificationAsSeen(notification: ParentNotification): void {
    if (notification.seen) return;

    this.notificationService.markAsSeen(notification.id).subscribe({
      next: (updated) => {
        this.notifications.update(list =>
          list.map(n => n.id === updated.id ? updated : n)
        );
      }
    });
  }

  protected markAllRead(): void {
    const parentId = Number(this.currentUser()?.id);

    if (!parentId) return;

    this.notificationService.markAllRead(parentId).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => ({ ...n, seen: true }))
        );
      }
    });
  }

  protected onNotifClick(notification: ParentNotification): void {
    this.markNotificationAsSeen(notification);
    this.navigateFromToast(notification);
    this.notificationPanelOpen.set(false);
  }

  // ===============================
  // SCROLL
  // ===============================

  @HostListener('window:scroll')
  protected onScroll(): void {
    this.isScrolled.set(window.scrollY > 10);
  }
}
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
