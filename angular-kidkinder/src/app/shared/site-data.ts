export interface FeatureItem {
  icon: string;
  title: string;
  description: string;
}

export interface ClassItem {
  image: string;
  title: string;
  description: string;
  age: string;
  seats: string;
  time: string;
  fee: string;
}

export interface TeacherItem {
  image: string;
  name: string;
  role: string;
}

export interface TestimonialItem {
  image: string;
  name: string;
  role: string;
  text: string;
}

export interface BlogItem {
  image: string;
  title: string;
  text: string;
}

export interface QuickLinkItem {
  label: string;
  path: string;
}

export interface DashboardStat {
  label: string;
  value: string;
  colorClass: string;
  icon: string;
}

export interface ModuleCard {
  title: string;
  description: string;
  action: string;
  colorClass: string;
  route: string;
}

export interface TableColumn {
  key: string;
  label: string;
}

export interface TableRow {
  [key: string]: string;
}

export const navItems: QuickLinkItem[] = [
  { label: 'Accueil', path: '/' },
  { label: 'A propos', path: '/about' },
  { label: 'Classes', path: '/classes' },
  { label: 'Equipe', path: '/team' },
  { label: 'Contact', path: '/contact' }
];

export const parentNavItems: QuickLinkItem[] = [
  { label: 'Connexion', path: '/login' },
  { label: 'Portail parent', path: '/parent/portal' }
];

export const adminNavItems: QuickLinkItem[] = [
  { label: 'Connexion', path: '/login' },
  { label: 'Dashboard', path: '/dashboard' }
];

export const managementNavItems: QuickLinkItem[] = [
  { label: 'Vue globale', path: '/dashboard' },
  { label: 'Parents', path: '/dashboard/parents' },
  { label: 'Enfants', path: '/dashboard/children' },
  { label: 'Classes', path: '/dashboard/classes' },
  { label: 'Enseignants', path: '/dashboard/teachers' },
  { label: 'Inscriptions', path: '/dashboard/registrations' },
  { label: 'Paiements', path: '/dashboard/payments' },
  { label: 'Transport', path: '/dashboard/transport' }
];

export const features: FeatureItem[] = [
  { icon: 'flaticon-050-fence', title: 'Aire de jeux', description: 'Un espace securise pour apprendre en s amusant.' },
  { icon: 'flaticon-022-drum', title: 'Musique et danse', description: 'Des activites creatives pour developper la confiance.' },
  { icon: 'flaticon-030-crayons', title: 'Arts plastiques', description: 'Des ateliers manuels adaptes a chaque tranche d age.' },
  { icon: 'flaticon-017-toy-car', title: 'Transport securise', description: 'Une organisation rassurante pour les familles.' },
  { icon: 'flaticon-025-sandwich', title: 'Repas equilibres', description: 'Des menus simples et sains pour les enfants.' },
  { icon: 'flaticon-047-backpack', title: 'Sorties educatives', description: 'Des experiences concretes pour ouvrir la curiosite.' }
];

export const classes: ClassItem[] = [
  {
    image: '/assets/kidkinder/img/class-1.jpg',
    title: 'Atelier dessin',
    description: 'Initiation aux couleurs, aux formes et a la creativite.',
    age: '3 - 6 ans',
    seats: '40 places',
    time: '08:00 - 10:00',
    fee: '290 TND / mois'
  },
  {
    image: '/assets/kidkinder/img/class-2.jpg',
    title: 'Eveil langage',
    description: 'Jeux et activites pour enrichir le vocabulaire au quotidien.',
    age: '3 - 6 ans',
    seats: '40 places',
    time: '10:00 - 12:00',
    fee: '290 TND / mois'
  },
  {
    image: '/assets/kidkinder/img/class-3.jpg',
    title: 'Sciences ludiques',
    description: 'Petites experiences et observation du monde qui entoure l enfant.',
    age: '4 - 6 ans',
    seats: '30 places',
    time: '14:00 - 16:00',
    fee: '320 TND / mois'
  }
];

