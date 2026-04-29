import { TestBed } from '@angular/core/testing';

import { ValidationTraitements } from './validation-traitements';

describe('ValidationTraitements', () => {
  let service: ValidationTraitements;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ValidationTraitements);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
