import { UserRole } from './auth.models';

export type WorkspaceRole = Exclude<UserRole, 'ADMIN'>;

export interface PublicNavItem {
  label: string;
  fragment: string;
}

export interface WorkspaceNavItem {
  label: string;
  route: string;
}

export interface WorkspaceNavGroup {
  key: string;
  label: string;
  route: string;
  children?: WorkspaceNavItem[];
}

export interface LandingFeature {
  icon: string;
  title: string;
  description: string;
  tone: 'mint' | 'blush' | 'lime';
}

export interface LandingService {
  icon: string;
  title: string;
  description: string;
  tone: 'mint' | 'blush' | 'lime';
}

export interface LandingStep {
  number: string;
  title: string;
  description: string;
  icon: string;
}

export const publicNavItems: PublicNavItem[] = [
  { label: 'Accueil', fragment: 'hero' },
  { label: 'A propos', fragment: 'a-propos' },
  { label: 'Services', fragment: 'services' },
  { label: 'Contact', fragment: 'contact' }
];

export const landingMissionCards: LandingFeature[] = [
  {
    icon: 'S',
    title: 'Securite',
    description: 'Protection et tracabilite des donnees de chaque enfant.',
    tone: 'mint'
  },
  {
    icon: 'C',
    title: 'Communication',
    description: 'Echanges directs et securises entre les familles et l equipe.',
    tone: 'blush'
  },
  {
    icon: 'P',
    title: 'Pedagogie',
    description: 'Suivi personnalise du parcours educatif de chaque enfant.',
    tone: 'lime'
  }
];

export const landingServices: LandingService[] = [
  { icon: '1', title: 'Dossiers enfants', description: 'Profils, urgences, documents et liens avec les parents.', tone: 'mint' },
  { icon: '2', title: 'Sante et soins', description: 'Allergies, traitements et alertes claires pour chaque enfant.', tone: 'blush' },
  { icon: '3', title: 'Activites et menus', description: 'Planning pedagogique, evenements et repas centralises.', tone: 'lime' },
  { icon: '4', title: 'Messagerie interne', description: 'Conversations fluides entre parents, animatrices et equipe.', tone: 'mint' },
  { icon: '5', title: 'Trajets', description: 'Demandes de transport, horaires et validations simplifiees.', tone: 'blush' },
  { icon: '6', title: 'Boutique', description: 'Catalogue, panier et commandes utiles au quotidien.', tone: 'lime' }
];

export const landingSteps: LandingStep[] = [
  {
    number: '1',
    title: 'Recevez vos acces',
    description: 'La garderie cree les comptes parent et animatrice.',
    icon: 'A'
  },
  {
    number: '2',
    title: 'Connectez-vous',
    description: 'Choisissez votre role et utilisez vos identifiants securises.',
    icon: 'B'
  },
  {
    number: '3',
    title: 'Gerez sereinement',
    description: 'Retrouvez toutes les informations importantes dans un seul espace.',
    icon: 'C'
  }
];

export const workspaceNavByRole: Record<WorkspaceRole, WorkspaceNavGroup[]> = {
  PARENT: [
    { key: 'reclamations', label: 'Reclamations', route: '/parent/reclamations' },
    { key: 'dashboard', label: 'Tableau de bord', route: '/parent/tableau-de-bord' },
    {
      key: 'enfants',
      label: 'Enfants',
      route: '/parent/enfants',
      children: [
        { label: 'Enfants', route: '/parent/enfants' },
        { label: 'Sante', route: '/parent/sante' }
      ]
    },
    {
      key: 'activites',
      label: 'Activites',
      route: '/parent/activites',
      children: [
        { label: 'Activites', route: '/parent/activites' },
        { label: 'Menus', route: '/parent/menus' }
      ]
    },
    { key: 'messages', label: 'Messages', route: '/parent/messages' },
    { key: 'trajets', label: 'Trajets', route: '/parent/trajets' },
    { key: 'boutique', label: 'Boutique', route: '/parent/boutique' }
  ],
  ANIMATRICE: [
    { key: 'reclamations', label: 'Reclamations', route: '/animatrice/reclamations' },
    { key: 'dashboard', label: 'Tableau de bord', route: '/animatrice/tableau-de-bord' },
    { key: 'groupes', label: 'Mes groupes', route: '/animatrice/groupes' },
    { key: 'sante', label: 'Sante & alertes', route: '/animatrice/sante' },
    { key: 'activites', label: 'Activites', route: '/animatrice/activites' },
    { key: 'messages', label: 'Messages', route: '/animatrice/messages' },
    {
      key: 'rh',
      label: 'RH',
      route: '/animatrice/formations',
      children: [
        { label: 'Formations', route: '/animatrice/formations' },
        { label: 'Planning', route: '/animatrice/planning' }
      ]
    }
  ]
};
