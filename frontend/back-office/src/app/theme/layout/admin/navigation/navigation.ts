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
        title: 'Deconnexion',
        type: 'item',
        url: '/login',
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
        title: 'Classes & Salles',
        type: 'collapse',
        icon: 'feather icon-box',
        children: [
          {
            id: 'salles-liste',
            title: 'Salles',
            type: 'item',
            url: '/salles',
            classes: 'nav-item'
          },
          {
            id: 'classes-liste',
            title: 'Classes',
            type: 'item',
            url: '/classes',
            classes: 'nav-item'
          },
          {
            id: 'groupes-liste',
            title: 'Groupes',
            type: 'item',
            url: '/groupes',
            classes: 'nav-item'
          },
          {
            id: 'affectations-liste',
            title: 'Affectations',
            type: 'item',
            url: '/affectations',
            classes: 'nav-item'
          },
          {
            id: 'planning-liste',
            title: 'Planning Visuel',
            type: 'item',
            url: '/planning',
            classes: 'nav-item'
          },
          {
            id: 'ai-report',
            title: 'Générateur Rapports (IA)',
            type: 'item',
            url: '/ai-report',
            classes: 'nav-item'
          }
        ]
      },
      {
        id: 'evenements',
        title: 'Evenements & Menus',
        type: 'item',
        url: '/events',
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
        type: 'collapse',
        classes: 'nav-item',
        icon: 'feather icon-shopping-cart',
        children: [
          {
            id: 'boutique-dashboard',
            title: 'Dashboard',
            type: 'item',
            url: '/admin/boutique/dashboard',
            icon: 'feather icon-bar-chart-2'
          },
          {
            id: 'boutique-categories',
            title: 'Catégories',
            type: 'item',
            url: '/admin/boutique/categories',
            icon: 'feather icon-tag'
          },
          {
            id: 'boutique-produits',
            title: 'Produits',
            type: 'item',
            url: '/admin/boutique/produits',
            icon: 'feather icon-package'
          },
          {
            id: 'boutique-commandes',
            title: 'Commandes',
            type: 'item',
            url: '/admin/boutique/commandes',
            icon: 'feather icon-clipboard'
          },
          {
            id: 'boutique-predictions',
            title: 'Predictions ML',
            type: 'item',
            url: '/admin/boutique/predictions',
            icon: 'feather icon-activity'
          },
          {
            id: 'boutique-affinites',
            title: 'Affinites parents',
            type: 'item',
            url: '/admin/boutique/affinites',
            icon: 'feather icon-users'
          },
          {
            id: 'marketing-assistant',
            title: 'Assistant Marketing',
            type: 'item',
            url: '/admin/marketing',
            icon: 'feather icon-cpu'
          }
        ]
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
