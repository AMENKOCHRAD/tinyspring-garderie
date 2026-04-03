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
        type: 'item',
        url: '/rh-formations',
        classes: 'nav-item',
        icon: 'feather icon-book'
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