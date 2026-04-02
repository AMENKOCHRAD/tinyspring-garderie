import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function trimmedRequiredValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (typeof value !== 'string') {
      return null;
    }

    return value.trim().length === 0 ? { trimmedRequired: true } : null;
  };
}

export function noEdgeSpacesValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (typeof value !== 'string' || value.length === 0) {
      return null;
    }

    return value !== value.trim() ? { edgeSpaces: true } : null;
  };
}
