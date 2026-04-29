import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
<<<<<<< HEAD
<<<<<<< HEAD
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../shared/auth.service';
import { Enfant, EnfantDTO, EnfantService } from '../services/enfant.service';
import { ParentChangementsComponent } from '../components/parent-changements.component';
import { TunisiaAddressPickerComponent } from '../components/tunisia-address-picker.component';
=======
import { ParentActivitiesComponent } from '../events/parent-activities.component';
import { ParentMenusComponent } from '../menus/parent-menus.component';
import { AuthService } from '../shared/auth.service';
>>>>>>> origin/gestion-evenements
=======
import { AuthService } from '../shared/auth.service';
>>>>>>> origin/gestion_boutique

type ParentPageKey =
  | 'tableau-de-bord'
  | 'enfants'
  | 'sante'
<<<<<<< HEAD
<<<<<<< HEAD
  | 'changements'
=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
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

<<<<<<< HEAD
<<<<<<< HEAD
interface ConditionSanitaireFront {
  id: number;
  nomCondition: string;
  type: 'MALADIE_CHRONIQUE' | 'MALADIE_TEMPORAIRE';
  description: string;
  dateDebut: string;
  dateFin?: string | null;
  enfant?: Enfant;
  traitements?: TraitementFront[];
}

interface TraitementFront {
  id: number;
  nomTraitement: string;
  description: string;
  ordonnance: string;
  dateDebut: string;
  dateFin?: string | null;
  heuresPrises: string[];
  statut: 'EN_ATTENTE_VALIDATION' | 'VALIDE' | 'REFUSE' | 'ACTIF' | 'ANNULE';
}

interface EnfantAvecSante {
  enfant: Enfant;
  conditions: ConditionSanitaireFront[];
}

interface ObservationFront {
  id: number;
  type: 'SANTE' | 'COMPORTEMENT' | string;
  titre: string;
  description: string;
  creeLe: string;
  creeParNom?: string | null;
  enfantId?: number;
  enfantNom?: string;
  enfantPrenom?: string;
  luParent?: boolean;
  luLe?: string | null;
  observeLe?: string | null;
  urgence?: string | null;
  temperature?: number | null;
  lieu?: string | null;
  symptomes?: string | null;
  actionsEffectuees?: string | null;
}

interface AllergieOption {
  label: string;
  value: string;
}

interface AllergieCategory {
  title: string;
  options: AllergieOption[];
}

=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
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
<<<<<<< HEAD
<<<<<<< HEAD
  changements: {
    chip: 'Changements',
    title: 'Changements & observations',
    description: 'Consultez les changements declares par l equipe, et suivez les informations importantes.'
  },
=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
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
<<<<<<< HEAD
<<<<<<< HEAD
  imports: [CommonModule, FormsModule, ParentChangementsComponent, TunisiaAddressPickerComponent],
=======
  imports: [CommonModule, ParentActivitiesComponent, ParentMenusComponent],
>>>>>>> origin/gestion-evenements
=======
  imports: [CommonModule],
>>>>>>> origin/gestion_boutique
  templateUrl: './parent-workspace-page.component.html',
  styleUrl: './parent-workspace-page.component.css'
})
export class ParentWorkspacePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
<<<<<<< HEAD
<<<<<<< HEAD
  private readonly enfantService = inject(EnfantService);

  protected readonly todayIso = this.getLocalTodayIso();
=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique

  protected readonly page = signal<ParentPageKey>('tableau-de-bord');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
<<<<<<< HEAD
<<<<<<< HEAD
    const firstName = this.authService.getCurrentUser()?.nom?.split(' ')[0] ?? 'Parent';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

  readonly groupesSanguins: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly traitementHeuresDisponibles: string[] = this.buildHeuresTraitementDisponibles();
  readonly allergiesCatalog: AllergieCategory[] = [
    {
      title: 'Allergies alimentaires',
      options: [
        { label: '🥜 Arachides', value: 'Arachides' },
        { label: '🥛 Lait', value: 'Lait' },
        { label: '🥚 Œufs', value: 'Œufs' },
        { label: '🐟 Poisson', value: 'Poisson' },
        { label: '🍤 Fruits de mer', value: 'Fruits de mer' },
        { label: '🌾 Gluten (blé)', value: 'Gluten (blé)' },
        { label: '🍓 Fruits', value: 'Fruits' }
      ]
    },
    {
      title: 'Allergies respiratoires',
      options: [
        { label: '🌸 Pollen', value: 'Pollen' },
        { label: '🐱 Poils d’animaux', value: 'Poils d’animaux' },
        { label: '🏠 Acariens (poussière)', value: 'Acariens (poussière)' }
      ]
    },
    {
      title: 'Allergies cutanées (peau)',
      options: [
        { label: '🧼 Produits cosmétiques', value: 'Produits cosmétiques' },
        { label: '🧴 Savons / shampoings', value: 'Savons / shampoings' },
        { label: '👕 Tissus (laine)', value: 'Tissus (laine)' },
        { label: '💍 Nickel (bijoux)', value: 'Nickel (bijoux)' },
        { label: '🌿 Plantes (ortie…)', value: 'Plantes (ortie…)' }
      ]
    },
    {
      title: 'Allergies aux insectes',
      options: [
        { label: '🐝 Abeilles', value: 'Abeilles' },
        { label: '🐜 Fourmis', value: 'Fourmis' },
        { label: '🦟 Moustiques', value: 'Moustiques' },
        { label: '🐝 Guêpes', value: 'Guêpes' }
      ]
    }
  ];

  enfants: Enfant[] = [];
  enfantsAvecSante: EnfantAvecSante[] = [];
  healthPage = 1;