export const teachers: TeacherItem[] = [
  { image: '/assets/kidkinder/img/team-1.jpg', name: 'Julia Smith', role: 'Musique' },
  { image: '/assets/kidkinder/img/team-2.jpg', name: 'Jhon Doe', role: 'Langage' },
  { image: '/assets/kidkinder/img/team-3.jpg', name: 'Mollie Ross', role: 'Danse' },
  { image: '/assets/kidkinder/img/team-4.jpg', name: 'Donald John', role: 'Arts' }
];

export const testimonials: TestimonialItem[] = [
  {
    image: '/assets/kidkinder/img/testimonial-1.jpg',
    name: 'Sonia',
    role: 'Maman de Lina',
    text: 'Une equipe douce, disponible et tres engagee. Notre fille adore venir chaque matin.'
  },
  {
    image: '/assets/kidkinder/img/testimonial-2.jpg',
    name: 'Yassine',
    role: 'Papa de Sami',
    text: 'Le cadre est rassurant et les activites sont bien pensees pour les petits.'
  },
  {
    image: '/assets/kidkinder/img/testimonial-3.jpg',
    name: 'Meriem',
    role: 'Maman de Nour',
    text: 'On sent une vraie attention portee au rythme et a l autonomie des enfants.'
  }
];

export const blogPosts: BlogItem[] = [
  {
    image: '/assets/kidkinder/img/blog-1.jpg',
    title: 'Comment accompagner les premiers apprentissages',
    text: 'Des gestes simples pour encourager la curiosite et la confiance a la maison.'
  },
  {
    image: '/assets/kidkinder/img/blog-2.jpg',
    title: 'Le jeu comme moteur de progression',
    text: 'Pourquoi les activites ludiques sont essentielles au developpement global.'
  },
  {
    image: '/assets/kidkinder/img/blog-3.jpg',
    title: 'Bien preparer la rentree en maternelle',
    text: 'Quelques reperes concrets pour aider l enfant a vivre cette transition sereinement.'
  }
];

export const dashboardStats: DashboardStat[] = [
  { label: 'Parents inscrits', value: '128', colorClass: 'bg-primary', icon: 'fa-users' },
  { label: 'Enfants actifs', value: '186', colorClass: 'bg-secondary', icon: 'fa-child' },
  { label: 'Classes ouvertes', value: '9', colorClass: 'bg-success', icon: 'fa-school' },
  { label: 'Paiements du mois', value: '41', colorClass: 'bg-warning', icon: 'fa-wallet' }
];

export const parentModules: ModuleCard[] = [
  { title: 'Evenements', description: 'Consulter les activites, sorties et animations a venir.', action: 'Voir les evenements', colorClass: 'bg-primary', route: '/parent/portal' },
  { title: 'Menu', description: 'Suivre les repas de la semaine et les informations alimentaires.', action: 'Voir le menu', colorClass: 'bg-success', route: '/parent/portal' },
  { title: 'Transport', description: 'Faire une demande de transport selon trajet et horaire.', action: 'Demander un transport', colorClass: 'bg-info', route: '/parent/portal' },
  { title: 'Classes', description: 'Retrouver la classe, le programme et les horaires de votre enfant.', action: 'Voir les classes', colorClass: 'bg-secondary', route: '/parent/portal' },
  { title: 'Boutique en ligne', description: 'Commander des articles et fournitures proposes par la garderie.', action: 'Ouvrir la boutique', colorClass: 'bg-warning', route: '/parent/portal' },
  { title: 'Staff', description: 'Consulter l equipe encadrante et les informations utiles.', action: 'Voir le staff', colorClass: 'bg-dark', route: '/parent/portal' },
  { title: 'Messagerie', description: 'Echanger avec l administration et l equipe pedagogique.', action: 'Ouvrir la messagerie', colorClass: 'bg-danger', route: '/parent/portal' },
  { title: 'Reclamations', description: 'Soumettre une reclamation ou suivre son traitement.', action: 'Faire une reclamation', colorClass: 'bg-primary', route: '/parent/portal' }
];

