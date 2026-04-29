import { TestBed } from '@angular/core/testing';

import { Enfant } from './enfant';

describe('Enfant', () => {
  let service: Enfant;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Enfant);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
