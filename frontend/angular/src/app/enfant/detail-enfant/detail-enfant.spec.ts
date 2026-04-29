import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetailEnfant } from './detail-enfant';

describe('DetailEnfant', () => {
  let component: DetailEnfant;
  let fixture: ComponentFixture<DetailEnfant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetailEnfant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetailEnfant);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