export const animatorModules: ModuleCard[] = [
  { title: 'Evenements', description: 'Preparer les animations et le planning des activites.', action: 'Gerer les evenements', colorClass: 'bg-primary', route: '/animateur/portal' },
  { title: 'Classes', description: 'Consulter les groupes et l organisation quotidienne.', action: 'Voir les classes', colorClass: 'bg-secondary', route: '/animateur/portal' },
  { title: 'Staff', description: 'Coordonner avec le reste de l equipe.', action: 'Voir le staff', colorClass: 'bg-dark', route: '/animateur/portal' },
  { title: 'Messagerie', description: 'Recevoir et envoyer les informations utiles.', action: 'Ouvrir la messagerie', colorClass: 'bg-info', route: '/animateur/portal' }
];

export const parentOverview = [
  { label: 'Enfant principal', value: 'Lina Ben Salah' },
  { label: 'Classe', value: 'Atelier dessin' },
  { label: 'Enseignant', value: 'Julia Smith' },
  { label: 'Dernier paiement', value: '02/04/2026' }
];

export const parentChildrenColumns: TableColumn[] = [
  { key: 'name', label: 'Enfant' },
  { key: 'class', label: 'Classe' },
  { key: 'schedule', label: 'Horaire' },
  { key: 'status', label: 'Statut' }
];

export const parentChildrenRows: TableRow[] = [
  { name: 'Lina Ben Salah', class: 'Atelier dessin', schedule: 'Lun-Ven 08:00-10:00', status: 'Active' },
  { name: 'Adam Ben Salah', class: 'Eveil langage', schedule: 'Lun-Ven 10:00-12:00', status: 'En attente' }
];

export const parentPaymentsColumns: TableColumn[] = [
  { key: 'month', label: 'Mois' },
  { key: 'amount', label: 'Montant' },
  { key: 'method', label: 'Paiement' },
  { key: 'status', label: 'Statut' }
];

export const parentPaymentsRows: TableRow[] = [
  { month: 'Mars 2026', amount: '290 TND', method: 'Carte', status: 'Paye' },
  { month: 'Avril 2026', amount: '290 TND', method: 'En attente', status: 'A regler' }
];

export const parentTransportColumns: TableColumn[] = [
  { key: 'child', label: 'Enfant' },
  { key: 'route', label: 'Trajet' },
  { key: 'schedule', label: 'Horaire' },
  { key: 'status', label: 'Statut' }
];

export const parentTransportRows: TableRow[] = [
  { child: 'Lina Ben Salah', route: 'Lac 1 -> Garderie', schedule: '07:30', status: 'En attente' },
  { child: 'Adam Ben Salah', route: 'Garderie -> Lac 1', schedule: '16:30', status: 'Acceptee' }
];

export const parentsColumns: TableColumn[] = [
  { key: 'name', label: 'Parent' },
  { key: 'email', label: 'Email' },
  { key: 'phone', label: 'Telephone' },
  { key: 'children', label: 'Enfants' }
];

export const parentsRows: TableRow[] = [
  { name: 'Sonia Trabelsi', email: 'sonia.trabelsi@example.com', phone: '+216 20 111 111', children: '2' },
  { name: 'Yassine Gharbi', email: 'yassine.gharbi@example.com', phone: '+216 21 222 222', children: '1' },
  { name: 'Amira Ben Amor', email: 'amira.benamor@example.com', phone: '+216 22 333 333', children: '3' }
];

export const childrenColumns: TableColumn[] = [
  { key: 'name', label: 'Enfant' },
  { key: 'age', label: 'Age' },
  { key: 'class', label: 'Classe' },
  { key: 'parent', label: 'Parent' }
];

