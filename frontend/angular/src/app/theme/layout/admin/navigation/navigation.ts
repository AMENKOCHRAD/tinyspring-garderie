export interface NavigationItem {
  id: string;
  title: string;
  type: 'item' | 'collapse' | 'group';
  translate?: string;
  icon?: string;
  hidden?: boolean;
  url?: string;
  classes?: string;
  exactMatch?: boolean;
  external?: boolean;
  target?: boolean;
  breadcrumbs?: boolean;
  badge?: {
    title?: string;
    type?: string;
  };
  children?: NavigationItem[];
}

export const NavigationItems: NavigationItem[] = [
  {
    id: 'navigation',
    title: 'Navigation',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'dashboard',
        title: 'Tableau de bord',
        type: 'item',
        url: '/analytics',
        icon: 'feather icon-home'
      },
      {
        id: 'sign-in',
        title: 'Connexion',
        type: 'item',
        url: '/sign-in',
        icon: 'feather icon-log-in'
      },
      {
        id: 'logout',
        title: 'Déconnexion',
        type: 'item',
        url: '/logout',
        icon: 'feather icon-log-out'
      }
    ]
  },
  {
    id: 'gestions',
    title: 'Gestions',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'utilisateurs',
        title: 'Gestion des utilisateurs',
        type: 'item',
        url: '/gestion-utilisateurs',
        classes: 'nav-item',
        icon: 'feather icon-users'
      },
      {
        id: 'enfants',
        title: 'Gestion des enfants',
        type: 'item',
        url: '/gestion-enfants',
        classes: 'nav-item',
        icon: 'feather icon-user'
      },
      {
        id: 'classes',
        title: 'Classes & Groupes',
        type: 'item',
        url: '/classes-groupes',
        classes: 'nav-item',
        icon: 'feather icon-box'
      },
      {
        id: 'evenements',
        title: 'Événements & Menus',
        type: 'item',
        url: '/evenements-menus',
        classes: 'nav-item',
        icon: 'feather icon-calendar'
      },
      {
        id: 'messagerie',
        title: 'Messagerie interne',
        type: 'item',
        url: '/messagerie',
        classes: 'nav-item',
        icon: 'feather icon-message-circle'
      },
      {
        id: 'rh',
        title: 'RH & Formations',
        type: 'collapse',
        classes: 'nav-item',
        icon: 'feather icon-book',
        children: [
          {
            id: 'rh-dashboard',
            title: 'Dashboard RH',
            type: 'item',
            url: '/rh/dashboard',
            classes: 'nav-item',
            icon: 'feather icon-pie-chart'
          },
          {
            id: 'rh-animatrices',
            title: 'Animatrices',
            type: 'item',
            url: '/rh/animatrices',
            classes: 'nav-item',
            icon: 'feather icon-user'
          },
          {
            id: 'rh-absences-conges',
            title: 'Absences & Congés',
            type: 'item',
            url: '/rh/absences-conges',
            classes: 'nav-item',
            icon: 'feather icon-calendar'
          },
          {
            id: 'rh-formations',
            title: 'Formations',
            type: 'item',
            url: '/rh/formations',
            classes: 'nav-item',
            icon: 'feather icon-award'
          },
          {
            id: 'rh-calendrier',
            title: 'Calendrier',
            type: 'item',
            url: '/rh/calendrier',
            classes: 'nav-item',
            icon: 'feather icon-calendar'
          },
          // ✅ NOUVEAU — Moteur de règles
          {
            id: 'rh-quotas',
            title: '⚙️ Moteur de règles',
            type: 'item',
            url: '/rh/quotas',
            classes: 'nav-item',
            icon: 'feather icon-settings'
          },
          {
  id: 'rh-rapports',
  title: '🤖 Rapports IA',
  type: 'item',
  url: '/rh/rapports',
  classes: 'nav-item',
  icon: 'feather icon-file-text'
}
        ]
      },
      {
        id: 'boutique',
        title: 'Boutique en ligne',
        type: 'item',
        url: '/boutique',
        classes: 'nav-item',
        icon: 'feather icon-shopping-cart'
      },
      {
        id: 'transport',
        title: 'Gestion de transport',
        type: 'item',
        url: '/transport',
        classes: 'nav-item',
        icon: 'feather icon-navigation'
      }
    ]
  }
];