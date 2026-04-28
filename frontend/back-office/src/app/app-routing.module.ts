// Angular Import
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// project import
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';
import { AuthGuard } from './services/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    canActivate: [AuthGuard],
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
        path: 'salles',
        loadComponent: () => import('./demo/gestion-classes/salles/salles.component').then((c) => c.SallesComponent)
      },
      {
        path: 'classes',
        loadComponent: () => import('./demo/gestion-classes/classes/classes.component').then((c) => c.ClassesComponent)
      },
      {
        path: 'groupes',
        loadComponent: () => import('./demo/gestion-classes/groupes/groupes.component').then((c) => c.GroupesComponent)
      },
      {
        path: 'affectations',
        loadComponent: () => import('./demo/gestion-classes/affectations/affectations.component').then((c) => c.AffectationsComponent)
      },
      {
        path: 'planning',
        loadComponent: () => import('./demo/gestion-classes/planning/planning.component').then((c) => c.PlanningComponent)
      },
      {
        path: 'ai-report',
        loadComponent: () => import('./demo/gestion-classes/ai-report/ai-report.component').then((c) => c.AiReportComponent)
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