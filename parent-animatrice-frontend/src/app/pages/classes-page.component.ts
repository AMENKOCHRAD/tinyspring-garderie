import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeroComponent } from '../components/page-hero.component';
import { classes } from '../shared/site-data';

@Component({
  selector: 'app-classes-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeroComponent],
  template: `
    <app-page-hero title="Classes"></app-page-hero>

    <div class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Classes populaires</span></p>
          <h1 class="mb-4">Classes pour vos enfants</h1>
        </div>
        <div class="row">
          <div class="col-lg-4 mb-5" *ngFor="let item of classes">
            <div class="card border-0 bg-light shadow-sm pb-2 h-100">
              <img class="card-img-top mb-2" [src]="item.image" [alt]="item.title">
              <div class="card-body text-center">
                <h4 class="card-title">{{ item.title }}</h4>
                <p class="card-text">{{ item.description }}</p>
              </div>
              <div class="card-footer bg-transparent py-4 px-5">
                <div class="row border-bottom">
                  <div class="col-6 py-1 text-right border-right"><strong>Age</strong></div>
                  <div class="col-6 py-1">{{ item.age }}</div>
                </div>
                <div class="row border-bottom">
                  <div class="col-6 py-1 text-right border-right"><strong>Places</strong></div>
                  <div class="col-6 py-1">{{ item.seats }}</div>
                </div>
                <div class="row border-bottom">
                  <div class="col-6 py-1 text-right border-right"><strong>Horaire</strong></div>
                  <div class="col-6 py-1">{{ item.time }}</div>
                </div>
                <div class="row">
                  <div class="col-6 py-1 text-right border-right"><strong>Tarif</strong></div>
                  <div class="col-6 py-1">{{ item.fee }}</div>
                </div>
              </div>
              <a routerLink="/contact" class="btn btn-primary px-4 mx-auto mb-4">Reserver</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ClassesPageComponent {
  protected readonly classes = classes;
}
