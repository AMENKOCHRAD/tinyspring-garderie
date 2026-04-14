import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../shared/auth.service';
import { UserRole } from '../shared/auth.models';
import { ToastOutletComponent } from './toast-outlet.component';
import { CartService } from '../shared/cart.service';
import { WorkspaceNavGroup, workspaceNavByRole } from '../shared/tinyspring-data';

@Component({
  selector: 'app-workspace-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, ToastOutletComponent],
  templateUrl: './workspace-shell.component.html',
  styleUrl: './workspace-shell.component.css'
})
export class WorkspaceShellComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly currentUser = this.authService.currentUser;
  protected readonly isScrolled = signal(false);
  protected readonly menuOpen = signal(false);
  protected readonly role = signal<UserRole>(this.route.snapshot.data['role'] as UserRole);
  protected readonly currentPage = signal(this.getPageFromUrl(this.router.url));
  protected readonly navGroups = computed(() => workspaceNavByRole[this.role()]);
  protected readonly activeGroup = computed(() => this.getActiveGroup(this.navGroups(), this.currentPage()));
  protected readonly subnavItems = computed(() => this.activeGroup()?.children ?? []);
  protected readonly cartItemCount = this.cartService.itemCount;
  protected readonly notificationCount = computed(() => (this.role() === 'PARENT' ? 3 : 5));
  protected readonly roleLabel = computed(() => (this.role() === 'PARENT' ? 'Espace parent' : 'Espace animateur'));
  protected readonly cartBadgePulse = signal(false);
  protected readonly cartIconBounce = signal(false);

  public constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.menuOpen.set(false);
        this.currentPage.set(this.getPageFromUrl(event.urlAfterRedirects));
      }
    });

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
