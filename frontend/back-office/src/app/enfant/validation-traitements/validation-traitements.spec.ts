import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidationTraitements } from './validation-traitements';

describe('ValidationTraitements', () => {
  let component: ValidationTraitements;
  let fixture: ComponentFixture<ValidationTraitements>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ValidationTraitements]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ValidationTraitements);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
