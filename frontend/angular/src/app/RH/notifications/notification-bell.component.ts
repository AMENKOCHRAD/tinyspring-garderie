import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService, Notification } from '../../services/RH/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="notif-bell-wrapper">

      <!-- Cloche -->
      <button class="bell-btn" (click)="toggleDropdown()">
        <span class="bell-icon">🔔</span>
        <span class="badge-count" *ngIf="count > 0">{{ count > 9 ? '9+' : count }}</span>
      </button>

      <!-- Dropdown -->
      <div class="notif-dropdown" *ngIf="isOpen">
        <div class="notif-header">
          <span class="notif-title">Notifications</span>
          <button class="btn-tout-lire" (click)="marquerToutLu()" *ngIf="count > 0">
            Tout lire
          </button>
        </div>

        <div class="notif-list">
          <div
            *ngFor="let notif of notifications"
            class="notif-item"
            [class.non-lue]="!notif.read"
            (click)="marquerLue(notif)">
            <div class="notif-icon">{{ getIcon(notif.type) }}</div>
            <div class="notif-content">
              <p class="notif-message">{{ notif.message }}</p>
              <span class="notif-time">{{ getTemps(notif.createdAt) }}</span>
            </div>
            <button class="btn-supprimer" (click)="supprimer($event, notif.id)">✕</button>
          </div>

          <div class="notif-empty" *ngIf="notifications.length === 0">
            Aucune notification 🎉
          </div>
        </div>
      </div>

      <!-- Overlay pour fermer -->
      <div class="overlay" *ngIf="isOpen" (click)="isOpen = false"></div>
    </div>
  `,
  styles: [`
    .notif-bell-wrapper {
      position: relative;
      display: inline-block;
    }

    .bell-btn {
      background: none;
      border: none;
      cursor: pointer;
      position: relative;
      padding: 8px;
      font-size: 20px;

      .badge-count {
        position: absolute;
        top: 0;
        right: 0;
        background: #ef4444;
        color: white;
        border-radius: 50%;
        width: 18px;
        height: 18px;
        font-size: 11px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        animation: pulse 1.5s infinite;
      }
    }

    @keyframes pulse {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.2); }
    }

    .notif-dropdown {
      position: absolute;
      top: 44px;
      right: 0;
      width: 360px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.15);
      z-index: 9999;
      overflow: hidden;
    }

    .notif-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 16px;
      border-bottom: 1px solid #f1f5f9;

      .notif-title {
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;
      }

      .btn-tout-lire {
        background: none;
        border: none;
        color: #4f46e5;
        font-size: 13px;
        cursor: pointer;
        &:hover { text-decoration: underline; }
      }
    }

    .notif-list {
      max-height: 380px;
      overflow-y: auto;
    }

    .notif-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      border-bottom: 1px solid #f8fafc;
      cursor: pointer;
      transition: background 0.15s;

      &:hover { background: #f8fafc; }

      &.non-lue {
        background: #eff6ff;
        &:hover { background: #dbeafe; }
      }

      .notif-icon { font-size: 20px; }

      .notif-content {
        flex: 1;

        .notif-message {
          font-size: 13px;
          color: #374151;
          margin: 0 0 4px;
          line-height: 1.4;
        }

        .notif-time {
          font-size: 11px;
          color: #94a3b8;
        }
      }

      .btn-supprimer {
        background: none;
        border: none;
        color: #cbd5e0;
        cursor: pointer;
        font-size: 12px;
        padding: 2px 4px;
        &:hover { color: #ef4444; }
      }
    }

    .notif-empty {
      text-align: center;
      padding: 32px;
      color: #94a3b8;
      font-size: 14px;
    }

    .overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 9998;
    }
  `]
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  notifications: Notification[] = [];
  count = 0;
  isOpen = false;
  private subs: Subscription[] = [];

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    // Charger les notifications existantes
    this.notificationService.chargerNotifications().subscribe({
      next: (notifs) => {
        this.notificationService.setNotifications(notifs);
      }
    });

    // S'abonner aux notifications en temps réel
    this.subs.push(
      this.notificationService.notifications$.subscribe(notifs => {
        this.notifications = notifs;
      })
    );

    this.subs.push(
      this.notificationService.count$.subscribe(count => {
        this.count = count;
      })
    );

    // Démarrer SSE
    this.notificationService.connectSSE();
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.notificationService.disconnectSSE();
  }

  toggleDropdown(): void {
    this.isOpen = !this.isOpen;
  }

  marquerLue(notif: Notification): void {
    if (!notif.read) {
      this.notificationService.marquerLue(notif.id).subscribe(() => {
        notif.read = true;
        this.notificationService.setNotifications(this.notifications);
      });
    }
  }

  marquerToutLu(): void {
    this.notificationService.marquerToutLu().subscribe(() => {
      this.notifications.forEach(n => n.read = true);
      this.notificationService.setNotifications(this.notifications);
    });
  }

  supprimer(event: Event, id: number): void {
    event.stopPropagation();
    this.notificationService.supprimer(id).subscribe(() => {
      this.notifications = this.notifications.filter(n => n.id !== id);
      this.notificationService.setNotifications(this.notifications);
    });
  }

  getIcon(type: string): string {
    switch (type) {
      case 'ABSENCE': return '📋';
      case 'ANIMATRICE': return '👤';
      case 'FORMATION': return '📚';
      default: return '🔔';
    }
  }

  getTemps(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60) return 'À l\'instant';
    if (diff < 3600) return `Il y a ${Math.floor(diff / 60)} min`;
    if (diff < 86400) return `Il y a ${Math.floor(diff / 3600)} h`;
    return `Il y a ${Math.floor(diff / 86400)} j`;
  }
}