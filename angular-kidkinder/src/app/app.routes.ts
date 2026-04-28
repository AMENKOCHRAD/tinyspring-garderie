import { Routes } from '@angular/router';
import { WorkspaceShellComponent } from './components/workspace-shell.component';
import { AnimatorWorkspacePageComponent } from './pages/animator-workspace-page.component';
import { HomePageComponent } from './pages/home-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ParentBoutiqueCartPageComponent } from './pages/parent-boutique-cart-page.component';
import { ParentBoutiqueCommandeAnnuleePageComponent } from './pages/parent-boutique-commande-annulee-page.component';
import { ParentBoutiqueEspecesConfirmePageComponent } from './pages/parent-boutique-especes-confirme-page.component';
import { ParentBoutiqueLienExpirePageComponent } from './pages/parent-boutique-lien-expire-page.component';
import { ParentBoutiqueOrderDetailPageComponent } from './pages/parent-boutique-order-detail-page.component';
import { ParentBoutiqueOrdersPageComponent } from './pages/parent-boutique-orders-page.component';
import { ParentBoutiquePaymentCancelPageComponent } from './pages/parent-boutique-payment-cancel-page.component';
import { ParentBoutiqueProduitDetailPageComponent } from './pages/parent-boutique-produit-detail-page.component';
import { ParentBoutiquePaymentSuccessPageComponent } from './pages/parent-boutique-payment-success-page.component';
import { ParentBoutiquePageComponent } from './pages/parent-boutique-page.component';
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
      { path: 'activites', component: ParentWorkspacePageComponent, data: { page: 'activites' } },
      { path: 'menus', component: ParentWorkspacePageComponent, data: { page: 'menus' } },
      { path: 'messages', component: ParentWorkspacePageComponent, data: { page: 'messages' } },
      { path: 'trajets', component: ParentWorkspacePageComponent, data: { page: 'trajets' } },
      { path: 'boutique', component: ParentBoutiquePageComponent },
      { path: 'boutique/produits/:id', component: ParentBoutiqueProduitDetailPageComponent },
      { path: 'boutique/cart', component: ParentBoutiqueCartPageComponent },
      { path: 'boutique/orders', component: ParentBoutiqueOrdersPageComponent },
      { path: 'boutique/orders/:id', component: ParentBoutiqueOrderDetailPageComponent },
      { path: 'boutique/paiement/success', component: ParentBoutiquePaymentSuccessPageComponent },
      { path: 'boutique/paiement/cancel', component: ParentBoutiquePaymentCancelPageComponent },
      { path: 'boutique/paiement/especes-confirme', component: ParentBoutiqueEspecesConfirmePageComponent },
      { path: 'boutique/paiement/commande-annulee', component: ParentBoutiqueCommandeAnnuleePageComponent },
      { path: 'boutique/paiement/lien-expire', component: ParentBoutiqueLienExpirePageComponent },
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
