import { CommonModule } from '@angular/common';
import { AfterViewInit, OnDestroy ,Component, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event as EventModel, EventType } from '../../models/event.model';
import { EventFormSubmission } from '../../models/event-form-submission.model';
import { EventRequest } from '../../models/event-request.model';
import { getSafeEventPhotoUrl } from '../../utils/photo-url.util';

import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';

@Component({
  selector: 'app-event-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule,FormsModule, SharedModule],
  templateUrl: './event-form.component.html',
  styleUrls: ['./event-form.component.scss']
})
export class EventFormComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
   locationSearch = '';
  searching = false;
  searchResults: Array<{
  display_name: string;
  lat: string;
  lon: string;
}> = [];
  readonly submitting = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly submitLabel = input('Enregistrer');
  readonly initialEvent = input<EventModel | null>(null);
  readonly submitted = output<EventFormSubmission>();
  readonly cancelled = output<void>();

  validationMessage: string | null = null;
  selectedPhotoFile: File | null = null;
  selectedPhotoFileName = '';
  private objectPreviewUrl: string | null = null;

  readonly eventTypes: EventType[] = [
    'SORTIE',
    'FETE',
    'ATELIER',
    'REUNION_PARENTS',
    'ACTIVITE'
  ];

  readonly form = this.fb.group(
    {
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(5000)]],
      type: ['', [Validators.required]],
      startDatetime: ['', [Validators.required]],
      endDatetime: ['', [Validators.required]],
      location: ['', [Validators.maxLength(255)]],
      latitude: [null as number | null],
      longitude: [null as number | null],
      maxCapacity: [1, [Validators.required, Validators.min(1)]],
      requiresAuthorization: [false],
      classroomId: [null as number | null, [Validators.required, Validators.min(1)]],
      createdBy: [null as number | null, [Validators.required, Validators.min(1)]],
      eventPrice: [0, [Validators.required, Validators.min(0)]]
    },
    { validators: this.dateRangeValidator() }
  );

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
    if (!safeUrl) {
      return '';
    }

    return safeUrl.split('/').pop() ?? '';
  }

  get isDateRangeInvalid(): boolean {
    return Boolean(
      this.form.hasError('invalidDateRange') &&
        (this.form.get('endDatetime')?.touched || this.form.get('startDatetime')?.touched)
    );
  }

  constructor() {
    effect(() => {
      const event = this.initialEvent();

      this.clearSelectedPhoto(false);

      if (!event) {
        this.form.reset({
          title: '',
          description: '',
          type: '',
          startDatetime: '',
          endDatetime: '',
          location: '',
          latitude: null,
          longitude: null,
          maxCapacity: 1,
          requiresAuthorization: false,
          classroomId: null,
          createdBy: null,
          eventPrice: 0
        });
        this.form.markAsPristine();
        this.form.markAsUntouched();
        this.validationMessage = null;
        return;
      }

      this.form.reset({
        title: event.title,
        description: event.description,
        type: event.type,
        startDatetime: this.toDateTimeLocalValue(event.startDatetime),
        endDatetime: this.toDateTimeLocalValue(event.endDatetime),
        location: event.location,
        latitude: (event as any).latitude ?? null,
        longitude: (event as any).longitude ?? null,
        maxCapacity: event.maxCapacity,
        requiresAuthorization: event.requiresAuthorization,
        classroomId: event.classroomId,
        createdBy: event.createdBy,
        eventPrice: event.eventPrice
      });
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.form.updateValueAndValidity();
      this.validationMessage = null;
    });
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

  submit(): void {
    if (this.form.invalid) {
      this.validationMessage =
        'Veuillez corriger les champs requis avant de continuer.';
      this.form.markAllAsTouched();
      return;
    }

    this.validationMessage = null;
    const rawValue = this.form.getRawValue();
    const currentEvent = this.initialEvent();

    const payload: EventRequest = {
      title: (rawValue.title ?? '').trim(),
      description: (rawValue.description ?? '').trim(),
      type: rawValue.type as EventType,
      startDatetime: rawValue.startDatetime ?? '',
      endDatetime: rawValue.endDatetime ?? '',
      location: (rawValue.location ?? '').trim(),
      latitude: rawValue.latitude ?? null,
      longitude: rawValue.longitude ?? null,
      maxCapacity: Number(rawValue.maxCapacity),
      requiresAuthorization: Boolean(rawValue.requiresAuthorization),
      classroomId: Number(rawValue.classroomId),
      createdBy: Number(rawValue.createdBy),
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

  formatEventType(type: EventType): string {
    return type.replace(/_/g, ' ');
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
  private setMarker(lat: number, lng: number): void {
  if (!this.map) return;

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
      const placeName =
        response?.display_name ||
        `${lat.toFixed(6)}, ${lng.toFixed(6)}`;

      this.form.patchValue({
        location: placeName
      });
    },
    error: () => {
      this.form.patchValue({
        location: `${lat.toFixed(6)}, ${lng.toFixed(6)}`
      });
    }
  });
}
  private initMap(): void {
  if (this.map) return;

  this.map = L.map('event-map').setView([36.8065, 10.1815], 12);

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

  setTimeout(() => {
    this.map?.invalidateSize();
  }, 0);
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
  ngAfterViewInit(): void {
  this.initMap();
}

ngOnDestroy(): void {
  if (this.map) {
    this.map.remove();
    this.map = null;
  }
}
}
