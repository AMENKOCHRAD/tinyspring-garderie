import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { paymentsColumns, paymentsRows } from '../shared/site-data';

@Component({
  selector: 'app-payments-management-page',
  standalone: true,
  imports: [DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion des paiements</h3>
        <p class="text-muted mb-0">Historique et suivi des reglements.</p>
      </div>
      <button class="btn btn-primary" type="button">Enregistrer un paiement</button>
    </div>
    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>
  `
})
export class PaymentsManagementPageComponent {
  protected readonly columns = paymentsColumns;
  protected readonly rows = paymentsRows;
}
