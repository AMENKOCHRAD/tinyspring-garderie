import { Routes } from '@angular/router';

import { guestOnlyGuard, roleGuard, sessionGuard } from './auth/guards/auth.guard';

export const appRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./components/public/home-page.component').then((c) => c.HomePageComponent)
  },
  {
    path: 'login',
    canActivate: [guestOnlyGuard],
    loadComponent: () => import('./auth/components/login.component').then((c) => c.LoginComponent)
  },
  {
    path: 'parent',
    canActivate: [sessionGuard, roleGuard],
    data: { roles: ['PARENT'] },
    loadComponent: () =>
      import('./components/parent/parent-dashboard.component').then((c) => c.ParentDashboardComponent)
  },
  {
    path: 'demandes',
    canActivate: [sessionGuard, roleGuard],
    data: { roles: ['PARENT'] },
    loadComponent: () =>
      import('./components/demandes/parent-demandes.component').then((c) => c.ParentDemandesComponent)
  },
  {
    path: 'animatrice',
    canActivate: [sessionGuard, roleGuard],
    data: { roles: ['ANIMATRICE'] },
    loadComponent: () =>
      import('./components/animatrice/animatrice-dashboard.component').then((c) => c.AnimatriceDashboardComponent)
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
