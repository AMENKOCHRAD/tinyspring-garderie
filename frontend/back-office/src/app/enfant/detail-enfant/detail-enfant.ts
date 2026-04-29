import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { EnfantService, Enfant } from '../enfant';

@Component({
  selector: 'app-detail-enfant',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './detail-enfant.html',
  styleUrls: ['./detail-enfant.scss']
})
export class DetailEnfantComponent implements OnInit {
  enfant?: Enfant;
  isLoading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private enfantService: EnfantService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id || Number.isNaN(id)) {
      this.error = 'ID enfant invalide.';
      return;
    }

    this.isLoading = true;
    this.error = '';
    this.enfant = undefined;

    this.enfantService.getEnfantById(id).subscribe({
      next: (data) => {
        this.enfant = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.error = 'Erreur lors du chargement des détails';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