export const childrenRows: TableRow[] = [
  { name: 'Lina Ben Salah', age: '5', class: 'Atelier dessin', parent: 'Sonia Trabelsi' },
  { name: 'Sami Gharbi', age: '4', class: 'Eveil langage', parent: 'Yassine Gharbi' },
  { name: 'Nour Ben Amor', age: '6', class: 'Sciences ludiques', parent: 'Amira Ben Amor' }
];

export const classesColumns: TableColumn[] = [
  { key: 'title', label: 'Classe' },
  { key: 'teacher', label: 'Enseignant' },
  { key: 'capacity', label: 'Capacite' },
  { key: 'schedule', label: 'Horaire' }
];

export const classesRows: TableRow[] = [
  { title: 'Atelier dessin', teacher: 'Julia Smith', capacity: '40', schedule: '08:00 - 10:00' },
  { title: 'Eveil langage', teacher: 'Jhon Doe', capacity: '40', schedule: '10:00 - 12:00' },
  { title: 'Sciences ludiques', teacher: 'Mollie Ross', capacity: '30', schedule: '14:00 - 16:00' }
];

export const teachersColumns: TableColumn[] = [
  { key: 'name', label: 'Enseignant' },
  { key: 'role', label: 'Specialite' },
  { key: 'email', label: 'Email' },
  { key: 'status', label: 'Statut' }
];

export const teachersRows: TableRow[] = [
  { name: 'Julia Smith', role: 'Musique', email: 'julia.smith@example.com', status: 'Active' },
  { name: 'Jhon Doe', role: 'Langage', email: 'jhon.doe@example.com', status: 'Active' },
  { name: 'Mollie Ross', role: 'Danse', email: 'mollie.ross@example.com', status: 'Conge' }
];

export const registrationsColumns: TableColumn[] = [
  { key: 'child', label: 'Enfant' },
  { key: 'class', label: 'Classe' },
  { key: 'date', label: 'Date' },
  { key: 'status', label: 'Statut' }
];

export const registrationsRows: TableRow[] = [
  { child: 'Lina Ben Salah', class: 'Atelier dessin', date: '01/04/2026', status: 'Validee' },
  { child: 'Adam Ben Salah', class: 'Eveil langage', date: '03/04/2026', status: 'En attente' },
  { child: 'Nour Ben Amor', class: 'Sciences ludiques', date: '04/04/2026', status: 'Validee' }
];

export const paymentsColumns: TableColumn[] = [
  { key: 'parent', label: 'Parent' },
  { key: 'amount', label: 'Montant' },
  { key: 'date', label: 'Date' },
  { key: 'status', label: 'Statut' }
];

export const paymentsRows: TableRow[] = [
  { parent: 'Sonia Trabelsi', amount: '290 TND', date: '02/04/2026', status: 'Paye' },
  { parent: 'Yassine Gharbi', amount: '290 TND', date: '05/04/2026', status: 'En attente' },
  { parent: 'Amira Ben Amor', amount: '320 TND', date: '06/04/2026', status: 'Paye' }
];

export const transportColumns: TableColumn[] = [
  { key: 'child', label: 'Enfant' },
  { key: 'parent', label: 'Parent' },
  { key: 'route', label: 'Trajet' },
  { key: 'schedule', label: 'Horaire' },
  { key: 'status', label: 'Statut' }
];

export const transportRows: TableRow[] = [
  { child: 'Lina Ben Salah', parent: 'Sonia Trabelsi', route: 'Lac 1 -> Garderie', schedule: '07:30', status: 'En attente' },
  { child: 'Adam Ben Salah', parent: 'Sonia Trabelsi', route: 'Garderie -> Lac 1', schedule: '16:30', status: 'Acceptee' },
  { child: 'Nour Ben Amor', parent: 'Amira Ben Amor', route: 'Menzah 6 -> Garderie', schedule: '08:00', status: 'Refusee' }
];
