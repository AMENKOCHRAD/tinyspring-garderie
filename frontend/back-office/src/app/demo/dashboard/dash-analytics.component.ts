import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { NgApexchartsModule } from 'ng-apexcharts';

@Component({
  selector: 'app-dash-analytics',
  standalone: true,
  imports: [SharedModule, CommonModule, NgApexchartsModule],
  templateUrl: './dash-analytics.component.html',
  styleUrls: ['./dash-analytics.component.scss']
})
export class DashAnalyticsComponent implements OnInit {
  stats: any = null;
  loading: boolean = true;

  // Chart configs
  public chartOptionsLanguage: any;
  public chartOptionsOccupancy: any;
  public chartOptionsSalles: any;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<any>('http://localhost:8081/api/dashboard/kpis').subscribe({
      next: (data) => {
        this.stats = data;
        this.buildCharts();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur KPI', err);
        this.loading = false;
      }
    });
  }

  buildCharts() {
    // Taux d'occupation (Radial)
    this.chartOptionsOccupancy = {
      series: [this.stats.globalOccupancyRate],
      chart: { height: 350, type: 'radialBar' },
      plotOptions: {
        radialBar: {
          hollow: { size: '60%' },
          dataLabels: {
            value: { fontSize: '24px', fontWeight: 'bold' }
          }
        }
      },
      labels: ['Taux d\'Occupation'],
      colors: ['#28a745'] // Vert success
    };

    if (this.stats.globalOccupancyRate >= 80) this.chartOptionsOccupancy.colors = ['#dc3545'];
    else if (this.stats.globalOccupancyRate >= 50) this.chartOptionsOccupancy.colors = ['#ffc107'];

    // Répartition Langues (Donut)
    const langLabels = Object.keys(this.stats.repartitionsLangues || {});
    const langData = Object.values(this.stats.repartitionsLangues || {}) as number[];
    
    this.chartOptionsLanguage = {
      series: langData.length > 0 ? langData : [1],
      chart: { type: 'donut', height: 350 },
      labels: langLabels.length > 0 ? langLabels : ['Aucune Donnée'],
      colors: ['#0dcaf0', '#6610f2', '#fd7e14', '#20c997'],
      responsive: [{ breakpoint: 480, options: { chart: { width: 200 }, legend: { position: 'bottom' } } }]
    };

    // Enfants par Salle (Bar)
    const salleLabels = Object.keys(this.stats.enfantsParSalle || {});
    const salleData = Object.values(this.stats.enfantsParSalle || {}) as number[];

    this.chartOptionsSalles = {
      series: [{ name: 'Enfants assignés', data: salleData.length > 0 ? salleData : [0] }],
      chart: { type: 'bar', height: 350 },
      plotOptions: { bar: { borderRadius: 4, horizontal: true } },
      dataLabels: { enabled: false },
      xaxis: { categories: salleLabels.length > 0 ? salleLabels : ['N/A'] },
      colors: ['#4680ff']
    };
  }
}