healthPageSize = 2;

get enfantsAvecSantePagines(): EnfantAvecSante[] {
  const start = (this.healthPage - 1) * this.healthPageSize;
  const end = start + this.healthPageSize;
  return this.enfantsAvecSante.slice(start, end);
}

get totalHealthPages(): number {
  return Math.ceil(this.enfantsAvecSante.length / this.healthPageSize);
}

goToHealthPage(page: number): void {
  if (page < 1 || page > this.totalHealthPages) {
    return;
  }

  this.healthPage = page;
}

nextHealthPage(): void {
  this.goToHealthPage(this.healthPage + 1);
}

previousHealthPage(): void {
  this.goToHealthPage(this.healthPage - 1);
}

getHealthPages(): number[] {
  return Array.from({ length: this.totalHealthPages }, (_, i) => i + 1);
}
  observationsParEnfant: Record<number, ObservationFront[]> = {};
  selectedEnfant: Enfant | null = null;

  isSaving = false;
  isUpdating = false;
  isEditingEnfant = false;
  isLoadingObservations = false;

  errorMessage = '';
  successMessage = '';

  nouvelEnfant = {
    prenom: '',
    nom: '',
    dateNaissance: '',
    contactUrgence: '',
    allergies: '',
    groupeSanguin: '',
    notes: '',
    photoDataUrl: '',
    adresse: '',
    adresseLat: null as number | null,
    adresseLng: null as number | null
  };
  nouvelEnfantAllergiesSelection: string[] = [];
  nouvelEnfantAllergiesAutres = '';

  editEnfantForm = {
    id: 0,
    prenom: '',
    nom: '',
    dateNaissance: '',
    contactUrgence: '',
    allergies: '',
    groupeSanguin: '',
    photo: '',
    adresse: '',
    adresseLat: null as number | null,
    adresseLng: null as number | null
  };
  editEnfantAllergiesSelection: string[] = [];
  editEnfantAllergiesAutres = '';

  conditionsSanitaires: ConditionSanitaireFront[] = [];
  conditionSelectionnee: ConditionSanitaireFront | null = null;
  conditionDetail: ConditionSanitaireFront | null = null;

  isSavingCondition = false;
  isSavingTraitement = false;
  showTraitementForm = false;

  isEditingTraitement = false;
  isUpdatingTraitement = false;
  isDeletingTraitement = false;

  isEditingCondition = false;
  isUpdatingCondition = false;
  isDeletingCondition = false;

  nouvelleCondition = {
    enfantId: null as number | null,
    nomCondition: '',
    type: '' as '' | 'MALADIE_CHRONIQUE' | 'MALADIE_TEMPORAIRE',
    description: '',
    dateDebut: '',
    dateFin: ''
  };

  editConditionForm = {
    id: 0,
    enfantId: null as number | null,
    nomCondition: '',
    type: '' as '' | 'MALADIE_CHRONIQUE' | 'MALADIE_TEMPORAIRE',
    description: '',
    dateDebut: '',
    dateFin: ''
  };

  nouveauTraitement = {
    nomTraitement: '',
    description: '',
    ordonnancePdf: null as File | null,
    dateDebut: '',
    dateFin: '',
    heure1: '',
    heure2: '',
    heure3: ''
  };
  nouveauTraitementHeuresCount = 1;

  editTraitementForm = {
    id: 0,
    conditionId: 0,
    nomTraitement: '',
    description: '',
    ordonnance: '',
    dateDebut: '',
    dateFin: '',
    heure1: '',
    heure2: '',
    heure3: ''
  };
  editTraitementHeuresCount = 1;

  onOrdonnanceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.nouveauTraitement.ordonnancePdf = file;
  }

=======
=======
>>>>>>> origin/gestion_boutique
    const firstName = this.authService.getCurrentUser()?.nom.split(' ')[0] ?? 'Parent';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

<<<<<<< HEAD
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as ParentPageKey);
    });
