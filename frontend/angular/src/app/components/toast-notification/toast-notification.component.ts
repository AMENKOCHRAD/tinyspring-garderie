import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { NotificationService } from 'src/app/services/notification.service';

@Component({
  selector: 'app-toast-notification',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <div class="tn-container">
      @for (toast of toasts(); track toast.id) {
        <div class="tn-toast">
          <button class="tn-close" (click)="close(toast.id)">×</button>

          <div class="tn-body">
            <div class="tn-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22"
                   viewBox="0 0 24 24" fill="none" stroke="#dc3545"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                <line x1="3" y1="6" x2="21" y2="6"/>
                <path d="M16 10a4 4 0 0 1-8 0"/>
              </svg>
            </div>
            <div class="tn-content">
              <p class="tn-title">Nouvelle commande&nbsp;!</p>
              <p class="tn-sub">
                <span class="tn-client">{{ truncate(toast.clientNom) }}</span>
                vient de commander
                <span class="tn-amount">{{ toast.montant | number:'1.2-2' }}&nbsp;€</span>
              </p>
              <p class="tn-time">{{ elapsed(toast.time, now()) }}</p>
            </div>
          </div>

          <div class="tn-progress"></div>
        </div>
      }
    </div>
  `,
  styles: [`
    .tn-container {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 10px;
      pointer-events: none;
    }

    .tn-toast {
      position: relative;
      width: 320px;
      background: #fff;
      border-left: 4px solid #dc3545;
      border-radius: 12px;
      padding: 14px 36px 18px 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
      pointer-events: all;
      overflow: hidden;
      animation: tn-slideInRight 0.3s ease-out forwards;
    }

    .tn-close {
      position: absolute;
      top: 8px;
      right: 10px;
      background: none;
      border: none;
      font-size: 18px;
      line-height: 1;
      color: #6c757d;
      cursor: pointer;
      padding: 0;
    }
    .tn-close:hover { color: #343a40; }

    .tn-body {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .tn-icon { flex-shrink: 0; margin-top: 2px; }
    .tn-content { flex: 1; min-width: 0; }

    .tn-title {
      margin: 0 0 3px;
      font-weight: 700;
      font-size: 14px;
      color: #212529;
    }

    .tn-sub {
      margin: 0 0 4px;
      font-size: 13px;
      color: #495057;
      line-height: 1.4;
    }

    .tn-client { font-weight: 600; }

    .tn-amount {
      font-weight: 700;
      color: #1a73e8;
    }

    .tn-time {
      margin: 0;
      font-size: 11px;
      color: #adb5bd;
    }

    .tn-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      width: 100%;
      background: #dc3545;
      border-radius: 0 0 0 8px;
      animation: tn-shrink 5s linear forwards;
    }

    @keyframes tn-slideInRight {
      from { transform: translateX(110%); opacity: 0; }
      to   { transform: translateX(0);    opacity: 1; }
    }

    @keyframes tn-shrink {
      from { width: 100%; }
      to   { width: 0%;   }
    }
  `]
})
export class ToastNotificationComponent {
  private notifService = inject(NotificationService);
  toasts = this.notifService.toasts;

  /** Tick mis à jour chaque seconde pour forcer le recalcul du temps écoulé */
  now = signal(Date.now());

  constructor() {
    setInterval(() => this.now.set(Date.now()), 1000);
  }

  close(id: number): void {
    this.notifService.removeToast(id);
  }

  truncate(name: string): string {
    return name.length > 20 ? name.slice(0, 20) + '…' : name;
  }

  elapsed(time: Date, now: number): string {
    const secs = Math.floor((now - time.getTime()) / 1000);
    if (secs < 2) return 'À l\'instant';
    if (secs < 60) return `Il y a ${secs} sec`;
    return `Il y a ${Math.floor(secs / 60)} min`;
  }
}
