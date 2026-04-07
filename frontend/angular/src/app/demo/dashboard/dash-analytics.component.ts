import { Component } from '@angular/core';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dash-analytics',
  standalone: true,
  imports: [SharedModule, RouterModule],
  templateUrl: './dash-analytics.component.html',
  styleUrls: ['./dash-analytics.component.scss']
})
export class DashAnalyticsComponent {
  cards = [
    {
      background: 'bg-gradient-blue',
      icon: 'icon-users',
      title: 'Gestion des utilisateurs',
      route: null
    },
    {
      background: 'bg-gradient-green',
      icon: 'icon-user',
      title: 'Gestion des enfants',
      route: '/gestion-enfants'
    },
    {
      background: 'bg-gradient-purple',
      icon: 'icon-box',
      title: 'Classes & Groupes',
      route: null
    },
    {
      background: 'bg-gradient-orange',
      icon: 'icon-calendar',
      title: 'Événements & Menus',
      route: null
    },
    {
      background: 'bg-gradient-teal',
      icon: 'icon-message-circle',
      title: 'Messagerie interne',
      route: null
    },
    {
      background: 'bg-gradient-pink',
      icon: 'icon-book',
      title: 'RH & Formations',
      route: null
    },
    {
      background: 'bg-gradient-yellow',
      icon: 'icon-shopping-cart',
      title: 'Boutique en ligne',
      route: null
    },
    {
      background: 'bg-gradient-dark',
      icon: 'icon-navigation',
      title: 'Gestion de transport',
      route: null
    }
  ];
}