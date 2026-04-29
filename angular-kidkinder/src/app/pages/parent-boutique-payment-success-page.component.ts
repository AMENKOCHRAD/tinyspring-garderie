import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommandeDto } from '../shared/boutique.models';

@Component({
  selector: 'app-parent-boutique-payment-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './parent-boutique-payment-success-page.component.html',
  styleUrl: './parent-boutique-payment-success-page.component.css'
})
export class ParentBoutiquePaymentSuccessPageComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiUrl = 'http://localhost:8081/api/boutique/commandes/session';
  private verificationTimeoutId: number | null = null;
  private redirectTimeoutId: number | null = null;
  private emailEchecDeclenche = false;

  protected readonly statut = signal<'loading' | 'confirmed' | 'pending' | 'error'>('loading');
  protected readonly sessionId = signal('');
  protected readonly commandeId = signal('');

  ngOnInit(): void {
    this.sessionId.set(this.route.snapshot.queryParamMap.get('session_id') || '');
    this.destroyRef.onDestroy(() => this.clearTimeouts());

    if (!this.sessionId()) {
      this.statut.set('error');
      return;
    }

    this.verificationTimeoutId = window.setTimeout(() => this.verifyPaymentStatus(), 3000);
  }

  private verifyPaymentStatus(): void {
    const currentSessionId = this.sessionId();
    const token = localStorage.getItem('token');

    if (!currentSessionId || !token) {
      this.statut.set('error');
      return;
    }

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    this.http
      .get<CommandeDto>(`${this.apiUrl}/${encodeURIComponent(currentSessionId)}`, { headers })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (commande) => {
          this.commandeId.set(commande.id ? String(commande.id) : '');

          if (commande.statut === 'CONFIRMEE' || commande.paymentStatus === 'PAID') {
            this.statut.set('confirmed');
            this.scheduleRedirect();
            return;
          }

          this.statut.set('error');
          this.declencherEmailEchec();
        },
        error: () => {
          this.statut.set('error');
          this.declencherEmailEchec();
        }
      });
  }

  private scheduleRedirect(): void {
    if (this.redirectTimeoutId !== null) {
      window.clearTimeout(this.redirectTimeoutId);
    }

    this.redirectTimeoutId = window.setTimeout(() => {
      void this.router.navigate(['/parent/boutique/orders']);
    }, 5000);
  }

  private clearTimeouts(): void {
    if (this.verificationTimeoutId !== null) {
      window.clearTimeout(this.verificationTimeoutId);
    }

    if (this.redirectTimeoutId !== null) {
      window.clearTimeout(this.redirectTimeoutId);
    }
  }

  private declencherEmailEchec(): void {
    const sessionId = this.sessionId();

    if (!sessionId || this.emailEchecDeclenche) {
      return;
    }

    const token = localStorage.getItem('token');

    if (!token) {
      return;
    }

    this.emailEchecDeclenche = true;

    void fetch(`http://localhost:8081/api/boutique/commandes/echec-par-session/${encodeURIComponent(sessionId)}`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
      .then(() => console.log('📧 Email échec paiement déclenché'))
      .catch((e) => console.error('Erreur déclenchement email:', e));
  }
}
