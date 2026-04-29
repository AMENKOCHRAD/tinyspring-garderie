import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ToastService } from '../shared/toast.service';

@Component({
  selector: 'app-toast-outlet',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="toast-stack" aria-live="polite" aria-atomic="true">
      <article
        *ngFor="let toast of toastService.toasts()"
        class="toast-card"
        [class.toast-card--success]="toast.tone === 'success'"
        [class.toast-card--info]="toast.tone === 'info'"
        [class.toast-card--error]="toast.tone === 'error'">
        <div class="toast-card__content">
          <span class="toast-card__icon">{{ toast.icon }}</span>
          <span>{{ toast.message }}</span>
        </div>
        <button type="button" class="toast-card__close" (click)="toastService.dismiss(toast.id)" aria-label="Fermer">
          ×
        </button>
        <span class="toast-card__progress"></span>
      </article>
    </section>
  `,
  styleUrl: './toast-outlet.component.css'
})
export class ToastOutletComponent {
  protected readonly toastService = inject(ToastService);
}
