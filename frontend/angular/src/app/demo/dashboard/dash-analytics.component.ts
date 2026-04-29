import { Component } from '@angular/core';
<<<<<<< HEAD
<<<<<<< HEAD
=======
=======
>>>>>>> origin/gestion-evenements
import { RouterModule } from '@angular/router';

// project import
>>>>>>> origin/gestion-transports
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dash-analytics',
<<<<<<< HEAD
<<<<<<< HEAD
  standalone: true,
=======
>>>>>>> origin/gestion-transports
=======
>>>>>>> origin/gestion-evenements
  imports: [SharedModule, RouterModule],
  templateUrl: './dash-analytics.component.html',
  styleUrls: ['./dash-analytics.component.scss']
})
export class DashAnalyticsComponent {
  cards = [
<<<<<<< HEAD
<<<<<<< HEAD
    
  {
    background: 'bg-gradient-green',
    icon: 'icon-user',
    title: 'Gestion des enfants',
    route: '/gestion-enfants'
  }
,
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
=======
    { background: 'bg-gradient-blue', icon: 'icon-users', title: 'Gestion des utilisateurs', route: '/gestion-utilisateurs' },
    { background: 'bg-gradient-green', icon: 'icon-user', title: 'Gestion des enfants', route: '/gestion-enfants' },
    { background: 'bg-gradient-purple', icon: 'icon-box', title: 'Classes & Groupes', route: '/classes-groupes' },
    { background: 'bg-gradient-orange', icon: 'icon-calendar', title: 'Evenements & Menus', route: '/evenements-menus' },
    { background: 'bg-gradient-teal', icon: 'icon-message-circle', title: 'Messagerie interne', route: '/messagerie' },
    { background: 'bg-gradient-pink', icon: 'icon-book', title: 'RH & Formations', route: '/rh-formations' },
    { background: 'bg-gradient-yellow', icon: 'icon-shopping-cart', title: 'Boutique en ligne', route: '/boutique' },
    { background: 'bg-gradient-dark', icon: 'icon-navigation', title: 'Gestion de transport', route: '/transport' }
>>>>>>> origin/gestion-transports
=======
    { background: 'bg-gradient-blue', icon: 'icon-users', title: 'Gestion des utilisateurs', route: 'javascript:' },
    { background: 'bg-gradient-green', icon: 'icon-user', title: 'Gestion des enfants', route: 'javascript:' },
    { background: 'bg-gradient-purple', icon: 'icon-box', title: 'Classes & Groupes', route: 'javascript:' },
    { background: 'bg-gradient-orange', icon: 'icon-calendar', title: 'Événements & Menus', route: '/events' },
    { background: 'bg-gradient-teal', icon: 'icon-message-circle', title: 'Messagerie interne', route: 'javascript:' },
    { background: 'bg-gradient-pink', icon: 'icon-book', title: 'RH & Formations', route: 'javascript:' },
    { background: 'bg-gradient-yellow', icon: 'icon-shopping-cart', title: 'Boutique en ligne', route: 'javascript:' },
    { background: 'bg-gradient-dark', icon: 'icon-navigation', title: 'Gestion de transport', route: 'javascript:' }
>>>>>>> origin/gestion-evenements
  ];
}