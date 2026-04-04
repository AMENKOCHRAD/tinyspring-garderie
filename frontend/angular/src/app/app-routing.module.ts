// Angular Import
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// project import
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    children: [
      {
        path: '',
        redirectTo: '/analytics',
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
          import(
            './events/pages/event-registrations/event-registrations.component'
          ).then((c) => c.EventRegistrationsComponent)
      },
      {
        path: 'events/:id',
        loadComponent: () =>
          import('./events/pages/event-detail/event-detail.component').then(
            (c) => c.EventDetailComponent
          )
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
      },
      {
        path: 'sign-in',
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