<<<<<<< HEAD
<<<<<<< HEAD

    this.chargerEnfants();
  }

  private buildHeuresTraitementDisponibles(): string[] {
    // 07:30 -> 18:30 inclus, pas de 30 minutes
    const result: string[] = [];
    const startMinutes = 7 * 60 + 30;
    const endMinutes = 18 * 60 + 30;

    for (let m = startMinutes; m <= endMinutes; m += 30) {
      const hh = String(Math.floor(m / 60)).padStart(2, '0');
      const mm = String(m % 60).padStart(2, '0');
      result.push(`${hh}:${mm}`);
    }

    return result;
  }

  private parseAllergies(raw: string | null | undefined): { selected: string[]; autres: string } {
    const input = (raw || '').trim();
    if (!input) {
      return { selected: [], autres: '' };
    }

    const tokens = input
      .split(',')
      .map((t) => t.trim())
      .filter((t) => !!t);

    const known = new Set(this.allergiesCatalog.flatMap((c) => c.options.map((o) => o.value)));
    const selected = tokens.filter((t) => known.has(t));
    const autres = tokens.filter((t) => !known.has(t)).join(', ');

    return { selected, autres };
  }

  private buildAllergiesString(selected: string[], autres: string): string {
    const cleanSelected = Array.from(
      new Set((selected || []).map((s) => (s || '').trim()).filter(Boolean))
    );

    const autresParts = (autres || '')
      .split(/[,\\n]/g)
      .map((t) => t.trim())
      .filter((t) => !!t);

    const all = [...cleanSelected, ...autresParts];
    return all.join(', ');
  }

  chargerEnfants(): void {
    const currentUser = this.authService.getCurrentUser();

    if (!currentUser?.id) {
      this.errorMessage = 'Utilisateur non connecte ou id parent introuvable.';
      return;
    }

    const parentId = Number(currentUser.id);

    if (isNaN(parentId)) {
      this.errorMessage = 'ID parent invalide.';
      return;
    }

    this.enfantService.getEnfantsParParent(parentId).subscribe({
      next: (data) => {
        this.enfants = data ?? [];
        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les enfants.';
      }
    });
  }

  ajouterEnfant(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const currentUser = this.authService.getCurrentUser();

    if (!currentUser?.id) {
      this.errorMessage = 'Parent introuvable.';
      return;
    }

    const prenom = this.nouvelEnfant.prenom.trim();
    const nom = this.nouvelEnfant.nom.trim();
    const dateNaissance = this.nouvelEnfant.dateNaissance;
    const contactUrgence = this.nouvelEnfant.contactUrgence.trim();
    const allergies = this.buildAllergiesString(
      this.nouvelEnfantAllergiesSelection,
      this.nouvelEnfantAllergiesAutres
    );
    const groupeSanguin = this.nouvelEnfant.groupeSanguin.trim();

    if (!prenom) {
      this.errorMessage = 'Le prenom est obligatoire.';
      return;
    }

    if (!nom) {
      this.errorMessage = 'Le nom est obligatoire.';
      return;
    }

    if (!dateNaissance) {
      this.errorMessage = 'La date de naissance est obligatoire.';
      return;
    }

    if (!this.isAgeStrictlyGreaterThanTwo(dateNaissance)) {
      this.errorMessage = "L'enfant doit avoir plus de 2 ans.";
      return;
    }

    if (!contactUrgence) {
      this.errorMessage = 'Le numero de contact d urgence est obligatoire.';
      return;
    }

    if (!this.validatePhone(contactUrgence)) {
      this.errorMessage = 'Le numero de telephone doit contenir exactement 8 chiffres.';
      return;
    }

    if (!groupeSanguin) {
      this.errorMessage = 'Le groupe sanguin est obligatoire.';
      return;
    }

    if (!this.validateGroupeSanguin(groupeSanguin)) {
      this.errorMessage = 'Le groupe sanguin est invalide.';
      return;
    }

    const parentId = Number(currentUser.id);

    if (isNaN(parentId)) {
      this.errorMessage = 'ID utilisateur invalide.';
      return;
    }

    const dto: EnfantDTO = {
      prenom,
      nom,
      dateNaissance,
      contactUrgence,
      allergies,
      groupeSanguin,
      photo: this.nouvelEnfant.photoDataUrl || '',
      adresse: (this.nouvelEnfant.adresse || '').trim(),
      adresseLat: this.nouvelEnfant.adresseLat,
      adresseLng: this.nouvelEnfant.adresseLng,
      parentId
    };

    this.isSaving = true;

    this.enfantService.ajouterEnfant(dto).subscribe({
      next: (enfantAjoute) => {
        this.enfants = [enfantAjoute, ...this.enfants];
        this.successMessage = 'Enfant ajoute avec succes.';
        this.resetForm();
        this.isSaving = false;
        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        this.extractHttpErrorDetails(err, (details) => {
          this.errorMessage = `Erreur lors de l'ajout de l'enfant${status}. ${details}`.trim();
        });
        this.isSaving = false;
      }
    });
  }

  voirProfil(enfant: Enfant): void {
    if (!enfant?.id) {
      this.errorMessage = 'Enfant introuvable.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.isEditingEnfant = false;

    this.enfantService.getEnfantById(enfant.id).subscribe({
      next: (data) => {
        this.selectedEnfant = data;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger le profil complet.';
      }
    });
  }

  activerModification(): void {
    if (!this.selectedEnfant) {
      return;
    }

    this.isEditingEnfant = true;

    this.editEnfantForm = {
      id: this.selectedEnfant.id,
      prenom: this.selectedEnfant.prenom || '',
      nom: this.selectedEnfant.nom || '',
      dateNaissance: this.selectedEnfant.dateNaissance || '',
      contactUrgence: this.selectedEnfant.contactUrgence || '',
      allergies: this.selectedEnfant.allergies || '',
      groupeSanguin: this.selectedEnfant.groupeSanguin || '',
      photo: this.selectedEnfant.photo || '',
      adresse: this.selectedEnfant.adresse || '',
      adresseLat: this.selectedEnfant.adresseLat ?? null,
      adresseLng: this.selectedEnfant.adresseLng ?? null
    };

    const parsed = this.parseAllergies(this.editEnfantForm.allergies);
    this.editEnfantAllergiesSelection = parsed.selected;
    this.editEnfantAllergiesAutres = parsed.autres;
  }

  annulerModification(): void {
    this.isEditingEnfant = false;
    this.editEnfantAllergiesSelection = [];
    this.editEnfantAllergiesAutres = '';
  }

  fermerProfil(): void {
    this.selectedEnfant = null;
    this.isEditingEnfant = false;
  }

  enregistrerModificationEnfant(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const currentUser = this.authService.getCurrentUser();

    if (!currentUser?.id) {
      this.errorMessage = 'Parent introuvable.';
      return;
    }

    const prenom = this.editEnfantForm.prenom.trim();
    const nom = this.editEnfantForm.nom.trim();
    const dateNaissance = this.editEnfantForm.dateNaissance;
    const contactUrgence = this.editEnfantForm.contactUrgence.trim();
    const allergies = this.buildAllergiesString(
      this.editEnfantAllergiesSelection,
      this.editEnfantAllergiesAutres
    );
    const groupeSanguin = this.editEnfantForm.groupeSanguin.trim();
    const photo = this.editEnfantForm.photo.trim();

    if (!prenom) {
      this.errorMessage = 'Le prenom est obligatoire.';
      return;
    }

    if (!nom) {
      this.errorMessage = 'Le nom est obligatoire.';
      return;
    }

    if (!dateNaissance) {
      this.errorMessage = 'La date de naissance est obligatoire.';
      return;
    }

    if (!this.isAgeStrictlyGreaterThanTwo(dateNaissance)) {
      this.errorMessage = "L'enfant doit avoir plus de 2 ans.";
      return;
    }

    if (!contactUrgence) {
      this.errorMessage = 'Le numero de contact d urgence est obligatoire.';
      return;
    }

    if (!this.validatePhone(contactUrgence)) {
      this.errorMessage = 'Le numero de telephone doit contenir exactement 8 chiffres.';
      return;
    }

    if (!groupeSanguin) {
      this.errorMessage = 'Le groupe sanguin est obligatoire.';
      return;
    }

    if (!this.validateGroupeSanguin(groupeSanguin)) {
      this.errorMessage = 'Le groupe sanguin est invalide.';
      return;
    }

    const parentId = Number(currentUser.id);

    if (isNaN(parentId)) {
      this.errorMessage = 'ID parent invalide.';
      return;
    }

    const dto: EnfantDTO = {
      prenom,
      nom,
      dateNaissance,
      contactUrgence,
      allergies,
      groupeSanguin,
      photo,
      adresse: (this.editEnfantForm.adresse || '').trim(),
      adresseLat: this.editEnfantForm.adresseLat,
      adresseLng: this.editEnfantForm.adresseLng,
      parentId
    };

    this.isUpdating = true;

    this.enfantService.modifierEnfant(this.editEnfantForm.id, dto).subscribe({
      next: () => {
        this.successMessage = 'Informations modifiees avec succes.';
        this.isEditingEnfant = false;
        this.chargerEnfants();

        this.enfantService.getEnfantById(this.editEnfantForm.id).subscribe({
          next: (data) => {
            this.selectedEnfant = data;
            this.isUpdating = false;
          },
          error: () => {
            this.isUpdating = false;
          }
        });
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Erreur lors de la modification.';
        this.isUpdating = false;
      }
    });
  }

  chargerConditionsDeTousLesEnfants(): void {
    this.errorMessage = '';

    if (!this.enfants.length) {
      this.conditionsSanitaires = [];
      this.enfantsAvecSante = [];
      this.observationsParEnfant = {};
      return;
    }

    const requetes = this.enfants.map((enfant) =>
      this.enfantService.getConditionsParEnfant(enfant.id).pipe(
        map((conditions: any[]) =>
          (conditions || []).map((condition) => ({
            ...condition,
            enfant
          }))
        ),
        catchError((err) => {
          console.error(`Erreur chargement conditions enfant ${enfant.id}`, err);
          return of([]);
        })
      )
    );

    forkJoin(requetes).subscribe({
      next: (resultats) => {
        this.conditionsSanitaires = resultats.flat() as ConditionSanitaireFront[];

        this.enfantsAvecSante = this.enfants.map((enfant) => ({
  enfant,
  conditions: this.conditionsSanitaires.filter(
    (condition) => condition.enfant?.id === enfant.id
  )
}));

this.healthPage = 1;

this.chargerTraitementsPourToutesLesConditions();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Impossible de charger les conditions sanitaires.';
      }
    });
  }

  chargerTraitementsPourToutesLesConditions(): void {
    const toutesLesConditions = this.enfantsAvecSante.flatMap((item) => item.conditions);

    if (!toutesLesConditions.length) {
      return;
    }

    const requetes = toutesLesConditions.map((condition) =>
      this.enfantService.getTraitementsParCondition(condition.id).pipe(
        map((traitements: any[]) => ({
          conditionId: condition.id,
          traitements: (traitements ?? []) as TraitementFront[]
        })),
        catchError((err) => {
          console.error(`Erreur chargement traitements condition ${condition.id}`, err);
          return of({
            conditionId: condition.id,
            traitements: []
          });
        })
      )
    );

    forkJoin(requetes).subscribe({
      next: (resultats) => {
        this.enfantsAvecSante = this.enfantsAvecSante.map((item) => ({
          ...item,
          conditions: item.conditions.map((condition) => {
            const match = resultats.find((r) => r.conditionId === condition.id);
            return {
              ...condition,
              traitements: match?.traitements ?? []
            };
          })
        }));

        this.chargerObservationsPourTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
      }
    });
  }

  chargerObservationsPourTousLesEnfants(): void {
    if (!this.enfants.length) {
      this.observationsParEnfant = {};
      return;
    }

    if (this.isLoadingObservations) {
      return;
    }

    this.isLoadingObservations = true;

    const requetes = this.enfants.map((enfant) =>
      this.enfantService.getObservationsParentParEnfant(enfant.id).pipe(
        map((observations: any[]) => ({
          enfantId: enfant.id,
          observations: (observations ?? []) as ObservationFront[]
        })),
        catchError((err) => {
          console.error(`Erreur chargement observations enfant ${enfant.id}`, err);
          return of({
            enfantId: enfant.id,
            observations: [] as ObservationFront[]
          });
        })
      )
    );

    forkJoin(requetes).subscribe({
      next: (resultats) => {
        const mapById: Record<number, ObservationFront[]> = {};
        resultats.forEach((entry) => {
          mapById[entry.enfantId] = entry.observations;
        });
        this.observationsParEnfant = mapById;
        this.isLoadingObservations = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoadingObservations = false;
      }
    });
  }

  ajouterConditionSanitaire(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.nouvelleCondition.enfantId) {
      this.errorMessage = 'Veuillez choisir un enfant.';
      return;
    }

    if (!this.nouvelleCondition.nomCondition.trim()) {
      this.errorMessage = 'Le nom de la condition est obligatoire.';
      return;
    }

    if (!this.nouvelleCondition.type) {
      this.errorMessage = 'Le type de condition est obligatoire.';
      return;
    }

    if (!this.nouvelleCondition.dateDebut) {
      this.errorMessage = 'La date de debut est obligatoire.';
      return;
    }

    if (this.isDateBefore(this.nouvelleCondition.dateDebut, this.todayIso)) {
      this.errorMessage = 'La date de debut ne peut pas etre dans le passe.';
      return;
    }

    if (
      this.nouvelleCondition.type === 'MALADIE_TEMPORAIRE' &&
      !this.nouvelleCondition.dateFin
    ) {
      this.errorMessage = 'La date de fin est obligatoire pour une maladie temporaire.';
      return;
    }

    if (this.nouvelleCondition.dateFin && this.isDateBefore(this.nouvelleCondition.dateFin, this.nouvelleCondition.dateDebut)) {
      this.errorMessage = 'La date de fin doit etre apres la date de debut.';
      return;
    }

    const payload = {
      nomCondition: this.nouvelleCondition.nomCondition.trim(),
      type: this.nouvelleCondition.type,
      description: this.nouvelleCondition.description.trim(),
      dateDebut: this.nouvelleCondition.dateDebut,
      dateFin: this.nouvelleCondition.dateFin || null
    };

    this.isSavingCondition = true;

    this.enfantService
      .ajouterConditionSanitaire(this.nouvelleCondition.enfantId, payload)
      .subscribe({
        next: () => {
          this.errorMessage = '';
          this.successMessage = 'Condition sanitaire ajoutee avec succes.';
          this.resetConditionForm();
          this.isSavingCondition = false;
          this.chargerConditionsDeTousLesEnfants();
        },
        error: (err) => {
          console.error('Erreur backend condition sanitaire :', err);
          this.errorMessage =
            err?.error?.message ||
            err?.error?.error ||
            err?.error ||
            err?.message ||
            "Erreur lors de l'ajout de la condition sanitaire.";
          this.isSavingCondition = false;
        }
      });
  }

  prefillConditionForChild(enfantId: number): void {
    this.nouvelleCondition.enfantId = enfantId;
  }

  voirDetailsCondition(condition: ConditionSanitaireFront): void {
    this.conditionDetail = condition;
    this.isEditingCondition = false;
    this.showTraitementForm = false;
    this.errorMessage = '';
    this.successMessage = '';

    if (condition.id) {
      this.chargerTraitementsCondition(condition.id);
    }
  }

  fermerDetailsCondition(): void {
    this.conditionDetail = null;
    this.isEditingCondition = false;
  }

  activerModificationCondition(): void {
    if (!this.conditionDetail) {
      return;
    }

    this.isEditingCondition = true;

    this.editConditionForm = {
      id: this.conditionDetail.id,
      enfantId: this.conditionDetail.enfant?.id ?? null,
      nomCondition: this.conditionDetail.nomCondition || '',
      type: this.conditionDetail.type || '',
      description: this.conditionDetail.description || '',
      dateDebut: this.conditionDetail.dateDebut || '',
      dateFin: this.conditionDetail.dateFin || ''
    };
  }

  annulerModificationCondition(): void {
    this.isEditingCondition = false;
  }

  enregistrerModificationCondition(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.editConditionForm.nomCondition.trim()) {
      this.errorMessage = 'Le nom de la condition est obligatoire.';
      return;
    }

    if (!this.editConditionForm.type) {
      this.errorMessage = 'Le type de condition est obligatoire.';
      return;
    }

    if (!this.editConditionForm.dateDebut) {
      this.errorMessage = 'La date de debut est obligatoire.';
      return;
    }

    if (
      this.editConditionForm.type === 'MALADIE_TEMPORAIRE' &&
      !this.editConditionForm.dateFin
    ) {
      this.errorMessage = 'La date de fin est obligatoire pour une maladie temporaire.';
      return;
    }

    const payload = {
      nomCondition: this.editConditionForm.nomCondition.trim(),
      type: this.editConditionForm.type,
      description: this.editConditionForm.description.trim(),
      dateDebut: this.editConditionForm.dateDebut,
      dateFin:
        this.editConditionForm.type === 'MALADIE_CHRONIQUE'
          ? null
          : this.editConditionForm.dateFin || null
    };

    this.isUpdatingCondition = true;

    this.enfantService.modifierConditionSanitaire(this.editConditionForm.id, payload).subscribe({
      next: (updatedCondition) => {
        this.successMessage = 'Condition sanitaire modifiee avec succes.';
        this.isUpdatingCondition = false;
        this.isEditingCondition = false;

        const enfantAssocie =
          this.enfants.find((e) => e.id === this.editConditionForm.enfantId) || this.conditionDetail?.enfant;

        this.conditionDetail = {
          ...updatedCondition,
          enfant: enfantAssocie
        };

        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage =
          err?.error?.message ||
          err?.error ||
          'Erreur lors de la modification de la condition.';
        this.isUpdatingCondition = false;
      }
    });
  }

  supprimerCondition(): void {
    if (!this.conditionDetail?.id) {
      this.errorMessage = 'Condition introuvable.';
      return;
    }

    const confirme = window.confirm('Voulez-vous vraiment supprimer cette condition sanitaire ?');
    if (!confirme) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.isDeletingCondition = true;

    this.enfantService.supprimerConditionSanitaire(this.conditionDetail.id).subscribe({
      next: () => {
        this.successMessage = 'Condition sanitaire supprimee avec succes.';
        this.isDeletingCondition = false;
        this.conditionDetail = null;
        this.isEditingCondition = false;
        this.conditionSelectionnee = null;
        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage =
          err?.error?.message ||
          err?.error ||
          'Erreur lors de la suppression de la condition.';
        this.isDeletingCondition = false;
      }
    });
  }

  preparerTraitement(condition: ConditionSanitaireFront): void {
    this.conditionSelectionnee = condition;
    this.showTraitementForm = true;
    this.chargerTraitementsCondition(condition.id);
  }

  chargerTraitementsCondition(conditionId: number): void {
    this.enfantService.getTraitementsParCondition(conditionId).subscribe({
      next: (data) => {
        this.conditionSelectionnee = this.conditionSelectionnee
          ? {
              ...this.conditionSelectionnee,
              traitements: (data ?? []) as TraitementFront[]
            }
          : this.conditionSelectionnee;

        this.enfantsAvecSante = this.enfantsAvecSante.map((item) => ({
          ...item,
          conditions: item.conditions.map((condition) =>
            condition.id === conditionId
              ? { ...condition, traitements: (data ?? []) as TraitementFront[] }
              : condition
          )
        }));
      },
      error: (err) => {
        console.error('Erreur chargement traitements :', err);
        this.errorMessage =
          err?.error?.message ||
          err?.error?.error ||
          err?.error ||
          'Impossible de charger les traitements.';
      }
    });
  }

  ajouterTraitement(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.conditionSelectionnee?.id) {
      this.errorMessage = 'Condition sanitaire introuvable.';
      return;
    }

    if (!this.nouveauTraitement.nomTraitement.trim()) {
      this.errorMessage = 'Le nom du traitement est obligatoire.';
      return;
    }

    if (!this.nouveauTraitement.ordonnancePdf) {
      this.errorMessage = "L'ordonnance PDF est obligatoire.";
      return;
    }

    if (this.nouveauTraitement.ordonnancePdf.type && this.nouveauTraitement.ordonnancePdf.type !== 'application/pdf') {
      this.errorMessage = "L'ordonnance doit etre un fichier PDF.";
      return;
    }

    if (!this.nouveauTraitement.dateDebut) {
      this.errorMessage = 'La date de debut du traitement est obligatoire.';
      return;
    }

    if (this.isDateBefore(this.nouveauTraitement.dateDebut, this.todayIso)) {
      this.errorMessage = 'La date de debut ne peut pas etre dans le passe.';
      return;
    }

    if (this.nouveauTraitement.dateFin && this.isDateBefore(this.nouveauTraitement.dateFin, this.nouveauTraitement.dateDebut)) {
      this.errorMessage = 'La date de fin doit etre apres la date de debut.';
      return;
    }

    const heuresPrises = [
      this.nouveauTraitement.heure1,
      this.nouveauTraitement.heure2,
      this.nouveauTraitement.heure3
    ].filter((h) => !!h) as string[];

    if (heuresPrises.length === 0) {
      this.errorMessage = 'Ajoute au moins une heure de prise.';
      return;
    }

    const payload = {
      nomTraitement: this.nouveauTraitement.nomTraitement.trim(),
      description: this.nouveauTraitement.description.trim(),
      dateDebut: this.nouveauTraitement.dateDebut,
      dateFin: this.nouveauTraitement.dateFin || null,
      heuresPrises
    };

    const formData = new FormData();
    formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    formData.append('ordonnancePdf', this.nouveauTraitement.ordonnancePdf);

    this.isSavingTraitement = true;

    this.enfantService.ajouterTraitementAvecOrdonnance(this.conditionSelectionnee.id, formData).subscribe({
      next: (created) => {
        const statut = String((created as any)?.statut ?? '');
        if (statut === 'VALIDE') {
          this.successMessage = 'Traitement valide automatiquement.';
        } else if (statut === 'REFUSE') {
          this.errorMessage = 'Traitement refuse automatiquement. Verifiez les informations et l ordonnance.';
        } else {
          this.successMessage = 'Traitement ajoute avec succes et envoye pour validation.';
        }
        this.resetTraitementForm();
        this.isSavingTraitement = false;
        this.showTraitementForm = false;
        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        this.extractHttpErrorDetails(err, (details) => {
          this.errorMessage = `Erreur lors de l'ajout du traitement${status}. ${details}`.trim();
        });
        this.isSavingTraitement = false;
      }
    });
  }

  annulerAjoutTraitement(): void {
    this.showTraitementForm = false;
    this.resetTraitementForm();
  }

  telechargerOrdonnance(traitement: TraitementFront): void {
    if (!traitement?.id) {
      this.errorMessage = 'Traitement introuvable.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.enfantService.telechargerOrdonnance(traitement.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ordonnance-${traitement.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error(err);
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        const details = err?.error?.message || err?.error || err?.message || '';
        this.errorMessage = `Impossible de telecharger l'ordonnance${status}. ${details}`.trim();
      }
    });
  }

  activerModificationTraitement(conditionId: number, traitement: TraitementFront): void {
    const heures = traitement.heuresPrises ?? [];

    this.errorMessage = '';
    this.successMessage = '';
    this.showTraitementForm = false;
    this.isEditingTraitement = true;

    this.editTraitementForm = {
      id: traitement.id,
      conditionId,
      nomTraitement: traitement.nomTraitement ?? '',
      description: traitement.description ?? '',
      ordonnance: traitement.ordonnance ?? '',
      dateDebut: traitement.dateDebut ?? '',
      dateFin: (traitement.dateFin as string) ?? '',
      heure1: heures[0] ?? '',
      heure2: heures[1] ?? '',
      heure3: heures[2] ?? ''
    };

    const nb = (heures ?? []).filter((h) => !!h).length;
    this.editTraitementHeuresCount = Math.min(3, Math.max(1, nb));
  }

  annulerModificationTraitement(): void {
    this.isEditingTraitement = false;
    this.isUpdatingTraitement = false;
    this.editTraitementHeuresCount = 1;
    this.editTraitementForm = {
      id: 0,
      conditionId: 0,
      nomTraitement: '',
      description: '',
      ordonnance: '',
      dateDebut: '',
      dateFin: '',
      heure1: '',
      heure2: '',
      heure3: ''
    };
  }

  ajouterHeureNouveauTraitement(): void {
    if (this.nouveauTraitementHeuresCount < 3) {
      this.nouveauTraitementHeuresCount += 1;
    }
  }

  retirerDerniereHeureNouveauTraitement(): void {
    if (this.nouveauTraitementHeuresCount <= 1) {
      return;
    }

    if (this.nouveauTraitementHeuresCount === 3) {
      this.nouveauTraitement.heure3 = '';
    } else if (this.nouveauTraitementHeuresCount === 2) {
      this.nouveauTraitement.heure2 = '';
    }

    this.nouveauTraitementHeuresCount -= 1;
  }

  ajouterHeureEditTraitement(): void {
    if (this.editTraitementHeuresCount < 3) {
      this.editTraitementHeuresCount += 1;
    }
  }

  retirerDerniereHeureEditTraitement(): void {
    if (this.editTraitementHeuresCount <= 1) {
      return;
    }

    if (this.editTraitementHeuresCount === 3) {
      this.editTraitementForm.heure3 = '';
    } else if (this.editTraitementHeuresCount === 2) {
      this.editTraitementForm.heure2 = '';
    }

    this.editTraitementHeuresCount -= 1;
  }

  enregistrerModificationTraitement(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.editTraitementForm.id || !this.editTraitementForm.conditionId) {
      this.errorMessage = 'Traitement introuvable.';
      return;
    }

    if (!this.editTraitementForm.nomTraitement.trim()) {
      this.errorMessage = 'Le nom du traitement est obligatoire.';
      return;
    }

    if (!this.editTraitementForm.ordonnance.trim()) {
      this.errorMessage = "L'ordonnance est obligatoire.";
      return;
    }

    if (!this.editTraitementForm.dateDebut) {
      this.errorMessage = 'La date de debut du traitement est obligatoire.';
      return;
    }

    const heuresPrises = [
      this.editTraitementForm.heure1,
      this.editTraitementForm.heure2,
      this.editTraitementForm.heure3
    ].filter((h) => !!h) as string[];

    if (heuresPrises.length === 0) {
      this.errorMessage = 'Ajoute au moins une heure de prise.';
      return;
    }

    const payload = {
      nomTraitement: this.editTraitementForm.nomTraitement.trim(),
      description: this.editTraitementForm.description.trim(),
      ordonnance: this.editTraitementForm.ordonnance.trim(),
      dateDebut: this.editTraitementForm.dateDebut,
      dateFin: this.editTraitementForm.dateFin || null,
      heuresPrises
    };

    this.isUpdatingTraitement = true;

    this.enfantService.modifierTraitementParent(this.editTraitementForm.id, payload).subscribe({
      next: () => {
        this.successMessage = 'Traitement modifie avec succes (renvoye pour validation).';
        const conditionId = this.editTraitementForm.conditionId;
        this.isUpdatingTraitement = false;
        this.annulerModificationTraitement();
        this.chargerTraitementsCondition(conditionId);
      },
      error: (err) => {
        console.error(err);
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        const details = err?.error?.message || err?.error || err?.message || '';
        this.errorMessage = `Erreur lors de la modification du traitement${status}. ${details}`.trim();
        this.isUpdatingTraitement = false;
      }
    });
  }

  supprimerTraitement(conditionId: number, traitement: TraitementFront): void {
    if (this.isDeletingTraitement) {
      return;
    }

    if (!traitement?.id) {
      this.errorMessage = 'Traitement introuvable.';
      return;
    }

    const confirme = window.confirm('Voulez-vous vraiment supprimer ce traitement ?');
    if (!confirme) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.isDeletingTraitement = true;

    this.enfantService.supprimerTraitementParent(traitement.id).subscribe({
      next: () => {
        this.successMessage = 'Traitement supprime.';
        this.isDeletingTraitement = false;
        this.chargerTraitementsCondition(conditionId);
      },
      error: (err) => {
        console.error(err);
        const status = err?.status != null ? ` (HTTP ${err.status})` : '';
        const details = err?.error?.message || err?.error || err?.message || '';
        this.errorMessage = `Erreur lors de la suppression du traitement${status}. ${details}`.trim();
        this.isDeletingTraitement = false;
      }
    });
  }

  totalConditions(): number {
    return this.enfantsAvecSante.reduce((total, item) => total + item.conditions.length, 0);
  }

  totalTraitements(): number {
    return this.enfantsAvecSante.reduce(
      (total, item) =>
        total +
        item.conditions.reduce(
          (subTotal, condition) => subTotal + (condition.traitements?.length ?? 0),
          0
        ),
      0
    );
  }

  totalTraitementsValides(): number {
    return this.enfantsAvecSante.reduce(
      (total, item) =>
        total +
        item.conditions.reduce(
          (subTotal, condition) =>
            subTotal +
            (condition.traitements?.filter(
              (traitement) =>
                traitement.statut === 'VALIDE' || traitement.statut === 'ACTIF'
            ).length ?? 0),
          0
        ),
      0
    );
  }

  validatePhone(phone: string): boolean {
    return /^\d{8}$/.test((phone || '').trim());
  }

  validateGroupeSanguin(value: string): boolean {
    return this.groupesSanguins.includes((value || '').trim());
  }

  isAgeStrictlyGreaterThanTwo(dateNaissance: string): boolean {
    if (!dateNaissance) {
      return false;
    }

    const birthDate = new Date(dateNaissance);
    const today = new Date();

    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (
      monthDiff < 0 ||
      (monthDiff === 0 && today.getDate() < birthDate.getDate())
    ) {
      age--;
    }

    return age > 2;
  }

  getInitial(prenom: string, nom: string): string {
    return (prenom?.[0] || nom?.[0] || '?').toUpperCase();
  }

  getAge(dateNaissance: string): number | null {
    if (!dateNaissance) {
      return null;
    }

    const birthDate = new Date(dateNaissance);
    const today = new Date();

    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();

    if (
      monthDiff < 0 ||
      (monthDiff === 0 && today.getDate() < birthDate.getDate())
    ) {
      age--;
    }

    return age;
  }

  private resetForm(): void {
    this.nouvelEnfant = {
      prenom: '',
      nom: '',
      dateNaissance: '',
      contactUrgence: '',
      allergies: '',
      groupeSanguin: '',
      notes: '',
      photoDataUrl: '',
      adresse: '',
      adresseLat: null,
      adresseLng: null
    };
    this.nouvelEnfantAllergiesSelection = [];
    this.nouvelEnfantAllergiesAutres = '';
  }

  onNouvelEnfantPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;

    if (!file) {
      this.nouvelEnfant.photoDataUrl = '';
      return;
    }

    if (!file.type?.startsWith('image/')) {
      this.errorMessage = 'Veuillez choisir une image (PNG, JPG...).';
      this.nouvelEnfant.photoDataUrl = '';
      input.value = '';
      return;
    }

    const maxBytes = 2 * 1024 * 1024; // 2MB
    if (file.size > maxBytes) {
      this.errorMessage = 'Image trop grande. Maximum 2 Mo.';
      this.nouvelEnfant.photoDataUrl = '';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      this.nouvelEnfant.photoDataUrl = result || '';
      if (this.nouvelEnfant.photoDataUrl) {
        this.errorMessage = '';
      }
    };
    reader.onerror = () => {
      this.errorMessage = "Impossible de lire l'image selectionnee.";
      this.nouvelEnfant.photoDataUrl = '';
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  clearNouvelEnfantPhoto(photoInput?: HTMLInputElement | null): void {
    this.nouvelEnfant.photoDataUrl = '';
    if (photoInput) {
      photoInput.value = '';
    }
  }

  onEditEnfantPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;

    if (!file) {
      return;
    }

    if (!file.type?.startsWith('image/')) {
      this.errorMessage = 'Veuillez choisir une image (PNG, JPG...).';
      input.value = '';
      return;
    }

    const maxBytes = 2 * 1024 * 1024; // 2MB
    if (file.size > maxBytes) {
      this.errorMessage = 'Image trop grande. Maximum 2 Mo.';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      this.editEnfantForm.photo = result || '';
      if (this.editEnfantForm.photo) {
        this.errorMessage = '';
      }
    };
    reader.onerror = () => {
      this.errorMessage = "Impossible de lire l'image selectionnee.";
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  clearEditEnfantPhoto(photoInput?: HTMLInputElement | null): void {
    this.editEnfantForm.photo = '';
    if (photoInput) {
      photoInput.value = '';
    }
  }

  private resetConditionForm(): void {
    this.nouvelleCondition = {
      enfantId: null,
      nomCondition: '',
      type: '',
      description: '',
      dateDebut: '',
      dateFin: ''
    };
  }

  private resetTraitementForm(): void {
    this.nouveauTraitement = {
      nomTraitement: '',
      description: '',
      ordonnancePdf: null,
      dateDebut: '',
      dateFin: '',
      heure1: '',
      heure2: '',
      heure3: ''
    };
    this.nouveauTraitementHeuresCount = 1;
  }

  private getLocalTodayIso(): string {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private extractHttpErrorDetails(err: any, cb: (details: string) => void): void {
    const fallback = err?.error?.message || err?.message || 'Erreur inconnue.';
    const raw = err?.error;

    if (raw instanceof Blob) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const text = String(reader.result ?? '');
          const parsed = JSON.parse(text);
          cb(parsed?.message || text || fallback);
        } catch {
          cb(String(reader.result ?? '') || fallback);
        }
      };
      reader.onerror = () => cb(fallback);
      reader.readAsText(raw);
      return;
    }

    cb(typeof raw === 'string' ? raw : fallback);
  }

  private isDateBefore(aIso: string, bIso: string): boolean {
    const a = (aIso || '').trim();
    const b = (bIso || '').trim();
    if (!a || !b) return false;
    return a < b;
=======
>>>>>>> origin/gestion-evenements
=======
>>>>>>> origin/gestion_boutique
  }
}
