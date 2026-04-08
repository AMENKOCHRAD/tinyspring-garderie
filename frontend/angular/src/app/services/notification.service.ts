import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router, NavigationEnd } from '@angular/router';
import { filter, Subject } from 'rxjs';
import { Commande } from 'src/app/models/boutique/commande.model';
import { Produit } from 'src/app/models/boutique/produit.model';

export interface OrderToast {
  id: number;
  clientNom: string;
  montant: number;
  time: Date;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly adminUrl = 'http://localhost:8081/api/admin/boutique/commandes';
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private previousCount = 0;
  private isInitialized = false;

  unreadCount = signal(0);
  toasts = signal<OrderToast[]>([]);
  /** Émis à chaque nouvelle commande détectée — pour s'abonner sans signals */
  newOrderArrived$ = new Subject<void>();

  private readonly adminProduitUrl = 'http://localhost:8081/api/admin/boutique/produits';
  lowStockProduits = signal<Produit[]>([]);
  lowStockCount = signal(0);

  constructor() {
    // Recalibre le baseline ET remet le badge à 0 quand l'admin visite la page commandes
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe((e) => {
      if (e.url === '/admin/boutique/commandes') {
        this.http.get<Commande[]>(this.adminUrl).subscribe({
          next: (commandes) => {
            this.previousCount = commandes.length;
            this.isInitialized = true;
            localStorage.setItem('notif_prev_count', String(this.previousCount));
            this.unreadCount.set(0);
            console.log('[Notif] Baseline recalibré après visite commandes :', this.previousCount);
          }
        });
      }
    });
  }

  startPolling(): void {
    console.log('[Notif] startPolling() appelé');
    if (this.intervalId) return; // déjà actif

    // NE PAS restaurer depuis localStorage : le premier poll() posera
    // le baseline correct en toutes circonstances (isInitialized = false)
    this.poll();
    this.pollStock();
    this.intervalId = setInterval(() => {
      this.poll();
      this.pollStock();
    }, 5_000);
  }

  stopPolling(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.previousCount = 0;
    this.isInitialized = false;
    this.unreadCount.set(0);
    this.toasts.set([]);
    this.lowStockProduits.set([]);
    this.lowStockCount.set(0);
    localStorage.removeItem('notif_prev_count');
  }

  removeToast(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private poll(): void {
    console.log('[Notif] poll() exécuté, previousCount =', this.previousCount);

    this.http.get<Commande[]>(this.adminUrl).subscribe({
      next: (commandes) => {
        const count = commandes.length;
        console.log('[Notif] commandes reçues :', count, '| isInitialized :', this.isInitialized);

        if (!this.isInitialized) {
          // Premier appel : pose le baseline sans notifier
          this.isInitialized = true;
          this.previousCount = count;
          localStorage.setItem('notif_prev_count', String(count));
          return;
        }

        if (count > this.previousCount) {
          const diff = count - this.previousCount;
          console.log('[Notif] NOUVELLE COMMANDE DÉTECTÉE ! diff =', diff);
          // Les nouvelles commandes sont les dernières dans la liste
          const newCommandes = commandes.slice(commandes.length - diff);

          newCommandes.forEach(cmd => {
            this.addToast({
              id: Date.now() + Math.random(),
              clientNom: cmd.userNom || cmd.userEmail,
              montant: cmd.montantTotal,
              time: new Date()
            });
          });

          this.unreadCount.update(n => n + diff);
          this.newOrderArrived$.next();

          if (!document.hidden) {
            this.playSound();
          }
        }

        // Mise à jour silencieuse (que count augmente ou diminue)
        this.previousCount = count;
        localStorage.setItem('notif_prev_count', String(count));
      },
      error: (err) => {
        console.error('[Notif] Erreur HTTP poll() :', err.status, err.message);
      }
    });
  }

  private addToast(toast: OrderToast): void {
    this.toasts.update(list => {
      const updated = [...list, toast];
      // FIFO : supprime le plus ancien si > 3
      return updated.length > 3 ? updated.slice(1) : updated;
    });
    // Auto-disparition après 5s
    setTimeout(() => this.removeToast(toast.id), 5000);
  }

  private playSound(): void {
    try {
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(440, ctx.currentTime + 0.15);
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    } catch {
      // AudioContext peut être bloqué par la politique du navigateur
    }
  }

  private pollStock(): void {
    this.http.get<Produit[]>(`${this.adminProduitUrl}/low-stock`)
      .subscribe({
        next: (produits) => {
          this.lowStockProduits.set(produits);
          this.lowStockCount.set(produits.length);
        },
        error: () => {}
      });
  }
}
