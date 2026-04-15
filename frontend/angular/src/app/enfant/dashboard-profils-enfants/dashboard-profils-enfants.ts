import { Component, OnInit } from '@angular/core';
import { EnfantService, Enfant } from '../enfant';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard-profils-enfants',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-profils-enfants.html',
  styleUrls: ['./dashboard-profils-enfants.css']
})
export class DashboardProfilsEnfants implements OnInit {
  enfants: Enfant[] = [];
  isLoading = false;
  error = '';

  constructor(private enfantService: EnfantService) {}

  ngOnInit(): void {
    this.loadEnfants();
  }

  loadEnfants(): void {
    this.isLoading = true;
    this.error = '';

    this.enfantService.getAllEnfants().subscribe({
      next: (data: Enfant[]) => {
        this.enfants = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement';
        this.isLoading = false;
      }
    });
  }

  archiverEnfant(id: number): void {
    if (confirm('Archiver cet enfant ?')) {
      this.error = '';

      this.enfantService.archiverEnfant(id).subscribe({
        next: () => {
          this.enfants = this.enfants.filter((e) => e.id !== id);
        },
        error: (err) => {
          console.error(err);
          this.error = 'Erreur lors de l’archivage';
        }
      });
    }
  }
}