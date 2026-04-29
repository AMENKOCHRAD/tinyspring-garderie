import { Routes } from '@angular/router';
import { WorkspaceShellComponent } from './components/workspace-shell.component';
import { AnimatorWorkspacePageComponent } from './pages/animator-workspace-page.component';
import { HomePageComponent } from './pages/home-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ParentWorkspacePageComponent } from './pages/parent-workspace-page.component';
import { authGuard as AuthGuard } from './shared/auth.guard';
import { roleGuard as RoleGuard } from './shared/role.guard';

export const routes: Routes = [
  { path: '', component: HomePageComponent },
  { path: 'connexion', component: LoginPageComponent },
  {
    path: 'parent',
    component: WorkspaceShellComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'PARENT', roles: ['PARENT'] },
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', component: ParentWorkspacePageComponent, data: { page: 'tableau-de-bord' } },
      { path: 'enfants', component: ParentWorkspacePageComponent, data: { page: 'enfants' } },
      { path: 'sante', component: ParentWorkspacePageComponent, data: { page: 'sante' } },
<<<<<<< HEAD
      { path: 'changements', component: ParentWorkspacePageComponent, data: { page: 'changements' } },
=======
>>>>>>> origin/gestion-evenements
      { path: 'activites', component: ParentWorkspacePageComponent, data: { page: 'activites' } },
      { path: 'menus', component: ParentWorkspacePageComponent, data: { page: 'menus' } },
      { path: 'messages', component: ParentWorkspacePageComponent, data: { page: 'messages' } },
      { path: 'trajets', component: ParentWorkspacePageComponent, data: { page: 'trajets' } },
      { path: 'boutique', component: ParentWorkspacePageComponent, data: { page: 'boutique' } },
      { path: 'enfants-sante', redirectTo: 'enfants', pathMatch: 'full' },
      { path: 'activites-menus', redirectTo: 'activites', pathMatch: 'full' },
      { path: 'paiements', redirectTo: 'tableau-de-bord', pathMatch: 'full' }
    ]
  },
  {
    path: 'animateur',
    component: WorkspaceShellComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { role: 'ANIMATRICE', roles: ['ANIMATRICE'] },
    children: [
      { path: '', redirectTo: 'tableau-de-bord', pathMatch: 'full' },
      { path: 'tableau-de-bord', component: AnimatorWorkspacePageComponent, data: { page: 'tableau-de-bord' } },
      { path: 'groupes', component: AnimatorWorkspacePageComponent, data: { page: 'groupes' } },
      { path: 'sante', component: AnimatorWorkspacePageComponent, data: { page: 'sante' } },
      { path: 'activites', component: AnimatorWorkspacePageComponent, data: { page: 'activites' } },
      { path: 'messages', component: AnimatorWorkspacePageComponent, data: { page: 'messages' } },
      { path: 'formations', component: AnimatorWorkspacePageComponent, data: { page: 'formations' } },
      { path: 'planning', component: AnimatorWorkspacePageComponent, data: { page: 'planning' } },
      { path: 'formations-planning', redirectTo: 'formations', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
