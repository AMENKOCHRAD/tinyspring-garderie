import { Routes } from '@angular/router';
import { AboutPageComponent } from './pages/about-page.component';
import { AnimatorPortalPageComponent } from './pages/animator-portal-page.component';
import { ClassesPageComponent } from './pages/classes-page.component';
import { ContactPageComponent } from './pages/contact-page.component';
import { HomePageComponent } from './pages/home-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ParentPortalPageComponent } from './pages/parent-portal-page.component';
import { TeamPageComponent } from './pages/team-page.component';
import { AuthGuard } from './shared/auth.guard';
import { RoleGuard } from './shared/role.guard';

export const routes: Routes = [
  // Public routes
  { path: '', component: HomePageComponent, pathMatch: 'full' },
  { path: 'about', component: AboutPageComponent },
  { path: 'classes', component: ClassesPageComponent },
  { path: 'team', component: TeamPageComponent },
  { path: 'contact', component: ContactPageComponent },
  { path: 'login', component: LoginPageComponent },

  // Parent portal (protected - PARENT role only)
  {
    path: 'parent/portal',
    component: ParentPortalPageComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['PARENT'] }
  },

  // Animator portal (protected - ANIMATRICE role only)
  {
    path: 'animateur/portal',
    component: AnimatorPortalPageComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ANIMATRICE'] }
  },

  // Admin is NOT part of this frontoffice
  // Admins have their own separate backoffice application
  // Attempting to access this frontoffice with ADMIN role will be rejected at login

  // Wildcard
  { path: '**', redirectTo: '' }
];
