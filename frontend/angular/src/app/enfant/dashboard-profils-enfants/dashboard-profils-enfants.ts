import { Component, OnInit } from '@angular/core';
import { EnfantService, Enfant } from '../enfant';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-profils-enfants',
  imports: [CommonModule], // ← ajouter ici
  templateUrl: './dashboard-profils-enfants.html',
  styleUrl: './dashboard-profils-enfants.css',
})
export class DashboardProfilsEnfants  implements OnInit {

  enfants: Enfant[] = [];
  isLoading = true;
  error = '';

  constructor(private enfantService: EnfantService) {}

  ngOnInit(): void {
    this.loadEnfants();
  }

  loadEnfants(): void {
    this.enfantService.getAllEnfants().subscribe({
      next: (data) => {
        this.enfants = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement';
        this.isLoading = false;
        console.error(err);
      }
    });
  }

  supprimerEnfant(id: number): void {
    if (confirm('Supprimer cet enfant ?')) {
      this.enfantService.deleteEnfant(id).subscribe(() => {
        this.enfants = this.enfants.filter(e => e.id !== id);
      });
    }
  }
}

