import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtatSanitaire } from './etat-sanitaire';

describe('EtatSanitaire', () => {
  let component: EtatSanitaire;
  let fixture: ComponentFixture<EtatSanitaire>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtatSanitaire]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EtatSanitaire);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
