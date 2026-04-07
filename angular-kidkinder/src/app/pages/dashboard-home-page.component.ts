import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { dashboardStats, paymentsColumns, paymentsRows, registrationsColumns, registrationsRows } from '../shared/site-data';

@Component({
  selector: 'app-dashboard-home-page',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  template: `
    <div class="row mb-4">
      <div class="col-md-6 col-xl-3 mb-3" *ngFor="let stat of stats">
        <div class="rounded text-white p-4 shadow-sm h-100" [ngClass]="stat.colorClass">
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <small class="d-block">{{ stat.label }}</small>
              <h2 class="mb-0">{{ stat.value }}</h2>
            </div>
            <i class="fa fa-2x" [ngClass]="stat.icon"></i>
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <div class="col-lg-6 mb-4">
        <h4 class="mb-3">Dernieres inscriptions</h4>
        <app-data-table [columns]="registrationsColumns" [rows]="registrationsRows"></app-data-table>
      </div>
      <div class="col-lg-6 mb-4">
        <h4 class="mb-3">Derniers paiements</h4>
        <app-data-table [columns]="paymentsColumns" [rows]="paymentsRows"></app-data-table>
      </div>
    </div>
  `
})
export class DashboardHomePageComponent {
  protected readonly stats = dashboardStats;
  protected readonly registrationsColumns = registrationsColumns;
  protected readonly registrationsRows = registrationsRows;
  protected readonly paymentsColumns = paymentsColumns;
  protected readonly paymentsRows = paymentsRows;
}
