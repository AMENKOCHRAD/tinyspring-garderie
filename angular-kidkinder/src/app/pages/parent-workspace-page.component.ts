import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../shared/auth.service';

type ParentPageKey =
  | 'tableau-de-bord'
  | 'enfants'
  | 'sante'
  | 'activites'
  | 'menus'
  | 'messages'
  | 'trajets'
  | 'boutique';

interface PageMeta {
  chip: string;
  title: string;
  description: string;
}

const pageMetaMap: Record<ParentPageKey, PageMeta> = {
  'tableau-de-bord': {
    chip: 'Espace parent',
    title: 'Bonjour, {{name}}',
    description: 'Un resume rapide de la semaine pour suivre les enfants, les messages et les alertes.'
  },
  enfants: {
    chip: 'Enfants',
    title: 'Profils enfants et documents',
    description: 'Retrouvez les profils, les documents et les informations principales de vos enfants.'
  },
  sante: {
    chip: 'Sante',
    title: 'Suivi sante et incidents',
    description: 'Centralisez allergies, traitements, incidents et informations a ajouter.'
  },
  activites: {
    chip: 'Activites',
    title: 'Calendrier et evenements',
    description: 'Consultez les activites a venir, les sorties et les autorisations.'
  },
  menus: {
    chip: 'Menus',
    title: 'Menus hebdomadaires',
    description: 'Suivez les repas et les reperes mensuels dans un espace dedie.'
  },
  messages: {
    chip: 'Messages',
    title: 'Conversations avec la garderie',
    description: 'Retrouvez les echanges importants avec l equipe dans une interface simple.'
  },
  trajets: {
    chip: 'Trajets',
    title: 'Organisation des trajets',
    description: 'Consultez vos demandes de transport et envoyez un nouveau besoin.'
  },
  boutique: {
    chip: 'Boutique',
    title: 'Catalogue et commandes',
    description: 'Parcourez les articles utiles et suivez votre panier puis vos commandes.'
  }
};

@Component({
  selector: 'app-parent-workspace-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './parent-workspace-page.component.html',
  styleUrl: './parent-workspace-page.component.css'
})
export class ParentWorkspacePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  protected readonly page = signal<ParentPageKey>('tableau-de-bord');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
    const firstName = this.authService.getCurrentUser()?.nom.split(' ')[0] ?? 'Parent';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as ParentPageKey);
    });
  }
}
