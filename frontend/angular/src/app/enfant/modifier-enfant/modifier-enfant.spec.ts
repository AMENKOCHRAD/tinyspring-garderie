import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModifierEnfant } from './modifier-enfant';

describe('ModifierEnfant', () => {
  let component: ModifierEnfant;
  let fixture: ComponentFixture<ModifierEnfant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModifierEnfant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ModifierEnfant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
