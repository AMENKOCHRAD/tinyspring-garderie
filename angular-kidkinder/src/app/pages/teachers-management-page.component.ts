import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { teachersColumns, teachersRows } from '../shared/site-data';

@Component({
  selector: 'app-teachers-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des enseignants</h3>
        <p class="text-muted mb-0">Equipe pedagogique et etat des affectations.</p>
      </div>
      <button class="btn btn-primary" type="button">Ajouter un enseignant</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class TeachersManagementPageComponent {
  protected readonly columns = teachersColumns;
  protected readonly rows = teachersRows;
}
