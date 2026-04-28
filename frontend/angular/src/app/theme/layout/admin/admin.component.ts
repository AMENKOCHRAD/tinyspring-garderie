// Angular Import
import { Component, HostListener, inject, OnInit } from '@angular/core';
import { RouterModule, Router, NavigationEnd, Event } from '@angular/router';
import { CommonModule } from '@angular/common';

// Project Import
import { NavBarComponent } from './nav-bar/nav-bar.component';
import { NavigationComponent } from './navigation/navigation.component';
import { BreadcrumbComponent } from '../../shared/components/breadcrumb/breadcrumb.component';
import { Footer } from './footer/footer';
import { LayoutStateService } from '../../shared/service/layout-state.service';

@Component({
  selector: 'app-admin',
  imports: [RouterModule, NavBarComponent, NavigationComponent, CommonModule, BreadcrumbComponent, Footer],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {
  private layoutState = inject(LayoutStateService);
  private router = inject(Router);

  // public props
  navCollapsed!: boolean;
  navCollapsedMob: boolean;
  windowWidth: number;
  isBoutiquePage = false;

  // constructor
  constructor() {
    this.windowWidth = window.innerWidth;
    this.navCollapsedMob = false;
  }

  ngOnInit(): void {
    this.router.events.subscribe((event: Event) => {
      if (event instanceof NavigationEnd) {
        this.isBoutiquePage = event.url.startsWith('/admin/boutique/') || event.url.startsWith('/admin/marketing');
      }
    });
  }

  @HostListener('window:resize', ['$event'])
  // eslint-disable-next-line
  onResize(event: any): void {
    this.windowWidth = event.target.innerWidth;
    if (this.windowWidth < 992) {
      document.querySelector('.pcoded-navbar')?.classList.add('menupos-static');
      if (document.querySelector('app-navigation.pcoded-navbar')?.classList.contains('navbar-collapsed')) {
        document.querySelector('app-navigation.pcoded-navbar')?.classList.remove('navbar-collapsed');
      }
    }
  }

  // public method
  navMobClick() {
    // if (this.windowWidth < 992) {
    //   if (this.navCollapsedMob && !document.querySelector('app-navigation.pcoded-navbar')?.classList.contains('mob-open')) {
    //     this.navCollapsedMob = !this.navCollapsedMob;
    //     setTimeout(() => {
    //       this.navCollapsedMob = !this.navCollapsedMob;
    //     }, 100);
    //   } else {
    //     this.navCollapsedMob = !this.navCollapsedMob;
    //   }
    // }
    this.layoutState.toggleNavCollapsedMob();
  }

  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.closeMenu();
    }
  }

  closeMenu() {
    if (document.querySelector('app-navigation.pcoded-navbar')?.classList.contains('mob-open')) {
      document.querySelector('app-navigation.pcoded-navbar')?.classList.remove('mob-open');
    }
  }
}
