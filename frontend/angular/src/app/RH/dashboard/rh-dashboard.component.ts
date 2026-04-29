import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NgApexchartsModule } from 'ng-apexcharts';
import { Subscription } from 'rxjs';
import { NotificationService, Notification } from '../../services/RH/notification.service';

interface ChartConfig {
  series: any[];
  chart: {
    type: 'donut' | 'pie' | 'bar' | 'line' | 'area';
    height: number;
    toolbar?: { show: boolean };
  };
  labels?: string[];
  colors?: string[];
  legend?: { position: 'bottom' | 'top' | 'left' | 'right' };
  dataLabels?: { enabled: boolean };
  plotOptions?: any;
  xaxis?: any;
  grid?: any;
}

@Component({
  selector: 'app-rh-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgApexchartsModule],
  templateUrl: './rh-dashboard.component.html',
  styleUrl: './rh-dashboard.component.scss'
})
export class RhDashboardComponent implements OnInit, OnDestroy {

  stats: any = null;
  isLoading = true;
  private refreshInterval: any;

  // ===== NOTIFICATIONS =====
  notifications: Notification[] = [];
  notifCount = 0;
  notifOpen = false;
  private subs: Subscription[] = [];

  // ===== CHARTS =====
  chartAbsencesStatut!: ChartConfig;
  chartAbsencesType!: ChartConfig;
  chartFormations!: ChartConfig;
  chartAnimatrices!: ChartConfig;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.refreshInterval = setInterval(() => this.loadStats(), 20000);
    this.initNotifications();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
    this.subs.forEach(s => s.unsubscribe());
    this.notificationService.disconnectSSE();
  }

  // ===== NOTIFICATIONS =====

  initNotifications(): void {
    this.notificationService.chargerNotifications().subscribe({
      next: (notifs) => {
        this.notificationService.setNotifications(notifs);
      }
    });

    this.subs.push(
      this.notificationService.notifications$.subscribe(notifs => {
        this.notifications = notifs;
        this.cdr.detectChanges();
      })
    );

    this.subs.push(
      this.notificationService.count$.subscribe(count => {
        this.notifCount = count;
        this.cdr.detectChanges();
      })
    );

    this.notificationService.connectSSE();
  }

  toggleNotif(): void {
    this.notifOpen = !this.notifOpen;
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

  supprimerNotif(event: Event, id: number): void {
    event.stopPropagation();
    this.notificationService.supprimer(id).subscribe(() => {
      this.notifications = this.notifications.filter(n => n.id !== id);
      this.notificationService.setNotifications(this.notifications);
    });
  }

  getNotifIcon(type: string): string {
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

  // ===== STATS =====

  // ✅ Supprimé getHeaders() — l'intercepteur JWT gère ça automatiquement

  loadStats(): void {
    this.http.get<any>('http://localhost:8081/api/admin/dashboard/stats').subscribe({
      next: (data) => {
        this.stats = data;
        this.buildCharts();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Erreur dashboard', err);
        this.isLoading = false;
      }
    });
  }

  buildCharts(): void {
    this.chartAbsencesStatut = {
      series: [this.stats.absencesEnAttente, this.stats.absencesApprouvees, this.stats.absencesRefusees],
      chart: { type: 'donut', height: 280 },
      labels: ['En attente', 'Approuvées', 'Refusées'],
      colors: ['#f59e0b', '#10b981', '#ef4444'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: true },
      plotOptions: { pie: { donut: { size: '65%' } } }
    };

    this.chartAbsencesType = {
      series: [this.stats.absences, this.stats.congesAnnuels, this.stats.congesMaladie, this.stats.congesMaternite],
      chart: { type: 'donut', height: 280 },
      labels: ['Absence', 'Congé annuel', 'Congé maladie', 'Congé maternité'],
      colors: ['#6366f1', '#3b82f6', '#06b6d4', '#ec4899'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: true }
    };

    this.chartAnimatrices = {
      series: [this.stats.animatricesActives, this.stats.animatricesInactives],
      chart: { type: 'pie', height: 280 },
      labels: ['Actives', 'Inactives'],
      colors: ['#10b981', '#ef4444'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: true }
    };

    this.chartFormations = {
      series: [{ name: 'Formations', data: [this.stats.formationsInscrites, this.stats.formationsEnCours, this.stats.formationsTerminees] }],
      chart: { type: 'bar', height: 280, toolbar: { show: false } },
      plotOptions: { bar: { borderRadius: 6, columnWidth: '45%' } },
      colors: ['#4f46e5'],
      xaxis: { categories: ['Inscrites', 'En cours', 'Terminées'] },
      dataLabels: { enabled: false },
      grid: { borderColor: '#f1f5f9' }
    };
  }

  getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'ABSENCE': 'Absence',
      'CONGE_ANNUEL': 'Congé annuel',
      'CONGE_MALADIE': 'Congé maladie',
      'CONGE_MATERNITE': 'Congé maternité'
    };
    return labels[type] || type;
  }
}