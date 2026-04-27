import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Animatrice } from '../animatrice.model';
import { AnimatriceService } from '../../../services/RH/animatrice.service';

@Component({
  selector: 'app-list-animatrice',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DecimalPipe],
  templateUrl: './list-animatrice.component.html',
  styleUrl: './list-animatrice.component.scss'
})
export class ListAnimatriceComponent implements OnInit, OnDestroy {

  animatrices: Animatrice[] = [];
  searchTerm: string = '';
  filterStatut: string = '';

  
  private refreshInterval: any;
  private readonly REFRESH_DELAY_MS = 10000;

  constructor(
    private animatriceService: AnimatriceService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAnimatrices();

    // ✅ Rafraîchissement automatique toutes les 15 secondes
    this.refreshInterval = setInterval(() => {
      this.loadAnimatrices();
    }, this.REFRESH_DELAY_MS);
  }

  ngOnDestroy(): void {
    // ✅ Nettoyage quand on quitte la page
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  loadAnimatrices(): void {
    this.animatriceService.getAllAnimatrices().subscribe({
      next: (data) => {
        this.animatrices = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement animatrices', err)
    });
  }

  getFilteredAnimatrices(): Animatrice[] {
    return this.animatrices.filter(a => {
      const matchSearch = !this.searchTerm ||
        a.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        a.prenom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        a.email.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchStatut = !this.filterStatut || a.statut === this.filterStatut;
      return matchSearch && matchStatut;
    });
  }

  getActiveCount(): number {
    return this.animatrices.filter(a => a.statut === 'ACTIVE').length;
  }

  getInactiveCount(): number {
    return this.animatrices.filter(a => a.statut === 'INACTIVE').length;
  }

  deleteAnimatrice(id: number): void {
    if (confirm('Voulez-vous vraiment supprimer cette animatrice ?')) {
      this.animatriceService.deleteAnimatrice(id).subscribe({
        next: () => {
          this.animatrices = this.animatrices.filter(a => a.id !== id);
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }

  getAvatarColor(prenom: string): string {
    const colors = [
      '#4f46e5', '#7c3aed', '#db2777', '#dc2626',
      '#d97706', '#16a34a', '#0891b2', '#0284c7',
      '#9333ea', '#c026d3', '#059669', '#0d9488'
    ];
    const index = (prenom?.charCodeAt(0) || 0) % colors.length;
    return colors[index];
  }
}