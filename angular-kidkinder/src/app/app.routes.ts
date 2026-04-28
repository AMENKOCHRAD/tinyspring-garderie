import { Routes } from '@angular/router';
import { WorkspaceShellComponent } from './components/workspace-shell.component';
import { AnimatorWorkspacePageComponent } from './pages/animator-workspace-page.component';
import { HomePageComponent } from './pages/home-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ParentWorkspacePageComponent } from './pages/parent-workspace-page.component';
import { authGuard as AuthGuard } from './shared/auth.guard';
import { roleGuard as RoleGuard } from './shared/role.guard';
import { MesAbsencesComponent } from './RH/pages/mes-absences/mes-absences.component';
import { NouvelleDemande } from './RH/pages/nouvelle-demande/nouvelle-demande.component';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'connexion', component: LoginPageComponent },

  // ✅ AJOUT — Page changement mot de passe (hors WorkspaceShell, pas de sidebar)
  {
    path: 'animateur/changer-mot-de-passe',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./RH/pages/changer-mot-de-passe/changer-mot-de-passe.component').then(
        c => c.ChangerMotDePasseComponent
      )
  },

  {
    path: 'parent',
    component: WorkspaceShellComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'PARENT', roles: ['PARENT'] },
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', component: ParentWorkspacePageComponent, data: { page: 'tableau-de-bord' } },
      { path: 'enfants',         component: ParentWorkspacePageComponent, data: { page: 'enfants' } },
      { path: 'sante',           component: ParentWorkspacePageComponent, data: { page: 'sante' } },
      { path: 'activites',       component: ParentWorkspacePageComponent, data: { page: 'activites' } },
      { path: 'menus',           component: ParentWorkspacePageComponent, data: { page: 'menus' } },
      { path: 'messages',        component: ParentWorkspacePageComponent, data: { page: 'messages' } },
      { path: 'trajets',         component: ParentWorkspacePageComponent, data: { page: 'trajets' } },
      { path: 'boutique',        component: ParentWorkspacePageComponent, data: { page: 'boutique' } },
    ]
  },
  {
    path: 'animateur',
    component: WorkspaceShellComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ANIMATRICE', roles: ['ANIMATRICE'] },
    children: [
{ path: '', redirectTo: '/animateur/rh/mes-absences', pathMatch: 'full' },      { path: 'tableau-de-bord', component: AnimatorWorkspacePageComponent, data: { page: 'tableau-de-bord' } },
      { path: 'groupes',         component: AnimatorWorkspacePageComponent, data: { page: 'groupes' } },
      { path: 'sante',           component: AnimatorWorkspacePageComponent, data: { page: 'sante' } },
      { path: 'activites',       component: AnimatorWorkspacePageComponent, data: { page: 'activites' } },
      { path: 'messages',        component: AnimatorWorkspacePageComponent, data: { page: 'messages' } },
      { path: 'formations',      component: AnimatorWorkspacePageComponent, data: { page: 'formations' } },
      { path: 'planning',        component: AnimatorWorkspacePageComponent, data: { page: 'planning' } },
      {
        path: 'mon-profil',
        loadComponent: () =>
          import('./RH/pages/mon-profil/mon-profil.component').then(c => c.MonProfilComponent)
      },
      { path: 'rh/mes-absences',     component: MesAbsencesComponent },
      { path: 'rh/nouvelle-demande', component: NouvelleDemande },
      {
        path: 'formations/disponibles',
        loadComponent: () =>
          import('./RH/pages/formations-disponibles/formations-disponibles.component').then(
            c => c.FormationsDisponiblesComponent
          )
      },
      {
        path: 'formations/mes-formations',
        loadComponent: () =>
          import('./RH/pages/mes-formations/mes-formations.component').then(
            c => c.MesFormationsComponent
          )
      }
    ]
  },
  { path: '**', redirectTo: '' }
];