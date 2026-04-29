import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionEnfants } from './gestion-enfants';

describe('GestionEnfants', () => {
  let component: GestionEnfants;
  let fixture: ComponentFixture<GestionEnfants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionEnfants]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionEnfants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
