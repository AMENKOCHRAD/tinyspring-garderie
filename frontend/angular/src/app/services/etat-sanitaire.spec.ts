import { TestBed } from '@angular/core/testing';

import { EtatSanitaire } from './etat-sanitaire';

describe('EtatSanitaire', () => {
  let service: EtatSanitaire;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EtatSanitaire);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
