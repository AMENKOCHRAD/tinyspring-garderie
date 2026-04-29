import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { childrenColumns, childrenRows } from '../shared/site-data';

@Component({
  selector: 'app-children-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des enfants</h3>
        <p class="text-muted mb-0">Suivi des enfants inscrits dans les classes.</p>
      </div>
      <button class="btn btn-primary" type="button">Ajouter un enfant</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class ChildrenManagementPageComponent {
  protected readonly columns = childrenColumns;
  protected readonly rows = childrenRows;
}
