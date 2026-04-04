import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, input, output } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';

import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Event, EventType } from '../../models/event.model';
import { EventRequest } from '../../models/event-request.model';

@Component({
  selector: 'app-event-form',
  imports: [CommonModule, ReactiveFormsModule, SharedModule],
  templateUrl: './event-form.component.html',
  styleUrls: ['./event-form.component.scss']
})
export class EventFormComponent {
  private readonly fb = inject(FormBuilder);

  readonly submitting = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly submitLabel = input('Enregistrer');
  readonly initialEvent = input<Event | null>(null);
  readonly submitted = output<EventRequest>();
  readonly cancelled = output<void>();

  validationMessage: string | null = null;

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
      maxCapacity: [1, [Validators.required, Validators.min(1)]],
      requiresAuthorization: [false],
      classroomId: [null as number | null, [Validators.required, Validators.min(1)]],
      createdBy: [null as number | null, [Validators.required, Validators.min(1)]],
      eventPrice: [0, [Validators.required, Validators.min(0)]],
      photoPrice: [0, [Validators.required, Validators.min(0)]],
      photoEvent: ['', [Validators.maxLength(2048)]]
    },
    { validators: this.dateRangeValidator() }
  );

  readonly photoPreview = computed(() => {
    const photoValue = this.form.get('photoEvent')?.value;
    return typeof photoValue === 'string' ? photoValue.trim() : '';
  });

  readonly isDateRangeInvalid = computed(
    () =>
      this.form.hasError('invalidDateRange') &&
      (this.form.get('endDatetime')?.touched || this.form.get('startDatetime')?.touched)
  );

  constructor() {
    effect(() => {
      const event = this.initialEvent();

      if (!event) {
        this.form.reset({
          title: '',
          description: '',
          type: '',
          startDatetime: '',
          endDatetime: '',
          location: '',
          maxCapacity: 1,
          requiresAuthorization: false,
          classroomId: null,
          createdBy: null,
          eventPrice: 0,
          photoPrice: 0,
          photoEvent: ''
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
        maxCapacity: event.maxCapacity,
        requiresAuthorization: event.requiresAuthorization,
        classroomId: event.classroomId,
        createdBy: event.createdBy,
        eventPrice: event.eventPrice,
        photoPrice: event.photoPrice,
        photoEvent: event.photoEvent ?? ''
      });
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.form.updateValueAndValidity();
      this.validationMessage = null;
    });
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

    this.submitted.emit({
      title: (rawValue.title ?? '').trim(),
      description: (rawValue.description ?? '').trim(),
      type: rawValue.type as EventType,
      startDatetime: rawValue.startDatetime ?? '',
      endDatetime: rawValue.endDatetime ?? '',
      location: (rawValue.location ?? '').trim(),
      maxCapacity: Number(rawValue.maxCapacity),
      requiresAuthorization: Boolean(rawValue.requiresAuthorization),
      classroomId: Number(rawValue.classroomId),
      createdBy: Number(rawValue.createdBy),
      eventPrice: Number(rawValue.eventPrice),
      photoPrice: Number(rawValue.photoPrice),
      photoEvent: (rawValue.photoEvent ?? '').trim() || undefined
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
}
