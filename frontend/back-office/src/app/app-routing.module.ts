import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

<<<<<<< HEAD:frontend/back-office/src/app/app-routing.module.ts
// Layouts
import { AdminComponent } from './theme/layout/admin/admin.component';
=======
import { adminAuthGuard, adminChildAuthGuard } from './guards/admin-auth.guard';
>>>>>>> origin/gestion-evenements:frontend/angular/src/app/app-routing.module.ts
import { GuestComponent } from './theme/layout/guest/guest.component';
import { AdminComponent } from './theme/layout/admin/admin.component';

// Pages
import { DashboardProfilsEnfants } from './enfant/dashboard-profils-enfants/dashboard-profils-enfants';
import { GestionEnfantsComponent } from './enfant/gestion-enfants/gestion-enfants';
import { EtatSanitaireComponent } from './enfant/etat-sanitaire/etat-sanitaire';
import { ValidationTraitementsComponent } from './enfant/validation-traitements/validation-traitements';
import { DetailEnfantComponent } from './enfant/detail-enfant/detail-enfant';
import { ModifierEnfantComponent } from './enfant/modifier-enfant/modifier-enfant';
import { StatistiquesEnfantsComponent } from './enfant/statistiques-enfants/statistiques-enfants';

const routes: Routes = [
  {
    path: '',
    redirectTo: '/sign-in',
    pathMatch: 'full'
  },
  {
    path: '',
    component: AdminComponent,
    canActivate: [adminAuthGuard],
    canActivateChild: [adminChildAuthGuard],
    children: [
      {
<<<<<<< HEAD:frontend/back-office/src/app/app-routing.module.ts
        path: '',
<<<<<<< HEAD:frontend/back-office/src/app/app-routing.module.ts
        redirectTo: 'analytics',
=======
        redirectTo: '/login',
>>>>>>> origin/gestion-transports:frontend/angular/src/app/app-routing.module.ts
        pathMatch: 'full'
      },
      {
=======
>>>>>>> origin/gestion-evenements:frontend/angular/src/app/app-routing.module.ts
        path: 'analytics',
        loadComponent: () =>
          import('./demo/dashboard/dash-analytics.component').then(
            (c) => c.DashAnalyticsComponent
          )
      },
      {
<<<<<<< HEAD:frontend/back-office/src/app/app-routing.module.ts
<<<<<<< HEAD:frontend/back-office/src/app/app-routing.module.ts
        path: 'gestion-enfants',
        component: GestionEnfantsComponent,
        children: [
          {
            path: '',
            redirectTo: 'liste',
            pathMatch: 'full'
          },
          {
  path: 'detail/:id',
  component: DetailEnfantComponent
},
{
  path: 'modifier/:id',
  component: ModifierEnfantComponent
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
          },
          {
            path: 'statistiques',
            component: StatistiquesEnfantsComponent
          }
        ]
=======
        path: 'transport',
        loadComponent: () =>
          import('./demo/transport/transport-dashboard/transport-dashboard.component').then(
            (c) => c.TransportDashboardComponent
          )
>>>>>>> origin/gestion-transports:frontend/angular/src/app/app-routing.module.ts
=======
        path: 'events',
        loadComponent: () =>
          import('./events/pages/event-list/event-list.component').then(
            (c) => c.EventListComponent
          )
      },
      {
        path: 'events/new',
        data: { mode: 'create' },
        loadComponent: () =>
          import('./events/pages/event-create/event-create.component').then(
            (c) => c.EventCreateComponent
          )
      },
      {
        path: 'events/registrations',
        loadComponent: () =>
          import('./events/pages/event-registrations/event-registrations.component').then(
            (c) => c.EventRegistrationsComponent
          )
      },
      {
        path: 'events/menus',
        loadComponent: () =>
          import('./events/pages/weekly-menu-list/weekly-menu-list.component').then(
            (c) => c.WeeklyMenuListComponent
          )
      },
      {
        path: 'events/menus/create',
        loadComponent: () =>
          import('./events/pages/weekly-menu-form/weekly-menu-form.component').then(
            (c) => c.WeeklyMenuFormComponent
          )
      },
      {
        path: 'events/menus/:id/edit',
        loadComponent: () =>
          import('./events/pages/weekly-menu-form/weekly-menu-form.component').then(
            (c) => c.WeeklyMenuFormComponent
          )
      },
      {
        path: 'events/menus/:menuId/days/:dayId/edit',
        loadComponent: () =>
          import('./events/pages/daily-menu-form/daily-menu-form.component').then(
            (c) => c.DailyMenuFormComponent
          )
      },
      {
        path: 'events/menus/:id',
        loadComponent: () =>
          import('./events/pages/weekly-menu-detail/weekly-menu-detail.component').then(
            (c) => c.WeeklyMenuDetailComponent
          )
      },
      {
        path: 'events/:id/edit',
        data: { mode: 'edit' },
        loadComponent: () =>
          import('./events/pages/event-create/event-create.component').then(
            (c) => c.EventCreateComponent
          )
      },
      {
        path: 'events/:id/registrations',
        loadComponent: () =>
          import('./events/pages/event-registrations/event-registrations.component').then(
            (c) => c.EventRegistrationsComponent
          )
      },
      {
        path: 'events/:id',
        loadComponent: () =>
          import('./events/pages/event-detail/event-detail.component').then(
            (c) => c.EventDetailComponent
          )
>>>>>>> origin/gestion-evenements:frontend/angular/src/app/app-routing.module.ts
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
    redirectTo: '/sign-in'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
