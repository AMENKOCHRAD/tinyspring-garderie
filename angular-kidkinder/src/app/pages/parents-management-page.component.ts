import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { parentsColumns, parentsRows } from '../shared/site-data';

@Component({
  selector: 'app-parents-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des parents</h3>
        <p class="text-muted mb-0">Liste des comptes parents du systeme.</p>
      </div>
      <button class="btn btn-primary" type="button">Ajouter un parent</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class ParentsManagementPageComponent {
  protected readonly columns = parentsColumns;
  protected readonly rows = parentsRows;
}
