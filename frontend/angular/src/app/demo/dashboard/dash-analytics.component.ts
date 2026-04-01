// angular import
import { Component } from '@angular/core';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';

@Component({
  selector: 'app-dash-analytics',
  imports: [SharedModule],
  templateUrl: './dash-analytics.component.html',
  styleUrls: ['./dash-analytics.component.scss']
})
export class DashAnalyticsComponent {
  cards = [
    { background: 'bg-gradient-blue', icon: 'icon-users', title: 'Gestion des utilisateurs', route: 'javascript:' },
    { background: 'bg-gradient-green', icon: 'icon-user', title: 'Gestion des enfants', route: 'javascript:' },
    { background: 'bg-gradient-purple', icon: 'icon-box', title: 'Classes & Groupes', route: 'javascript:' },
    { background: 'bg-gradient-orange', icon: 'icon-calendar', title: 'Événements & Menus', route: 'javascript:' },
    { background: 'bg-gradient-teal', icon: 'icon-message-circle', title: 'Messagerie interne', route: 'javascript:' },
    { background: 'bg-gradient-pink', icon: 'icon-book', title: 'RH & Formations', route: 'javascript:' },
    { background: 'bg-gradient-yellow', icon: 'icon-shopping-cart', title: 'Boutique en ligne', route: 'javascript:' },
    { background: 'bg-gradient-dark', icon: 'icon-navigation', title: 'Gestion de transport', route: 'javascript:' }
  ];
}
