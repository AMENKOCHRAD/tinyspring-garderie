import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    children: [
      { path: '', redirectTo: '/analytics', pathMatch: 'full' },
      {
        path: 'analytics',
        loadComponent: () =>
          import('./demo/dashboard/dash-analytics.component').then(c => c.DashAnalyticsComponent)
      },
      {
        path: 'component',
        loadChildren: () =>
          import('./demo/ui-element/ui-basic.module').then(m => m.UiBasicModule)
      },
      {
        path: 'chart',
        loadComponent: () =>
          import('./demo/chart-maps/core-apex.component').then(c => c.CoreApexComponent)
      },
      {
        path: 'forms',
        loadComponent: () =>
          import('./demo/forms/form-elements/form-elements.component').then(c => c.FormElementsComponent)
      },
      {
        path: 'tables',
        loadComponent: () =>
          import('./demo/tables/tbl-bootstrap/tbl-bootstrap.component').then(c => c.TblBootstrapComponent)
      },
      {
        path: 'sample-page',
        loadComponent: () =>
          import('./demo/other/sample-page/sample-page.component').then(c => c.SamplePageComponent)
      },

      // ===== RH =====
      {
        path: 'rh/dashboard',
        loadComponent: () =>
          import('./RH/dashboard/rh-dashboard.component').then(c => c.RhDashboardComponent)
      },
      {
        path: 'rh/calendrier',
        loadComponent: () =>
          import('./RH/calendrier/rh-calendrier.component').then(c => c.RhCalendrierComponent)
      },
      {
        path: 'rh/animatrices',
        loadComponent: () =>
          import('./RH/animatrice/list-animatrice/list-animatrice.component').then(c => c.ListAnimatriceComponent)
      },
      {
        path: 'rh/animatrices/new',
        loadComponent: () =>
          import('./RH/animatrice/form-animatrice/form-animatrice.component').then(c => c.FormAnimatriceComponent)
      },
      {
        path: 'rh/animatrices/edit/:id',
        loadComponent: () =>
          import('./RH/animatrice/form-animatrice/form-animatrice.component').then(c => c.FormAnimatriceComponent)
      },
      {
        path: 'rh/absences-conges',
        loadComponent: () =>
          import('./RH/absence-conge/list-absence-conge/list-absence-conge.component').then(c => c.ListAbsenceCongeComponent)
      },
      {
        path: 'rh/absences-conges/:id',
        loadComponent: () =>
          import('./RH/absence-conge/detail-absence-conge/detail-absence-conge.component').then(c => c.DetailAbsenceCongeComponent)
      },

      {
        path: 'rh/rapports',
        loadComponent: () =>
          import('./RH/rapports/rapport-rh.component').then(c => c.RapportRHComponent)
      },

      // ===== FORMATIONS =====
      {
        path: 'rh/formations',
        loadComponent: () =>
          import('./RH/formation/list-formation/list-formation.component').then(
            c => c.ListFormationComponent
          )
      },
      // ✅ Routes spécifiques AVANT /:id
      {
        path: 'rh/formations/suivi',
        loadComponent: () =>
          import('./RH/formation/suivi-animatrices/suivi-animatrices.component').then(
            c => c.SuiviAnimatricesComponent
          )
      },
      // ✅ /:id en dernier
      {
        path: 'rh/formations/:id',
        loadComponent: () =>
          import('./RH/formation/detail-formation/detail-formation.component').then(
            c => c.DetailFormationComponent
          )
      },
      {
  path: 'rh/absences-conges/:id',
  loadComponent: () =>
    import('./RH/absence-conge/detail-absence-conge/detail-absence-conge.component').then(
      c => c.DetailAbsenceCongeComponent
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
          import('./demo/pages/authentication/sign-up/sign-up.component').then(c => c.SignUpComponent)
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./demo/pages/authentication/sign-in/sign-in.component').then(c => c.SignInComponent)
      },
      {
        path: 'sign-in',
        loadComponent: () =>
          import('./demo/pages/authentication/sign-in/sign-in.component').then(c => c.SignInComponent)
      }
    ]
  },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}