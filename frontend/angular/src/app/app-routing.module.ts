import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// Layouts
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';

// Pages
import { DashboardProfilsEnfants } from './enfant/dashboard-profils-enfants/dashboard-profils-enfants';
import { GestionEnfantsComponent } from './enfant/gestion-enfants/gestion-enfants';
import { EtatSanitaireComponent } from './enfant/etat-sanitaire/etat-sanitaire';
import { ValidationTraitementsComponent } from './enfant/validation-traitements/validation-traitements';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    children: [
      {
        path: '',
        redirectTo: 'analytics',
        pathMatch: 'full'
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./demo/dashboard/dash-analytics.component').then(
            (c) => c.DashAnalyticsComponent
          )
      },
      {
        path: 'gestion-enfants',
        component: GestionEnfantsComponent,
        children: [
          {
            path: '',
            redirectTo: 'liste',
            pathMatch: 'full'
          },
           {
      path: 'validation-traitements',
      component: ValidationTraitementsComponent
    },
          {
            path: 'liste',
            component: DashboardProfilsEnfants
          },
          {
            path: 'etat-sanitaire',
            component: EtatSanitaireComponent
          }
        ]
      },
      {
        path: 'component',
        loadChildren: () =>
          import('./demo/ui-element/ui-basic.module').then(
            (m) => m.UiBasicModule
          )
      },
      {
        path: 'chart',
        loadComponent: () =>
          import('./demo/chart-maps/core-apex.component').then(
            (c) => c.CoreApexComponent
          )
      },
      {
        path: 'forms',
        loadComponent: () =>
          import('./demo/forms/form-elements/form-elements.component').then(
            (c) => c.FormElementsComponent
          )
      },
      {
        path: 'tables',
        loadComponent: () =>
          import('./demo/tables/tbl-bootstrap/tbl-bootstrap.component').then(
            (c) => c.TblBootstrapComponent
          )
      },
      {
        path: 'sample-page',
        loadComponent: () =>
          import('./demo/other/sample-page/sample-page.component').then(
            (c) => c.SamplePageComponent
          )
      }
    ]
  },
  {
    path: '',
    component: GuestComponent,
    children: [
      {
        path: 'register',
        loadComponent: () =>
          import('./demo/pages/authentication/sign-up/sign-up.component').then(
            (c) => c.SignUpComponent
          )
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./demo/pages/authentication/sign-in/sign-in.component').then(
            (c) => c.SignInComponent
          )
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}