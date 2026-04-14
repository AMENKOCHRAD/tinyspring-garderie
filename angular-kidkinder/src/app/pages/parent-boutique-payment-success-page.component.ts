import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-parent-boutique-payment-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-payment-success-page.component.html',
  styleUrl: './parent-boutique-payment-success-page.component.css'
})
export class ParentBoutiquePaymentSuccessPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly sessionId = signal('');

  ngOnInit(): void {
    this.sessionId.set(this.route.snapshot.queryParamMap.get('session_id') || '');

    const timeoutId = window.setTimeout(() => void this.router.navigate(['/parent/boutique/orders']), 5000);
    this.destroyRef.onDestroy(() => window.clearTimeout(timeoutId));
  }
}
