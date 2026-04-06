import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Animatrice } from '../animatrice.model';
import { AnimatriceService } from '../../../services/RH/animatrice.service';

@Component({
  selector: 'app-list-animatrice',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './list-animatrice.component.html',
  styleUrl: './list-animatrice.component.scss'
})
export class ListAnimatriceComponent implements OnInit {

  animatrices: Animatrice[] = [];
  searchTerm: string = '';
  filterStatut: string = '';

  constructor(
    private animatriceService: AnimatriceService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAnimatrices();
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
}