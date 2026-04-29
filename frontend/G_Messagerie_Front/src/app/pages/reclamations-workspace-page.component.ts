import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { of, switchMap, Subscription, tap, throwError } from 'rxjs';
import { AuthService } from '../shared/auth.service';
import {
  Reclamation,
  ReclamationCategory,
  ReclamationPriority,
  ReclamationService,
  ReclamationStatus
} from '../shared/reclamation.service';

interface CreationFormSnapshot {
  title: string;
  description: string;
  category: ReclamationCategory | '';
  priority: ReclamationPriority | '';
}

interface CreationAssistant {
  suggestedTitle: string;
  suggestedDescription: string;
  suggestedCategory: ReclamationCategory;
  suggestedPriority: ReclamationPriority;
  urgencyLabel: string;
  summary: string;
  confidence: number;
  detectedSignals: string[];
  nextQuestion: string;
  hasEnoughText: boolean;
  suggestionApplied: boolean;
  qualityScore: number;
  strengths: string[];
  missingItems: string[];
  recommendedAttachments: string[];
  duplicate?: Reclamation;
}

type ParentWorkflowFilter = 'ALL' | 'TO_COMPLETE' | 'WAITING_ADMIN' | 'ANSWER_RECEIVED' | 'WATCH' | 'CLOSED';

interface ParentWorkflowItem {
  key: ParentWorkflowFilter;
  label: string;
  description: string;
}

type FollowUpLevel = 'NONE' | 'POSSIBLE' | 'RECOMMENDED' | 'CONFIRM_RESPONSE';

interface FollowUpInsight {
  level: FollowUpLevel;
  label: string;
  reason: string;
  message: string;
  preparedAt?: string | null;
}

