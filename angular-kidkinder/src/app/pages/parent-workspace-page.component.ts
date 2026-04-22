import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from '../shared/auth.service';
import { Enfant, EnfantDTO, EnfantService } from '../services/enfant.service';
import { ParentChangementsComponent } from '../components/parent-changements.component';

type ParentPageKey =
  | 'tableau-de-bord'
  | 'enfants'
  | 'sante'
  | 'changements'
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
  changements: {
    chip: 'Changements',
    title: 'Changements & observations',
    description: 'Consultez les changements declares par l equipe, et suivez les informations importantes.'
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
  imports: [CommonModule, FormsModule, ParentChangementsComponent],
  templateUrl: './parent-workspace-page.component.html',
  styleUrl: './parent-workspace-page.component.css'
})
export class ParentWorkspacePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly enfantService = inject(EnfantService);

  protected readonly page = signal<ParentPageKey>('tableau-de-bord');
  protected readonly todayLabel = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  }).format(new Date());

  protected readonly pageMeta = computed(() => pageMetaMap[this.page()]);
  protected readonly pageTitle = computed(() => {
    const firstName = this.authService.getCurrentUser()?.nom?.split(' ')[0] ?? 'Parent';
    return this.pageMeta().title.replace('{{name}}', firstName);
  });

  readonly groupesSanguins: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

  enfants: Enfant[] = [];
  enfantsAvecSante: EnfantAvecSante[] = [];
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
    photoDataUrl: ''
  };

  editEnfantForm = {
    id: 0,
    prenom: '',
    nom: '',
    dateNaissance: '',
    contactUrgence: '',
    allergies: '',
    groupeSanguin: '',
    photo: ''
  };

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

  onOrdonnanceSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.nouveauTraitement.ordonnancePdf = file;
  }

  public constructor() {
    this.route.data.subscribe((data) => {
      this.page.set(data['page'] as ParentPageKey);
    });

    this.chargerEnfants();
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
    const allergies = this.nouvelEnfant.allergies.trim();
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
      photo: this.selectedEnfant.photo || ''
    };
  }

  annulerModification(): void {
    this.isEditingEnfant = false;
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
    const allergies = this.editEnfantForm.allergies.trim();
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

    if (
      this.nouvelleCondition.type === 'MALADIE_TEMPORAIRE' &&
      !this.nouvelleCondition.dateFin
    ) {
      this.errorMessage = 'La date de fin est obligatoire pour une maladie temporaire.';
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
      next: () => {
        this.successMessage = 'Traitement ajoute avec succes et envoye pour validation.';
        this.resetTraitementForm();
        this.isSavingTraitement = false;
        this.showTraitementForm = false;
        this.chargerConditionsDeTousLesEnfants();
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = "Erreur lors de l'ajout du traitement.";
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
  }

  annulerModificationTraitement(): void {
    this.isEditingTraitement = false;
    this.isUpdatingTraitement = false;
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
      photoDataUrl: ''
    };
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
}
