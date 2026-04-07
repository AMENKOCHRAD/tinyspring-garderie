import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardProfilsEnfants } from './dashboard-profils-enfants';

describe('DashboardProfilsEnfants', () => {
  let component: DashboardProfilsEnfants;
  let fixture: ComponentFixture<DashboardProfilsEnfants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardProfilsEnfants]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardProfilsEnfants);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
