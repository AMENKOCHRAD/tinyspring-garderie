import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { NgApexchartsModule } from 'ng-apexcharts';

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

  chartAbsencesStatut!: ChartConfig;
  chartAbsencesType!: ChartConfig;
  chartFormations!: ChartConfig;
  chartAnimatrices!: ChartConfig;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.refreshInterval = setInterval(() => this.loadStats(), 20000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  private getHeaders(): HttpHeaders {
    const credentials = btoa('admin@garderie.com:admin123');
    return new HttpHeaders({ 'Authorization': `Basic ${credentials}` });
  }

  loadStats(): void {
    this.http.get<any>('http://localhost:8081/api/admin/dashboard/stats',
      { headers: this.getHeaders() }).subscribe({
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
      series: [
        this.stats.absencesEnAttente,
        this.stats.absencesApprouvees,
        this.stats.absencesRefusees
      ],
      chart: { type: 'donut', height: 280 },
      labels: ['En attente', 'Approuvées', 'Refusées'],
      colors: ['#f59e0b', '#10b981', '#ef4444'],
      legend: { position: 'bottom' },
      dataLabels: { enabled: true },
      plotOptions: { pie: { donut: { size: '65%' } } }
    };

    this.chartAbsencesType = {
      series: [
        this.stats.absences,
        this.stats.congesAnnuels,
        this.stats.congesMaladie,
        this.stats.congesMaternite
      ],
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
      series: [{
        name: 'Formations',
        data: [
          this.stats.formationsInscrites,
          this.stats.formationsEnCours,
          this.stats.formationsTerminees
        ]
      }],
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