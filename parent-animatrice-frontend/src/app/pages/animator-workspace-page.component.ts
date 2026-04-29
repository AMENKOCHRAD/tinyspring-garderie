import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../shared/auth.service';

type AnimatorPageKey =
  | 'tableau-de-bord'
  | 'groupes'
  | 'sante'
  | 'activites'
  | 'messages'
  | 'formations'
  | 'planning';

interface PageMeta {
  chip: string;
  title: string;
  description: string;
}

const pageMetaMap: Record<AnimatorPageKey, PageMeta> = {
  'tableau-de-bord': {
    chip: 'Espace animateur',
    title: 'Bonjour, {{name}}',
    description: 'Une vue rapide de la journee pour suivre les groupes, la sante et les messages.'
  },
  groupes: {
    chip: 'Mes groupes',
    title: 'Classes et enfants du jour',
    description: 'Retrouvez les groupes, les capacites et la liste des enfants suivis.'
  },
  sante: {
    chip: 'Sante et alertes',
    title: 'Soins, allergies et incidents',
    description: 'Une section claire pour agir rapidement sur les points de sante importants.'
  },
  activites: {
    chip: 'Activites',
    title: 'Programme et menus',
    description: 'Le planning pedagogique et les reperes repas restent visibles ensemble.'
  },
  messages: {
    chip: 'Messages',
    title: 'Echanges avec les familles',
    description: 'Conservez une communication fluide avec les parents.'
  },
  formations: {
    chip: 'Formations',
    title: 'Progression et certifications',
    description: 'Retrouvez vos formations, le catalogue disponible et vos certificats.'
  },
  planning: {
    chip: 'Planning',
    title: 'Calendrier et absences',
    description: 'Consultez votre planning mensuel et preparez vos demandes de conge.'
  }
};

@Component({
  selector: 'app-animator-workspace-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './animator-workspace-page.component.html',
  styleUrl: './animator-workspace-page.component.css'
})
export class AnimatorWorkspacePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  protected readonly page = signal<AnimatorPageKey>('tableau-de-bord');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());
  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
    const firstName = this.authService.getCurrentUser()?.nom.split(' ')[0] ?? 'Animatrice';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as AnimatorPageKey);
    });
  }
}
