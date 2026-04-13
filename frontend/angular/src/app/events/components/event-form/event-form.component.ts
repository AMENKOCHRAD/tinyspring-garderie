import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {
  AfterViewInit,
  Component,
  effect,
  inject,
  input,
  OnDestroy,
  output
} from '@angular/core';
import * as L from 'leaflet';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';
import { Event as EventModel, EventStatus, EventType } from '../../models/event.model';
import { EventFormSubmission } from '../../models/event-form-submission.model';
import { EventRequest } from '../../models/event-request.model';
import { ClassroomOption } from '../../models/classroom-option.model';
import { ClassroomService } from '../../services/classroom.service';
import { getSafeEventPhotoUrl } from '../../utils/photo-url.util';

type EventFormStep = 1 | 2 | 3;

interface EventTypeOption {
  value: EventType;
  label: string;
  emoji: string;
}

interface EventStatusOption {
  value: EventStatus;
  label: string;
}

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SharedModule],
  templateUrl: './event-form.component.html',
  styleUrls: ['./event-form.component.scss']
})
export class EventFormComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly classroomService = inject(ClassroomService);

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private objectPreviewUrl: string | null = null;

  readonly submitting = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly submitLabel = input('Enregistrer');
  readonly initialEvent = input<EventModel | null>(null);
  readonly submitted = output<EventFormSubmission>();
  readonly cancelled = output<void>();

  readonly stepLabels: Array<{ step: EventFormStep; label: string }> = [
    { step: 1, label: 'Informations' },
    { step: 2, label: 'Participants' },
    { step: 3, label: 'Récapitulatif' }
  ];

  readonly eventTypeOptions: EventTypeOption[] = [
    { value: 'SORTIE', label: 'Sortie', emoji: '🚌' },
    { value: 'FETE', label: 'Fête', emoji: '🎉' },
    { value: 'ATELIER', label: 'Atelier', emoji: '🧠' },
    { value: 'REUNION_PARENTS', label: 'Réunion', emoji: '👨‍👩‍👧' },
    { value: 'ACTIVITE', label: 'Activité', emoji: '⭐' }
  ];

  readonly statusOptions: EventStatusOption[] = [
    { value: 'DRAFT', label: 'Brouillon' },
    { value: 'PUBLISHED', label: 'Publié' }
  ];

  readonly form = this.fb.group(
    {
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(5000)]],
      type: ['', [Validators.required]],
      status: ['DRAFT' as EventStatus, [Validators.required]],
      startDatetime: ['', [Validators.required]],
      endDatetime: ['', [Validators.required]],
      location: ['', [Validators.maxLength(255)]],
      latitude: [null as number | null],
      longitude: [null as number | null],
      maxCapacity: [30, [Validators.required, Validators.min(1)]],
      requiresAuthorization: [false],
      targetClassroomIds: [[] as number[], [this.requiredArrayValidator()]],
      classroomId: [null as number | null],
      createdBy: [this.currentUserId],
      eventPrice: [0, [Validators.required, Validators.min(0)]]
    },
    { validators: this.dateRangeValidator() }
  );

  currentStep: EventFormStep = 1;
  classesLoading = false;
  classroomLoadError: string | null = null;
  classrooms: ClassroomOption[] = [];
  validationMessage: string | null = null;
  selectedPhotoFile: File | null = null;
  selectedPhotoFileName = '';
  locationSearch = '';
  searching = false;
  searchResults: Array<{ display_name: string; lat: string; lon: string }> = [];

  get existingPhotoUrl(): string | null {
    return getSafeEventPhotoUrl(this.initialEvent()?.photoEvent);
  }

  get currentPhotoPreview(): string | null {
    return this.objectPreviewUrl || this.existingPhotoUrl;
  }

  get displayedFileName(): string {
    if (this.selectedPhotoFileName) {
      return this.selectedPhotoFileName;
    }

    const currentEvent = this.initialEvent();
    if (!currentEvent?.photoEvent) {
      return '';
    }

    const safeUrl = getSafeEventPhotoUrl(currentEvent.photoEvent);
    return safeUrl?.split('/').pop() ?? '';
  }

  get isDateRangeInvalid(): boolean {
    return Boolean(
      this.form.hasError('invalidDateRange') &&
        (this.form.get('endDatetime')?.touched || this.form.get('startDatetime')?.touched)
    );
  }

  get selectedClassroomIds(): number[] {
    return this.form.get('targetClassroomIds')?.value ?? [];
  }

  get selectedClassroomLabels(): string[] {
    return this.classrooms
      .filter((classroom) => this.selectedClassroomIds.includes(classroom.id))
      .map((classroom) => classroom.niveau || classroom.nom);
  }

  get currentTypeOption(): EventTypeOption | undefined {
    return this.eventTypeOptions.find((option) => option.value === this.form.get('type')?.value);
  }

  constructor() {
    this.loadClassrooms();

    effect(() => {
      const event = this.initialEvent();
      this.clearSelectedPhoto(false);

      if (!event) {
        this.currentStep = 1;
        this.locationSearch = '';
        this.form.reset({
          title: '',
          description: '',
          type: '',
          status: 'DRAFT',
          startDatetime: '',
          endDatetime: '',
          location: '',
          latitude: null,
          longitude: null,
          maxCapacity: 30,
          requiresAuthorization: false,
          targetClassroomIds: [],
          classroomId: null,
          createdBy: this.currentUserId,
          eventPrice: 0
        });
        this.form.markAsPristine();
        this.form.markAsUntouched();
        this.validationMessage = null;
        return;
      }

      const targetClassroomIds =
        event.targetClassroomIds && event.targetClassroomIds.length > 0
          ? event.targetClassroomIds
          : event.classroomId
            ? [event.classroomId]
            : [];

      this.currentStep = 1;
      this.locationSearch = event.location;
      this.form.reset({
        title: event.title,
        description: event.description,
        type: event.type,
        status: event.status || 'DRAFT',
        startDatetime: this.toDateTimeLocalValue(event.startDatetime),
        endDatetime: this.toDateTimeLocalValue(event.endDatetime),
        location: event.location,
        latitude: event.latitude ?? null,
        longitude: event.longitude ?? null,
        maxCapacity: event.maxCapacity,
        requiresAuthorization: event.requiresAuthorization,
        targetClassroomIds,
        classroomId: event.classroomId,
        createdBy: event.createdBy || this.currentUserId,
        eventPrice: event.eventPrice
      });
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.form.updateValueAndValidity();
      this.validationMessage = null;
    });
  }

  ngAfterViewInit(): void {
    this.tryInitMapForCurrentStep();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }

    if (this.objectPreviewUrl) {
      URL.revokeObjectURL(this.objectPreviewUrl);
      this.objectPreviewUrl = null;
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.item(0) ?? null;

    this.clearSelectedPhoto(false);

    if (!file) {
      return;
    }

    this.selectedPhotoFile = file;
    this.selectedPhotoFileName = file.name;
    this.objectPreviewUrl = URL.createObjectURL(file);
  }

  removeSelectedPhoto(input?: HTMLInputElement | null): void {
    this.clearSelectedPhoto(true);

    if (input) {
      input.value = '';
    }
  }

  selectEventType(type: EventType): void {
    this.form.patchValue({ type });
    this.form.get('type')?.markAsTouched();
  }

  toggleClassroomSelection(classroomId: number): void {
    const current = [...this.selectedClassroomIds];
    const index = current.indexOf(classroomId);

    if (index >= 0) {
      current.splice(index, 1);
    } else {
      current.push(classroomId);
    }

    this.updateSelectedClassrooms(current);
    this.form.get('targetClassroomIds')?.markAsTouched();
  }

  isClassroomSelected(classroomId: number): boolean {
    return this.selectedClassroomIds.includes(classroomId);
  }

  goToStep(step: EventFormStep): void {
    if (step === this.currentStep) {
      return;
    }

    if (step > this.currentStep && !this.validateStep(this.currentStep)) {
      return;
    }

    this.currentStep = step;
    this.validationMessage = null;
    this.tryInitMapForCurrentStep();
  }

  nextStep(): void {
    if (!this.validateStep(this.currentStep)) {
      return;
    }

    if (this.currentStep < 3) {
      this.currentStep = (this.currentStep + 1) as EventFormStep;
      this.validationMessage = null;
      this.tryInitMapForCurrentStep();
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep = (this.currentStep - 1) as EventFormStep;
      this.validationMessage = null;
      this.tryInitMapForCurrentStep();
    }
  }

  submit(): void {
    const createdBy = this.form.get('createdBy')?.value ?? this.currentUserId;
    this.form.patchValue({ createdBy });

    if (this.form.invalid || !this.validateStep(1) || !this.validateStep(2)) {
      this.validationMessage = 'Veuillez compléter les informations obligatoires avant de continuer.';
      this.currentStep = this.firstInvalidStep();
      return;
    }

    if (!createdBy) {
      this.validationMessage =
        "Impossible d'identifier l'utilisateur connecté. Reconnectez-vous avant de créer l'événement.";
      return;
    }

    const rawValue = this.form.getRawValue();
    const currentEvent = this.initialEvent();
    const selectedClassrooms = rawValue.targetClassroomIds ?? [];
    const primaryClassroomId = selectedClassrooms[0] ?? rawValue.classroomId ?? null;

    if (!primaryClassroomId) {
      this.validationMessage = 'Veuillez sélectionner au moins une classe cible.';
      this.currentStep = 2;
      return;
    }

    this.validationMessage = null;

    const payload: EventRequest = {
      title: (rawValue.title ?? '').trim(),
      description: (rawValue.description ?? '').trim(),
      type: rawValue.type as EventType,
      status: rawValue.status as EventStatus,
      startDatetime: rawValue.startDatetime ?? '',
      endDatetime: rawValue.endDatetime ?? '',
      location: (rawValue.location ?? '').trim(),
      latitude: rawValue.latitude ?? null,
      longitude: rawValue.longitude ?? null,
      maxCapacity: Number(rawValue.maxCapacity),
      requiresAuthorization: Boolean(rawValue.requiresAuthorization),
      classroomId: Number(primaryClassroomId),
      targetClassroomIds: selectedClassrooms,
      createdBy: Number(createdBy),
      eventPrice: Number(rawValue.eventPrice),
      photoEvent: currentEvent?.photoEvent
    };

    this.submitted.emit({
      payload,
      photoFile: this.selectedPhotoFile
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  getControl(name: string): AbstractControl | null {
    return this.form.get(name);
  }

  hasError(name: string, errorCode: string): boolean {
    const control = this.getControl(name);
    return Boolean(control?.hasError(errorCode) && (control.dirty || control.touched));
  }

  formatDateTimeSummary(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  searchLocation(): void {
    const query = this.locationSearch.trim();

    if (!query) {
      this.searchResults = [];
      return;
    }

    this.searching = true;

    const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(query)}&limit=5`;

    this.http.get<Array<{ display_name: string; lat: string; lon: string }>>(url).subscribe({
      next: (results) => {
        this.searchResults = results ?? [];
        this.searching = false;
      },
      error: () => {
        this.searchResults = [];
        this.searching = false;
      }
    });
  }

  selectSearchResult(result: { display_name: string; lat: string; lon: string }): void {
    const lat = Number(result.lat);
    const lng = Number(result.lon);

    if (Number.isNaN(lat) || Number.isNaN(lng)) {
      return;
    }

    this.setMarker(lat, lng);

    this.form.patchValue({
      location: result.display_name,
      latitude: lat,
      longitude: lng
    });

    this.locationSearch = result.display_name;
    this.searchResults = [];
  }

  clearSearchResults(): void {
    this.searchResults = [];
  }

  private get currentUserId(): number | null {
    return this.authService.getUser()?.id ?? null;
  }

  private loadClassrooms(): void {
    this.classesLoading = true;
    this.classroomLoadError = null;

    this.classroomService.getAll().subscribe({
      next: (classrooms) => {
        this.classrooms = classrooms;
        this.classesLoading = false;
      },
      error: () => {
        this.classrooms = [];
        this.classesLoading = false;
        this.classroomLoadError = "Impossible de charger les classes depuis la base.";
      }
    });
  }

  private requiredArrayValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      return Array.isArray(value) && value.length > 0 ? null : { required: true };
    };
  }

  private dateRangeValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startDatetime = control.get('startDatetime')?.value;
      const endDatetime = control.get('endDatetime')?.value;

      if (!startDatetime || !endDatetime) {
        return null;
      }

      return new Date(endDatetime) > new Date(startDatetime)
        ? null
        : { invalidDateRange: true };
    };
  }

  private validateStep(step: EventFormStep): boolean {
    const stepControls = this.getStepControlNames(step);

    stepControls.forEach((controlName) => this.form.get(controlName)?.markAsTouched());
    this.form.updateValueAndValidity();

    const hasInvalidControl = stepControls.some((controlName) => this.form.get(controlName)?.invalid);

    if (step === 1 && this.form.hasError('invalidDateRange')) {
      this.validationMessage = 'La date de fin doit être postérieure à la date de début.';
      return false;
    }

    if (hasInvalidControl) {
      this.validationMessage = 'Veuillez compléter les champs requis de cette étape.';
      return false;
    }

    this.validationMessage = null;
    return true;
  }

  private firstInvalidStep(): EventFormStep {
    if (!this.validateStepSilent(1)) {
      return 1;
    }

    if (!this.validateStepSilent(2)) {
      return 2;
    }

    if (!this.validateStepSilent(3)) {
      return 3;
    }

    return 3;
  }

  private validateStepSilent(step: EventFormStep): boolean {
    const stepControls = this.getStepControlNames(step);
    const hasInvalidControl = stepControls.some((controlName) => this.form.get(controlName)?.invalid);

    if (step === 1 && this.form.hasError('invalidDateRange')) {
      return false;
    }

    return !hasInvalidControl;
  }

  private getStepControlNames(step: EventFormStep): string[] {
    switch (step) {
      case 1:
        return ['type', 'title', 'startDatetime', 'endDatetime', 'location', 'status', 'eventPrice'];
      case 2:
        return ['targetClassroomIds', 'maxCapacity'];
      case 3:
      default:
        return [];
    }
  }

  private updateSelectedClassrooms(ids: number[]): void {
    const deduplicated = Array.from(new Set(ids));
    const primaryClassroomId = deduplicated[0] ?? null;

    this.form.patchValue({
      targetClassroomIds: deduplicated,
      classroomId: primaryClassroomId
    });
  }

  private toDateTimeLocalValue(value: string): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    const pad = (segment: number) => segment.toString().padStart(2, '0');

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private clearSelectedPhoto(clearFileName: boolean): void {
    this.selectedPhotoFile = null;

    if (clearFileName) {
      this.selectedPhotoFileName = '';
    }

    if (this.objectPreviewUrl) {
      URL.revokeObjectURL(this.objectPreviewUrl);
      this.objectPreviewUrl = null;
    }
  }

  private tryInitMapForCurrentStep(): void {
    if (this.currentStep !== 1) {
      return;
    }

    setTimeout(() => {
      this.initMap();
      this.syncMapMarkerWithForm();
    }, 0);
  }

  private initMap(): void {
    if (this.map) {
      this.map.invalidateSize();
      return;
    }

    const mapContainer = document.getElementById('event-map');
    if (!mapContainer) {
      return;
    }

    this.map = L.map(mapContainer).setView([36.8065, 10.1815], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;

      this.setMarker(lat, lng);

      this.form.patchValue({
        latitude: lat,
        longitude: lng
      });

      this.reverseGeocode(lat, lng);
    });

    this.map.invalidateSize();
  }

  private syncMapMarkerWithForm(): void {
    const latitude = this.form.get('latitude')?.value;
    const longitude = this.form.get('longitude')?.value;

    if (typeof latitude === 'number' && typeof longitude === 'number') {
      this.setMarker(latitude, longitude);
    }
  }

  private setMarker(lat: number, lng: number): void {
    if (!this.map) {
      return;
    }

    if (this.marker) {
      this.marker.setLatLng([lat, lng]);
    } else {
      this.marker = L.marker([lat, lng]).addTo(this.map);
    }

    this.map.setView([lat, lng], 15);
  }

  private reverseGeocode(lat: number, lng: number): void {
    const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`;

    this.http.get<any>(url).subscribe({
      next: (response) => {
        const placeName = response?.display_name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`;

        this.form.patchValue({
          location: placeName
        });
        this.locationSearch = placeName;
      },
      error: () => {
        const fallback = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
        this.form.patchValue({
          location: fallback
        });
        this.locationSearch = fallback;
      }
    });
  }
}
