import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { registrationsColumns, registrationsRows } from '../shared/site-data';

@Component({
  selector: 'app-registrations-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des inscriptions</h3>
        <p class="text-muted mb-0">Suivi des demandes et validations.</p>
      </div>
      <button class="btn btn-primary" type="button">Nouvelle inscription</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class RegistrationsManagementPageComponent {
  protected readonly columns = registrationsColumns;
  protected readonly rows = registrationsRows;
}
