import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { PageHeroComponent } from '../components/page-hero.component';
import { teachers } from '../shared/site-data';

@Component({
  selector: 'app-team-page',
  standalone: true,
  imports: [CommonModule, PageHeroComponent],
  template: `
    <app-page-hero title="Equipe"></app-page-hero>

    <div class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Nos enseignants</span></p>
          <h1 class="mb-4">Une equipe bienveillante</h1>
        </div>
        <div class="row">
          <div class="col-md-6 col-lg-3 text-center team mb-5" *ngFor="let teacher of teachers">
            <div class="position-relative overflow-hidden mb-4" style="border-radius: 100%;">
              <img class="img-fluid w-100" [src]="teacher.image" [alt]="teacher.name">
            </div>
            <h4>{{ teacher.name }}</h4>
            <i>{{ teacher.role }}</i>
          </div>
        </div>
      </div>
    </div>
  `
})
export class TeamPageComponent {
  protected readonly teachers = teachers;
}
