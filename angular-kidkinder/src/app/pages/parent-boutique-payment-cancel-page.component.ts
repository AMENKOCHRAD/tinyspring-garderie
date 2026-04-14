import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-parent-boutique-payment-cancel-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-payment-cancel-page.component.html',
  styleUrl: './parent-boutique-payment-cancel-page.component.css'
})
export class ParentBoutiquePaymentCancelPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  protected readonly commandeId = signal('');

  ngOnInit(): void {
    this.commandeId.set(this.route.snapshot.queryParamMap.get('commande_id') || '');
  }
}
