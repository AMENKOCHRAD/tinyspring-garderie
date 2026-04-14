import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TableColumn, TableRow } from '../shared/site-data';

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="table-responsive bg-white rounded shadow-sm p-3">
      <table class="table table-hover mb-0">
        <thead class="thead-light">
          <tr>
            <th *ngFor="let column of columns">{{ column.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let row of rows">
            <td *ngFor="let column of columns">{{ row[column.key] }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class DataTableComponent {
  @Input({ required: true }) columns!: TableColumn[];
  @Input({ required: true }) rows!: TableRow[];
}
