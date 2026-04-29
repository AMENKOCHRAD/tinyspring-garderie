import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { classesColumns, classesRows } from '../shared/site-data';

@Component({
  selector: 'app-classes-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des classes</h3>
        <p class="text-muted mb-0">Capacites, horaires et repartition des classes.</p>
      </div>
      <button class="btn btn-primary" type="button">Ajouter une classe</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class ClassesManagementPageComponent {
  protected readonly columns = classesColumns;
  protected readonly rows = classesRows;
}
