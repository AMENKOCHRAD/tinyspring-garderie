import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { DataTableComponent } from '../components/data-table.component';
import { transportColumns, transportRows } from '../shared/site-data';

@Component({
  selector: 'app-transport-management-page',
  standalone: true,
  imports: [CommonModule, DataTableComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <div>
        <h3 class="mb-1">Gestion du transport</h3>
        <p class="text-muted mb-0">Les demandes parent sont d abord creees en attente, puis l admin les accepte ou les refuse.</p>
      </div>
      <button class="btn btn-primary" type="button">Nouvelle affectation</button>
    </div>

    <app-data-table [columns]="columns" [rows]="rows"></app-data-table>

    <div class="row mt-4">
      <div class="col-lg-6 mb-4">
        <div class="bg-white rounded shadow-sm p-4 h-100">
          <h5>Workflow metier</h5>
          <p class="mb-2">1. Le parent saisit une demande de transport pour son enfant avec trajet et horaire.</p>
          <p class="mb-2">2. La demande est enregistree avec le statut <strong>En attente</strong>.</p>
          <p class="mb-2">3. L admin consulte la demande puis l accepte ou la refuse.</p>
          <p class="mb-0">4. Si elle est acceptee, le systeme cree automatiquement l affectation entre l enfant, le trajet et le transport.</p>
        </div>
      </div>
      <div class="col-lg-6 mb-4">
        <div class="bg-white rounded shadow-sm p-4 h-100">
          <h5>Actions admin</h5>
          <div class="d-flex flex-wrap">
            <button class="btn btn-success mr-2 mb-2" type="button">Accepter</button>
            <button class="btn btn-danger mr-2 mb-2" type="button">Refuser</button>
            <button class="btn btn-outline-primary mb-2" type="button">Creer l affectation</button>
          </div>
          <p class="mb-0 text-muted">Cette vue est prete a etre reliee a la logique backend TinySpring.</p>
        </div>
      </div>
    </div>
  `
})
export class TransportManagementPageComponent {
  protected readonly columns = transportColumns;
  protected readonly rows = transportRows;
}
