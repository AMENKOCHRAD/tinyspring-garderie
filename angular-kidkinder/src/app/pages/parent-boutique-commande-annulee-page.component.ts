import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-parent-boutique-commande-annulee-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="ts-container workspace-page">
      <section class="table-card payment-state">
        <div class="payment-state__icon payment-state__icon--error">&#10005;</div>
        <h1>Commande annulee</h1>
        <p class="muted">Votre commande a ete annulee avec succes.</p>
        <p class="muted">Aucun montant ne vous sera debite.</p>
        <div class="payment-state__actions">
          <a routerLink="/parent/boutique" class="ts-button ts-button--primary">Retour a la boutique</a>
        </div>
      </section>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .workspace-page {
      min-height: calc(100vh - 220px);
      display: grid;
      place-items: center;
      padding-block: 32px;
    }

    .payment-state {
      display: grid;
      justify-items: center;
      align-content: center;
      gap: 16px;
      width: min(100%, 640px);
      padding: 56px 32px;
      text-align: center;
    }

    .payment-state__icon {
      width: 96px;
      height: 96px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-size: 2.2rem;
      font-weight: 700;
      animation: payment-pop 0.5s ease both;
    }

    .payment-state__icon--error {
      background: rgba(220, 38, 38, 0.14);
      color: #dc2626;
    }

    .payment-state__actions {
      display: flex;
      gap: 14px;
      flex-wrap: wrap;
      justify-content: center;
    }

    .muted {
      max-width: 42ch;
    }

    @keyframes payment-pop {
      from {
        opacity: 0;
        transform: scale(0);
      }

      to {
        opacity: 1;
        transform: scale(1);
      }
    }

    @media (max-width: 640px) {
      .workspace-page {
        min-height: calc(100vh - 180px);
        padding-block: 20px;
      }

      .payment-state {
        padding: 40px 20px;
      }

      .payment-state__actions {
        width: 100%;
      }

      .payment-state__actions .ts-button {
        width: 100%;
        justify-content: center;
      }
    }
  `]
})
export class ParentBoutiqueCommandeAnnuleePageComponent {}