@Component({
  selector: 'app-reclamations-workspace-page',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './reclamations-workspace-page.component.html',
  styleUrl: './reclamations-workspace-page.component.css'
})
export class ReclamationsWorkspacePageComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly reclamationService = inject(ReclamationService);
  private readonly formSubscription = new Subscription();
  private readonly followUpStorageKey = 'tinyspring.parent.followups';
  private readonly followUpConversationStorageKey = 'tinyspring.parent.followupConversations';

  protected readonly currentUser = this.authService.currentUser;
  protected readonly canUseReclamations = computed(() => this.currentUser()?.role === 'PARENT');
  protected readonly roleLabel = computed(() => (this.currentUser()?.role === 'ANIMATRICE' ? 'Espace animatrice' : 'Espace parent'));

  protected readonly isLoading = signal(false);
  protected readonly isSubmitting = signal(false);
  protected readonly isUpdating = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');
  protected readonly moderationMessage = signal('');
  protected readonly imageFile = signal<File | null>(null);
  protected readonly attachmentFile = signal<File | null>(null);
  protected readonly reclamations = signal<Reclamation[]>([]);
  protected readonly selectedReclamation = signal<Reclamation | null>(null);
  protected readonly editingReclamation = signal<Reclamation | null>(null);
  protected readonly preparedFollowUps = signal<Record<number, string>>(this.loadPreparedFollowUps());
  protected readonly followUpConversations = signal<Record<number, number>>(this.loadFollowUpConversations());
  protected readonly sendingFollowUpId = signal<number | null>(null);

  protected readonly filterSearch = signal('');
  protected readonly filterCategory = signal<ReclamationCategory | ''>('');
  protected readonly filterStatus = signal<ReclamationStatus | ''>('');
  protected readonly filterPriority = signal<ReclamationPriority | ''>('');
  protected readonly workflowFilter = signal<ParentWorkflowFilter>('ALL');

  protected readonly workflowItems: ParentWorkflowItem[] = [
    { key: 'ALL', label: 'Toutes', description: 'Toutes les reclamations' },
    { key: 'TO_COMPLETE', label: 'A completer', description: 'Informations utiles manquantes' },
    { key: 'WAITING_ADMIN', label: 'En attente', description: 'Traitement administratif en cours' },
    { key: 'ANSWER_RECEIVED', label: 'Reponse recue', description: 'Une reponse est disponible' },
    { key: 'WATCH', label: 'A surveiller', description: 'Priorite elevee ou escalade' },
    { key: 'CLOSED', label: 'Cloturees', description: 'Dossiers termines' }
  ];

  protected readonly categories: ReclamationCategory[] = [
    'REPAS',
    'TRANSPORT',
    'COMPORTEMENT',
    'HYGIENE',
    'SECURITE',
    'PERSONNEL',
    'FINANCIER',
    'PEDAGOGIQUE',
    'ADMINISTRATIF',
    'AUTRE'
  ];

  protected readonly priorities: ReclamationPriority[] = ['LOW', 'MEDIUM', 'HIGH'];
  protected readonly statuses: ReclamationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    category: ['' as ReclamationCategory | ''],
    priority: ['' as ReclamationPriority | '']
  });

  protected readonly formSnapshot = signal<CreationFormSnapshot>({
    title: '',
    description: '',
    category: '',
    priority: ''
  });

  protected readonly creationAssistant = computed(() =>
    this.buildCreationAssistant(this.formSnapshot(), this.imageFile(), this.attachmentFile(), this.reclamations())
  );

  protected readonly editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    category: ['' as ReclamationCategory | ''],
    priority: ['' as ReclamationPriority | '']
  });

  protected readonly totalCount = computed(() => this.reclamations().length);
  protected readonly openCount = computed(() => this.reclamations().filter((item) => item.status === 'OPEN').length);
  protected readonly inProgressCount = computed(() => this.reclamations().filter((item) => item.status === 'IN_PROGRESS').length);
  protected readonly answeredCount = computed(
    () => this.reclamations().filter((item) => !!item.adminComment || item.status === 'RESOLVED').length
  );

  protected readonly filteredReclamations = computed(() => {
    const search = this.normalize(this.filterSearch());

    return this.reclamations().filter((rec) => {
      const text = this.normalize(`${rec.title || ''} ${rec.description || ''}`);
      const matchSearch = !search || text.includes(search);
      const matchCategory = !this.filterCategory() || rec.category === this.filterCategory();
      const matchStatus = !this.filterStatus() || rec.status === this.filterStatus();
      const matchPriority = !this.filterPriority() || rec.priority === this.filterPriority();
      const matchWorkflow = this.workflowFilter() === 'ALL' || this.getWorkflowState(rec) === this.workflowFilter();

      return matchSearch && matchCategory && matchStatus && matchPriority && matchWorkflow;
    });
  });

  protected readonly workflowCounts = computed(() => {
    const counts = new Map<ParentWorkflowFilter, number>();
    this.workflowItems.forEach((item) => counts.set(item.key, 0));
    counts.set('ALL', this.reclamations().length);

    this.reclamations().forEach((rec) => {
      const state = this.getWorkflowState(rec);
      counts.set(state, (counts.get(state) || 0) + 1);
    });

    return counts;
  });

  ngOnInit(): void {
    this.formSubscription.add(
      this.form.valueChanges.subscribe(() => {
        this.formSnapshot.set(this.form.getRawValue());
      })
    );
    this.loadReclamations();
  }

  ngOnDestroy(): void {
    this.formSubscription.unsubscribe();
  }

  protected loadReclamations(): void {
    this.clearMessages();

    if (!this.canUseReclamations()) {
      this.reclamations.set([]);
      this.selectedReclamation.set(null);
      this.errorMessage.set("Le backend actuel reserve les reclamations au parent et a l'administration.");
      return;
    }

    this.isLoading.set(true);
    this.reclamationService.getMine().subscribe({
      next: (items) => {
        this.reclamations.set(this.sortForParent(items));
        this.selectedReclamation.set(this.reclamations()[0] ?? null);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos reclamations pour le moment.');
        this.isLoading.set(false);
      }
    });
  }

  protected onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.imageFile.set(input.files?.[0] ?? null);
  }

  protected onAttachmentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.attachmentFile.set(input.files?.[0] ?? null);
  }

  protected applyAssistantSuggestion(): void {
    const assistant = this.creationAssistant();
    const current = this.form.getRawValue();

    this.form.patchValue({
      title: current.title.trim() ? current.title : assistant.suggestedTitle,
      description: current.description.trim() ? current.description : assistant.suggestedDescription,
      category: assistant.suggestedCategory,
      priority: assistant.suggestedPriority
    });
    this.moderationMessage.set('Suggestion appliquee: titre, description, categorie et priorite mis a jour si necessaire.');
  }

  protected submitReclamation(): void {
    this.clearMessages();

    if (!this.canUseReclamations()) {
      this.errorMessage.set('La creation de reclamation est reservee au parent.');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Veuillez completer le titre et la description.');
      return;
    }

    const value = this.form.getRawValue();
    const originalTitle = value.title.trim();
    const originalDescription = value.description.trim();

    this.isSubmitting.set(true);
    this.reclamationService
      .create({
        title: originalTitle,
        description: originalDescription,
        category: value.category,
        priority: value.priority,
        image: this.imageFile(),
        attachment: this.attachmentFile()
      })
      .subscribe({
        next: (created) => {
          this.reclamations.update((items) => this.sortForParent([created, ...items]));
          this.selectedReclamation.set(created);
          this.form.reset({ title: '', description: '', category: '', priority: '' });
          this.imageFile.set(null);
          this.attachmentFile.set(null);
          this.successMessage.set('Votre reclamation a bien ete envoyee.');

          if (this.wasContentModerated(originalTitle, originalDescription, created)) {
            this.moderationMessage.set('Certains mots inappropries ont ete filtres automatiquement.');
          }

          this.isSubmitting.set(false);
        },
        error: () => {
          this.errorMessage.set("La reclamation n'a pas pu etre envoyee.");
          this.isSubmitting.set(false);
        }
      });
  }

  protected selectReclamation(reclamation: Reclamation): void {
    this.selectedReclamation.set(reclamation);
    this.reclamationService.getById(reclamation.id).subscribe({
      next: (details) => this.selectedReclamation.set(details),
      error: () => this.errorMessage.set('Impossible de charger le detail de cette reclamation.')
    });
  }

  protected startEdit(reclamation: Reclamation): void {
    this.clearMessages();
    this.editingReclamation.set(reclamation);
    this.editForm.reset({
      title: reclamation.title,
      description: reclamation.description,
      category: reclamation.category ?? '',
      priority: reclamation.priority ?? ''
    });
  }

  protected cancelEdit(): void {
    this.editingReclamation.set(null);
    this.editForm.reset({ title: '', description: '', category: '', priority: '' });
  }

  protected updateReclamation(): void {
    this.clearMessages();

    const current = this.editingReclamation();
    if (!current) {
      this.errorMessage.set('Aucune reclamation selectionnee.');
      return;
    }

    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      this.errorMessage.set('Veuillez completer le titre et la description.');
      return;
    }

    const value = this.editForm.getRawValue();
    const originalTitle = value.title.trim();
    const originalDescription = value.description.trim();

    this.isUpdating.set(true);
    this.reclamationService
      .update(current.id, {
        title: originalTitle,
        description: originalDescription,
        category: value.category,
        priority: value.priority
      })
      .subscribe({
        next: (saved) => {
          this.reclamations.update((items) => this.sortForParent(items.map((item) => (item.id === saved.id ? saved : item))));
          this.selectedReclamation.set(saved);
          this.cancelEdit();
          this.successMessage.set('Reclamation modifiee avec succes.');

          if (this.wasContentModerated(originalTitle, originalDescription, saved)) {
            this.moderationMessage.set('Certains mots inappropries ont ete filtres automatiquement.');
          }

          this.isUpdating.set(false);
        },
        error: () => {
          this.errorMessage.set('Impossible de modifier la reclamation.');
          this.isUpdating.set(false);
        }
      });
  }

  protected deleteReclamation(reclamation: Reclamation): void {
    if (!confirm('Voulez-vous vraiment supprimer cette reclamation ?')) {
      return;
    }

    this.clearMessages();
    this.reclamationService.delete(reclamation.id).subscribe({
      next: () => {
        this.reclamations.update((items) => items.filter((item) => item.id !== reclamation.id));
        this.selectedReclamation.set(this.reclamations()[0] ?? null);
        this.successMessage.set('Reclamation supprimee avec succes.');
      },
      error: () => this.errorMessage.set('Impossible de supprimer la reclamation.')
    });
  }

  protected openAttachment(path?: string | null): void {
    const url = this.fileUrl(path);
    if (url) {
      window.open(url, '_blank');
    }
  }

  protected downloadAttachment(path?: string | null, fileName?: string | null): void {
    const url = this.fileUrl(path);
    if (!url) {
      return;
    }

    this.reclamationService.downloadFile(url).subscribe({
      next: (blob) => {
        const blobUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = fileName?.trim() || 'piece-jointe';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(blobUrl);
      },
      error: () => this.errorMessage.set('Impossible de telecharger la piece jointe.')
    });
  }

  protected clearFilters(): void {
    this.filterSearch.set('');
    this.filterCategory.set('');
    this.filterStatus.set('');
    this.filterPriority.set('');
    this.workflowFilter.set('ALL');
  }

  protected setWorkflowFilter(filter: ParentWorkflowFilter): void {
    this.workflowFilter.set(filter);
  }

  protected workflowCount(filter: ParentWorkflowFilter): number {
    return this.workflowCounts().get(filter) || 0;
  }

  protected getWorkflowState(reclamation: Reclamation): ParentWorkflowFilter {
    if (reclamation.status === 'RESOLVED' || reclamation.status === 'REJECTED') {
      return 'CLOSED';
    }

    if (
      reclamation.autoEscalated ||
      reclamation.smartPriorityLevel === 'CRITICAL' ||
      reclamation.smartPriorityLevel === 'HIGH' ||
      (reclamation.smartPriorityScore || 0) >= 70
    ) {
      return 'WATCH';
    }

    if (reclamation.adminComment?.trim()) {
      return 'ANSWER_RECEIVED';
    }

    if (this.needsParentCompletion(reclamation)) {
      return 'TO_COMPLETE';
    }

    return 'WAITING_ADMIN';
  }

  protected workflowLabel(reclamation: Reclamation): string {
    const state = this.getWorkflowState(reclamation);

    switch (state) {
      case 'TO_COMPLETE':
        return 'Action requise';
      case 'WAITING_ADMIN':
        return 'En attente admin';
      case 'ANSWER_RECEIVED':
        return 'Reponse disponible';
      case 'WATCH':
        return 'A surveiller';
      case 'CLOSED':
        return 'Cloturee';
      default:
        return 'Toutes';
    }
  }

  protected workflowClass(reclamation: Reclamation): string {
    const state = this.getWorkflowState(reclamation);

    if (state === 'WATCH') return 'workflow--danger';
    if (state === 'ANSWER_RECEIVED') return 'workflow--success';
    if (state === 'TO_COMPLETE') return 'workflow--warning';
    if (state === 'CLOSED') return 'workflow--muted';
    return 'workflow--info';
  }

  protected statusLabel(status?: ReclamationStatus | string | null): string {
    switch (status) {
      case 'OPEN':
        return 'Ouverte';
      case 'IN_PROGRESS':
        return 'En traitement';
      case 'RESOLVED':
        return 'Resolue';
      case 'REJECTED':
        return 'Rejetee';
      default:
        return status || '-';
    }
  }

  protected priorityLabel(priority?: ReclamationPriority | string | null): string {
    switch (priority) {
      case 'LOW':
        return 'Faible';
      case 'MEDIUM':
        return 'Moyenne';
      case 'HIGH':
        return 'Haute';
      default:
        return priority || '-';
    }
  }

  protected categoryLabel(category?: ReclamationCategory | string | null): string {
    switch (category) {
      case 'REPAS':
        return 'Repas';
      case 'TRANSPORT':
        return 'Transport';
      case 'COMPORTEMENT':
        return 'Comportement';
      case 'HYGIENE':
        return 'Hygiene';
      case 'SECURITE':
        return 'Securite';
      case 'PERSONNEL':
        return 'Personnel';
      case 'FINANCIER':
        return 'Financier';
      case 'PEDAGOGIQUE':
        return 'Pedagogique';
      case 'ADMINISTRATIF':
        return 'Administratif';
      case 'AUTRE':
        return 'Autre';
      default:
        return category || '-';
    }
  }

  protected decisionLabel(decision?: string | null): string {
    switch (decision) {
      case 'REPAIR_NEEDED':
        return 'Reparation necessaire';
      case 'INCREASE_SUPERVISION':
        return 'Surveillance renforcee';
      case 'STAFF_TRAINING':
        return 'Formation du personnel';
      case 'PROCESS_IMPROVEMENT':
        return 'Amelioration processus';
      case 'ADMINISTRATIVE_CORRECTION':
        return 'Correction administrative';
      case 'MEDICAL_ATTENTION':
        return 'Attention medicale';
      case 'TRANSPORT_ESCALATION':
        return 'Escalade transport';
      case 'PARENT_FOLLOWUP':
        return 'Suivi parent';
      default:
        return decision || '-';
    }
  }

  protected confidencePercent(value?: number | null): string {
    return value === null || value === undefined ? '-' : `${(value * 100).toFixed(1)}%`;
  }

  protected statusClass(status?: ReclamationStatus | string | null): string {
    if (status === 'RESOLVED') return 'status--mint';
    if (status === 'IN_PROGRESS') return 'status--lime';
    if (status === 'REJECTED') return 'status--danger';
    return 'status--blush';
  }

  protected smartClass(level?: string | null): string {
    if (level === 'CRITICAL' || level === 'HIGH') return 'status--danger';
    if (level === 'MEDIUM') return 'status--lime';
    return 'status--mint';
  }

  protected slaLabel(reclamation: Reclamation): string {
    if (reclamation.status === 'RESOLVED' || reclamation.status === 'REJECTED') {
      return 'Cloturee';
    }

    if (reclamation.smartPriorityLevel === 'CRITICAL' || (reclamation.smartPriorityReason || '').toUpperCase().includes('SLA')) {
      return 'A surveiller';
    }

    return 'SLA OK';
  }

  protected workflowAdvice(reclamation: Reclamation): string {
    const state = this.getWorkflowState(reclamation);

    switch (state) {
      case 'TO_COMPLETE':
        return 'Ajoutez une date, un lieu, une photo ou plus de contexte pour accelerer le traitement.';
      case 'WAITING_ADMIN':
        return 'Votre demande est bien envoyee. L administration doit maintenant la traiter.';
      case 'ANSWER_RECEIVED':
        return 'Une reponse administrative est disponible. Consultez le detail avant toute nouvelle action.';
      case 'WATCH':
        return 'Cette reclamation est prioritaire ou escaladee. Surveillez son evolution.';
      case 'CLOSED':
        return 'Ce dossier est cloture. Vous pouvez consulter la reponse et les pieces jointes.';
      default:
        return 'Suivez l evolution de votre reclamation depuis cette carte.';
    }
  }

  protected followUpInsight(reclamation: Reclamation): FollowUpInsight {
    const preparedAt = this.preparedFollowUps()[reclamation.id] ?? null;
    const ageDays = this.getAgeDays(reclamation.createdAt);
    const hasAdminResponse = !!reclamation.adminComment?.trim();
    const isClosed = reclamation.status === 'RESOLVED' || reclamation.status === 'REJECTED';
    const isHighRisk =
      reclamation.autoEscalated ||
      reclamation.smartPriorityLevel === 'CRITICAL' ||
      reclamation.smartPriorityLevel === 'HIGH' ||
      (reclamation.smartPriorityScore || 0) >= 70;

    if (isClosed) {
      return {
        level: 'NONE',
        label: 'Aucune relance',
        reason: 'Le dossier est cloture.',
        message: '',
        preparedAt
      };
    }

    if (hasAdminResponse) {
      return {
        level: 'CONFIRM_RESPONSE',
        label: 'Reponse a confirmer',
        reason: "Une reponse administrative est disponible; vous pouvez demander une precision si elle n'est pas suffisante.",
        message: this.buildFollowUpMessage(reclamation, 'CONFIRM_RESPONSE', ageDays),
        preparedAt
      };
    }

    if (isHighRisk || ageDays >= 3) {
      return {
        level: 'RECOMMENDED',
        label: 'Relance recommandee',
        reason: isHighRisk
          ? 'Cette reclamation est prioritaire ou escaladee.'
          : `Cette reclamation attend une reponse depuis ${ageDays} jour(s).`,
        message: this.buildFollowUpMessage(reclamation, 'RECOMMENDED', ageDays),
        preparedAt
      };
    }

    if (reclamation.status === 'OPEN' || reclamation.status === 'IN_PROGRESS') {
      return {
        level: 'POSSIBLE',
        label: 'Relance possible',
        reason: "Vous pouvez preparer une relance si vous souhaitez demander l'avancement.",
        message: this.buildFollowUpMessage(reclamation, 'POSSIBLE', ageDays),
        preparedAt
      };
    }

    return {
      level: 'NONE',
      label: 'Aucune relance',
      reason: 'Aucune action parent recommandee pour le moment.',
      message: '',
      preparedAt
    };
  }

  protected followUpClass(reclamation: Reclamation): string {
    const level = this.followUpInsight(reclamation).level;

    if (level === 'RECOMMENDED') return 'follow-up--recommended';
    if (level === 'CONFIRM_RESPONSE') return 'follow-up--confirm';
    if (level === 'POSSIBLE') return 'follow-up--possible';
    return 'follow-up--none';
  }

  protected canPrepareFollowUp(reclamation: Reclamation): boolean {
    return this.followUpInsight(reclamation).level !== 'NONE';
  }

  protected prepareFollowUp(reclamation: Reclamation): void {
    const insight = this.followUpInsight(reclamation);

    if (!insight.message) {
      return;
    }

    this.clearMessages();
    this.sendingFollowUpId.set(reclamation.id);

    this.resolveFollowUpConversationId(reclamation)
      .pipe(switchMap((conversationId) => this.reclamationService.sendMessage(conversationId, insight.message)))
      .subscribe({
        next: () => {
          this.markFollowUpPrepared(reclamation.id);
          this.successMessage.set(`Relance envoyee a l'administration pour la reclamation #${reclamation.id}.`);
          this.sendingFollowUpId.set(null);
        },
        error: () => {
          void this.copyText(insight.message);
          this.markFollowUpPrepared(reclamation.id);
          this.errorMessage.set("Impossible d'envoyer automatiquement. Le message a ete copie pour un envoi manuel.");
          this.sendingFollowUpId.set(null);
        }
      });
  }

  protected followUpPreparedLabel(reclamation: Reclamation): string {
    const preparedAt = this.preparedFollowUps()[reclamation.id];

    if (!preparedAt) {
      return '';
    }

    return `Relance preparee le ${new Intl.DateTimeFormat('fr-FR').format(new Date(preparedAt))}`;
  }

  protected isSendingFollowUp(reclamation: Reclamation): boolean {
    return this.sendingFollowUpId() === reclamation.id;
  }

  protected attachmentIcon(fileName?: string | null, attachmentType?: string | null): string {
    const ext = this.attachmentExtension(fileName, attachmentType);
    if (ext === 'pdf') return 'PDF';
    if (['doc', 'docx'].includes(ext)) return 'DOC';
    if (['xls', 'xlsx', 'csv'].includes(ext)) return 'XLS';
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'image'].includes(ext)) return 'IMG';
    if (['zip', 'rar', '7z'].includes(ext)) return 'ZIP';
    return 'FILE';
  }

  protected fileUrl(path?: string | null): string | null {
    return this.reclamationService.getFileUrl(path);
  }

  private sortForParent(items: Reclamation[]): Reclamation[] {
    return [...items].sort((a, b) => {
      const aTime = new Date(a.createdAt || 0).getTime();
      const bTime = new Date(b.createdAt || 0).getTime();
      return bTime - aTime;
    });
  }

  private wasContentModerated(originalTitle: string, originalDescription: string, savedReclamation: Reclamation): boolean {
    return originalTitle !== savedReclamation.title || originalDescription !== savedReclamation.description;
  }

  private attachmentExtension(fileName?: string | null, attachmentType?: string | null): string {
    if (fileName && fileName.includes('.')) {
      return fileName.split('.').pop()?.toLowerCase() || 'file';
    }

    if (attachmentType?.includes('pdf')) return 'pdf';
    if (attachmentType?.includes('word')) return 'doc';
    if (attachmentType?.includes('image')) return 'image';
    if (attachmentType?.includes('sheet') || attachmentType?.includes('excel')) return 'xls';
    if (attachmentType?.includes('zip') || attachmentType?.includes('rar')) return 'zip';
    return 'file';
  }

  private needsParentCompletion(reclamation: Reclamation): boolean {
    const text = this.normalize(`${reclamation.title || ''} ${reclamation.description || ''}`);
    const shortDescription = (reclamation.description || '').trim().length < 40;
    const missingTiming = !this.hasTimingDetail(text);
    const missingContext = !this.hasContextDetail(text, reclamation.category || 'AUTRE');
    const missingProof =
      !reclamation.imageName &&
      !reclamation.attachmentName &&
      ['TRANSPORT', 'REPAS', 'HYGIENE', 'SECURITE', 'FINANCIER', 'ADMINISTRATIF'].includes(reclamation.category || '');

    return shortDescription || missingTiming || missingContext || missingProof;
  }

  private buildFollowUpMessage(reclamation: Reclamation, level: FollowUpLevel, ageDays: number): string {
    if (level === 'CONFIRM_RESPONSE') {
      return `Bonjour,\n\nMerci pour votre reponse concernant ma reclamation #${reclamation.id} : ${reclamation.title}.\nJe souhaite avoir une precision complementaire afin de bien comprendre la suite donnee a ce dossier.\n\nCordialement.`;
    }

    const status = this.statusLabel(reclamation.status).toLowerCase();
    const ageSentence = ageDays > 0 ? `Elle est ouverte depuis ${ageDays} jour(s).` : "Elle vient d'etre envoyee.";
    const urgencySentence =
      level === 'RECOMMENDED'
        ? 'Comme cette demande est importante pour le suivi de mon enfant, je souhaite connaitre son avancement.'
        : 'Je souhaite simplement connaitre son avancement lorsque cela sera possible.';

    return `Bonjour,\n\nJe souhaite avoir un suivi concernant ma reclamation #${reclamation.id} : ${reclamation.title}.\nSon statut actuel est "${status}". ${ageSentence}\n${urgencySentence}\n\nMerci de me tenir informe(e) de la suite.\n\nCordialement.`;
  }

  private resolveFollowUpConversationId(reclamation: Reclamation) {
    const existingConversationId = this.followUpConversations()[reclamation.id];

    if (existingConversationId) {
      return of(existingConversationId);
    }

    return this.reclamationService.getUsersByRole('ADMIN').pipe(
      switchMap((admins) => {
        const admin = admins[0];

        if (!admin?.id) {
          return throwError(() => new Error('Aucun admin disponible'));
        }

        return this.reclamationService.createConversation(`Relance reclamation #${reclamation.id}`, admin.id, 'ADMIN');
      }),
      tap((conversation) => {
        if (conversation?.id) {
          this.markFollowUpConversation(reclamation.id, conversation.id);
        }
      }),
      switchMap((conversation) => {
        if (!conversation?.id) {
          return throwError(() => new Error('Conversation invalide'));
        }

        return [conversation.id];
      })
    );
  }

  private markFollowUpPrepared(reclamationId: number): void {
    const preparedAt = new Date().toISOString();
    const nextValue = {
      ...this.preparedFollowUps(),
      [reclamationId]: preparedAt
    };

    this.preparedFollowUps.set(nextValue);
    localStorage.setItem(this.followUpStorageKey, JSON.stringify(nextValue));
  }

  private markFollowUpConversation(reclamationId: number, conversationId: number): void {
    const nextValue = {
      ...this.followUpConversations(),
      [reclamationId]: conversationId
    };

    this.followUpConversations.set(nextValue);
    localStorage.setItem(this.followUpConversationStorageKey, JSON.stringify(nextValue));
  }

  private getAgeDays(createdAt?: string | null): number {
    if (!createdAt) {
      return 0;
    }

    const createdTime = new Date(createdAt).getTime();
    if (Number.isNaN(createdTime)) {
      return 0;
    }

    const diff = Date.now() - createdTime;
    return Math.max(0, Math.floor(diff / 86_400_000));
  }

  private loadPreparedFollowUps(): Record<number, string> {
    const raw = localStorage.getItem(this.followUpStorageKey);

    if (!raw) {
      return {};
    }

    try {
      return JSON.parse(raw) as Record<number, string>;
    } catch {
      localStorage.removeItem(this.followUpStorageKey);
      return {};
    }
  }

  private loadFollowUpConversations(): Record<number, number> {
    const raw = localStorage.getItem(this.followUpConversationStorageKey);

    if (!raw) {
      return {};
    }

    try {
      return JSON.parse(raw) as Record<number, number>;
    } catch {
      localStorage.removeItem(this.followUpConversationStorageKey);
      return {};
    }
  }

  private async copyText(value: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value);
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = value;
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }
  }

  private buildCreationAssistant(
    snapshot: CreationFormSnapshot,
    image: File | null,
    attachment: File | null,
    existingReclamations: Reclamation[]
  ): CreationAssistant {
    const text = this.normalize(`${snapshot.title} ${snapshot.description}`);
    const words = text.split(' ').filter((word) => word.length > 2);
    const categoryResult = this.suggestCategory(text);
    const suggestedCategory = categoryResult.category;
    const suggestedPriority = this.suggestPriority(text, suggestedCategory);
    const duplicate = this.findPossibleDuplicate(text, existingReclamations);
    const missingItems = this.getMissingItems(snapshot, suggestedCategory, image, attachment);
    const recommendedAttachments = this.getRecommendedAttachments(suggestedCategory, image, attachment);
    const hasEnoughText = words.length >= 2 || snapshot.title.trim().length >= 5 || snapshot.description.trim().length >= 8;
    const suggestedTitle = this.suggestTitle(snapshot, suggestedCategory);
    const suggestedDescription = this.suggestDescription(snapshot, suggestedCategory, suggestedPriority);
    const suggestionApplied =
      snapshot.category === suggestedCategory &&
      snapshot.priority === suggestedPriority &&
      snapshot.title.trim().length > 0 &&
      snapshot.description.trim().length > 0;
    const strengths: string[] = [];

    if (snapshot.title.trim().length >= 8) strengths.push('Titre clair');
    if (snapshot.description.trim().length >= 40) strengths.push('Description exploitable');
    if (this.hasTimingDetail(text)) strengths.push('Moment precise');
    if (image || attachment) strengths.push('Piece jointe presente');
    if (snapshot.category || suggestedCategory !== 'AUTRE') strengths.push('Categorie identifiee');

    let qualityScore = 20;
    qualityScore += snapshot.title.trim().length >= 8 ? 15 : 0;
    qualityScore += snapshot.description.trim().length >= 40 ? 25 : Math.min(15, Math.floor(snapshot.description.trim().length / 3));
    qualityScore += this.hasTimingDetail(text) ? 15 : 0;
    qualityScore += this.hasContextDetail(text, suggestedCategory) ? 15 : 0;
    qualityScore += image || attachment ? 10 : 0;
    qualityScore -= duplicate ? 15 : 0;
    qualityScore = Math.max(0, Math.min(100, qualityScore));

    return {
      suggestedTitle,
      suggestedDescription,
      suggestedCategory,
      suggestedPriority,
      urgencyLabel: this.parentUrgencyLabel(suggestedPriority, text),
      summary: this.buildAssistantSummary(suggestedCategory, suggestedPriority, words.length),
      confidence: categoryResult.confidence,
      detectedSignals: categoryResult.signals,
      nextQuestion: this.nextQuestionForCategory(suggestedCategory, text),
      hasEnoughText,
      suggestionApplied,
      qualityScore,
      strengths,
      missingItems,
      recommendedAttachments,
      duplicate
    };
  }

  private suggestCategory(text: string): { category: ReclamationCategory; confidence: number; signals: string[] } {
    const rules: Array<{ category: ReclamationCategory; keywords: string[] }> = [
      { category: 'TRANSPORT', keywords: ['bus', 'transport', 'chauffeur', 'trajet', 'retard', 'ramassage', 'arrivee'] },
      { category: 'REPAS', keywords: ['repas', 'menu', 'manger', 'dejeuner', 'gouter', 'allergie', 'vomir', 'nourriture'] },
      { category: 'HYGIENE', keywords: ['hygiene', 'sale', 'toilette', 'couche', 'odeur', 'proprete', 'vetement'] },
      { category: 'SECURITE', keywords: ['danger', 'blessure', 'tombe', 'surveillance', 'securite', 'accident', 'risque'] },
      { category: 'COMPORTEMENT', keywords: ['comportement', 'tape', 'pleure', 'dispute', 'violence', 'agressif', 'harcelement'] },
      { category: 'PEDAGOGIQUE', keywords: ['activite', 'apprentissage', 'classe', 'educatif', 'programme', 'sortie'] },
      { category: 'FINANCIER', keywords: ['facture', 'paiement', 'montant', 'argent', 'recu', 'frais'] },
      { category: 'ADMINISTRATIF', keywords: ['document', 'dossier', 'certificat', 'inscription', 'autorisation', 'administratif'] },
      { category: 'PERSONNEL', keywords: ['animatrice', 'personnel', 'directrice', 'responsable', 'equipe'] }
    ];

    const scored = rules
      .map((rule) => {
        const signals = rule.keywords.filter((keyword) => text.includes(keyword));
        return {
          category: rule.category,
          score: signals.length,
          signals
        };
      })
      .sort((a, b) => b.score - a.score);

    const best = scored[0];

    if (!best || best.score === 0) {
      return { category: 'AUTRE', confidence: text.length >= 12 ? 35 : 0, signals: [] };
    }

    return {
      category: best.category,
      confidence: Math.min(95, 50 + best.score * 15),
      signals: best.signals
    };
  }

  private suggestPriority(text: string, category: ReclamationCategory): ReclamationPriority {
    const highKeywords = ['urgent', 'danger', 'accident', 'blessure', 'allergie', 'malaise', 'securite', 'menace', 'grave'];
    const mediumKeywords = ['retard', 'recurrent', 'plusieurs', 'probleme', 'inquiet', 'important', 'erreur'];

    if (highKeywords.some((keyword) => text.includes(keyword)) || category === 'SECURITE') {
      return 'HIGH';
    }

    if (mediumKeywords.some((keyword) => text.includes(keyword)) || category === 'TRANSPORT' || category === 'REPAS') {
      return 'MEDIUM';
    }

    return 'LOW';
  }

  private getMissingItems(
    snapshot: CreationFormSnapshot,
    category: ReclamationCategory,
    image: File | null,
    attachment: File | null
  ): string[] {
    const text = this.normalize(`${snapshot.title} ${snapshot.description}`);
    const missing: string[] = [];

    if (snapshot.title.trim().length < 8) missing.push('Preciser un titre plus parlant');
    if (snapshot.description.trim().length < 40) missing.push('Ajouter plus de details dans la description');
    if (!this.hasTimingDetail(text)) missing.push('Indiquer la date, l heure ou le moment');
    if (!this.hasContextDetail(text, category)) missing.push(this.contextHint(category));
    if (!image && !attachment && this.getRecommendedAttachments(category, image, attachment).length > 0) {
      missing.push('Ajouter une preuve ou un document si disponible');
    }

    return missing;
  }

  private getRecommendedAttachments(category: ReclamationCategory, image: File | null, attachment: File | null): string[] {
    if (image || attachment) return [];

    switch (category) {
      case 'TRANSPORT':
        return ['Capture horaire', 'Photo ou preuve du retard'];
      case 'REPAS':
        return ['Photo du repas', 'Certificat medical en cas d allergie'];
      case 'HYGIENE':
        return ['Photo de la situation', 'Note avec date et lieu'];
      case 'SECURITE':
        return ['Photo de la zone', 'Document medical si blessure'];
      case 'FINANCIER':
        return ['Facture', 'Recu de paiement'];
      case 'ADMINISTRATIF':
        return ['Document concerne', 'Autorisation ou certificat'];
      default:
        return [];
    }
  }

  private findPossibleDuplicate(text: string, existingReclamations: Reclamation[]): Reclamation | undefined {
    if (text.length < 20) return undefined;
    const tokens = new Set(text.split(' ').filter((word) => word.length > 4));

    return existingReclamations.find((rec) => {
      if (rec.status === 'RESOLVED' || rec.status === 'REJECTED') return false;
      const otherTokens = new Set(this.normalize(`${rec.title} ${rec.description}`).split(' ').filter((word) => word.length > 4));
      const common = [...tokens].filter((token) => otherTokens.has(token)).length;
      return common >= 3;
    });
  }

  private hasTimingDetail(text: string): boolean {
    return /\b(\d{1,2}h|\d{1,2}:\d{2}|\d{1,2}\/\d{1,2}|lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche|hier|aujourd|matin|midi|soir)\b/.test(text);
  }

  private hasContextDetail(text: string, category: ReclamationCategory): boolean {
    const contextByCategory: Record<ReclamationCategory, string[]> = {
      TRANSPORT: ['bus', 'chauffeur', 'trajet', 'heure', 'retard', 'adresse'],
      REPAS: ['repas', 'menu', 'aliment', 'allergie', 'gouter', 'dejeuner'],
      COMPORTEMENT: ['enfant', 'groupe', 'classe', 'animatrice', 'incident'],
      HYGIENE: ['toilette', 'couche', 'vetement', 'salle', 'proprete'],
      SECURITE: ['lieu', 'zone', 'danger', 'blessure', 'surveillance', 'accident'],
      PERSONNEL: ['animatrice', 'personnel', 'responsable', 'equipe'],
      FINANCIER: ['facture', 'paiement', 'montant', 'recu'],
      PEDAGOGIQUE: ['activite', 'classe', 'programme', 'sortie'],
      ADMINISTRATIF: ['document', 'dossier', 'autorisation', 'certificat'],
      AUTRE: ['date', 'moment', 'lieu', 'groupe']
    };

    return contextByCategory[category].some((keyword) => text.includes(keyword));
  }

  private contextHint(category: ReclamationCategory): string {
    switch (category) {
      case 'TRANSPORT':
        return 'Ajouter heure, trajet ou chauffeur';
      case 'REPAS':
        return 'Ajouter le repas concerne ou la reaction de l enfant';
      case 'HYGIENE':
        return 'Ajouter le lieu et le moment observes';
      case 'SECURITE':
        return 'Ajouter le lieu du risque et les personnes presentes';
      case 'FINANCIER':
        return 'Ajouter facture, montant ou reference';
      default:
        return 'Ajouter le contexte: enfant, groupe, lieu ou moment';
    }
  }

  private parentUrgencyLabel(priority: ReclamationPriority, text: string): string {
    if (priority === 'HIGH') return 'Urgent';
    if (priority === 'MEDIUM' || text.includes('recurrent')) return 'Important';
    return 'Standard';
  }

  private buildAssistantSummary(category: ReclamationCategory, priority: ReclamationPriority, wordCount: number): string {
    if (wordCount < 5) {
      return 'Commencez a decrire la situation pour recevoir une suggestion automatique.';
    }

    return `Votre demande semble concerner ${this.categoryLabel(category).toLowerCase()} avec une priorite ${this.priorityLabel(priority).toLowerCase()}.`;
  }

  private suggestTitle(snapshot: CreationFormSnapshot, category: ReclamationCategory): string {
    const title = snapshot.title.trim();

    if (title) {
      return title;
    }

    const description = snapshot.description.trim();
    if (description) {
      const firstSentence = description.split(/[.!?]/)[0]?.trim() || description;
      const compact = firstSentence.length > 58 ? `${firstSentence.slice(0, 55).trim()}...` : firstSentence;
      return compact.charAt(0).toUpperCase() + compact.slice(1);
    }

    return `Reclamation ${this.categoryLabel(category).toLowerCase()}`;
  }

  private suggestDescription(snapshot: CreationFormSnapshot, category: ReclamationCategory, priority: ReclamationPriority): string {
    const description = snapshot.description.trim();

    if (description) {
      return description;
    }

    const title = snapshot.title.trim();
    const topic = title || `une situation liee a ${this.categoryLabel(category).toLowerCase()}`;
    const priorityText = this.priorityLabel(priority).toLowerCase();
    const contextSentence = this.descriptionContextSentence(category);

    return `Bonjour, je souhaite signaler ${topic}. ${contextSentence} Cette reclamation est proposee avec une priorite ${priorityText}. Merci de verifier la situation et de me tenir informe(e) de la suite.`;
  }

  private descriptionContextSentence(category: ReclamationCategory): string {
    switch (category) {
      case 'TRANSPORT':
        return 'La situation concerne le transport de mon enfant; je peux ajouter l heure, le trajet ou le chauffeur concerne si necessaire.';
      case 'REPAS':
        return 'La situation concerne le repas ou le menu; je peux preciser le jour, l aliment concerne et la reaction de mon enfant.';
      case 'HYGIENE':
        return 'La situation concerne l hygiene; je peux preciser le lieu, le moment et ajouter une photo si disponible.';
      case 'SECURITE':
        return 'La situation concerne la securite; je peux preciser le lieu, les personnes presentes et le risque observe.';
      case 'FINANCIER':
        return 'La situation concerne un paiement ou une facture; je peux ajouter la reference ou le justificatif.';
      case 'ADMINISTRATIF':
        return 'La situation concerne un document ou un dossier administratif; je peux ajouter les references utiles.';
      case 'PEDAGOGIQUE':
        return 'La situation concerne une activite ou un suivi pedagogique; je peux preciser le groupe et la date.';
      case 'COMPORTEMENT':
        return 'La situation concerne un comportement observe; je peux preciser le contexte, le groupe et le moment.';
      case 'PERSONNEL':
        return 'La situation concerne un echange avec l equipe; je peux preciser la personne ou le service concerne.';
      default:
        return 'Je peux ajouter la date, le lieu, le groupe ou toute piece utile pour faciliter le traitement.';
    }
  }

  private nextQuestionForCategory(category: ReclamationCategory, text: string): string {
    if (!this.hasTimingDetail(text)) {
      return 'A quel moment cela s est-il passe ?';
    }

    switch (category) {
      case 'TRANSPORT':
        return 'Quel trajet, bus ou chauffeur est concerne ?';
      case 'REPAS':
        return 'Quel repas ou aliment est concerne ?';
      case 'HYGIENE':
        return 'Dans quel lieu la situation a-t-elle ete observee ?';
      case 'SECURITE':
        return 'Y a-t-il eu blessure, temoin ou zone dangereuse ?';
      case 'FINANCIER':
        return 'Quelle facture, montant ou reference est concerne ?';
      case 'ADMINISTRATIF':
        return 'Quel document ou dossier est concerne ?';
      default:
        return 'Quel enfant, groupe ou lieu est concerne ?';
    }
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.moderationMessage.set('');
  }

  private normalize(value?: string | null): string {
    return (value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }
}
