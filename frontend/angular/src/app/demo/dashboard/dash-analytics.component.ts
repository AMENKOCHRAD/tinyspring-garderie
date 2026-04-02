// angular import
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';

@Component({
  selector: 'app-dash-analytics',
  imports: [SharedModule, RouterModule],
  templateUrl: './dash-analytics.component.html',
  styleUrls: ['./dash-analytics.component.scss']
})
export class DashAnalyticsComponent {
  cards = [
    { background: 'bg-gradient-blue', icon: 'icon-users', title: 'Gestion des utilisateurs', route: '/gestion-utilisateurs' },
    { background: 'bg-gradient-green', icon: 'icon-user', title: 'Gestion des enfants', route: '/gestion-enfants' },
    { background: 'bg-gradient-purple', icon: 'icon-box', title: 'Classes & Groupes', route: '/classes-groupes' },
    { background: 'bg-gradient-orange', icon: 'icon-calendar', title: 'Evenements & Menus', route: '/evenements-menus' },
    { background: 'bg-gradient-teal', icon: 'icon-message-circle', title: 'Messagerie interne', route: '/messagerie' },
    { background: 'bg-gradient-pink', icon: 'icon-book', title: 'RH & Formations', route: '/rh-formations' },
    { background: 'bg-gradient-yellow', icon: 'icon-shopping-cart', title: 'Boutique en ligne', route: '/boutique' },
    { background: 'bg-gradient-dark', icon: 'icon-navigation', title: 'Gestion de transport', route: '/transport' }
  ];
}
