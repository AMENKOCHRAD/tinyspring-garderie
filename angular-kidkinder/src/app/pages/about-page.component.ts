import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { features, teachers } from '../shared/site-data';
import { PageHeroComponent } from '../components/page-hero.component';

@Component({
  selector: 'app-about-page',
  standalone: true,
  imports: [CommonModule, PageHeroComponent],
  template: `
    <app-page-hero title="A propos"></app-page-hero>

    <div class="container-fluid py-5">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-5">
            <img class="img-fluid rounded mb-5 mb-lg-0" src="/assets/kidkinder/img/about-1.jpg" alt="A propos">
          </div>
          <div class="col-lg-7">
            <p class="section-title pr-5"><span class="pr-2">Notre approche</span></p>
            <h1 class="mb-4">Une conversion pensee pour Angular</h1>
            <p>Cette page reprend le contenu du template initial en separant clairement le layout, les composants de page et les donnees reutilisables.</p>
            <div class="row pt-2 pb-4">
              <div class="col-6 col-md-4">
                <img class="img-fluid rounded" src="/assets/kidkinder/img/about-2.jpg" alt="Equipe">
              </div>
              <div class="col-6 col-md-8">
                <ul class="list-inline m-0">
                  <li class="py-2 border-top border-bottom"><i class="fa fa-check text-primary mr-3"></i>Navigation SPA avec Angular Router</li>
                  <li class="py-2 border-bottom"><i class="fa fa-check text-primary mr-3"></i>Sections reutilisables</li>
                  <li class="py-2 border-bottom"><i class="fa fa-check text-primary mr-3"></i>Facile a brancher sur des endpoints Spring</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container-fluid pt-5">
      <div class="container pb-3">
        <div class="row">
          <div class="col-lg-4 col-md-6 pb-1" *ngFor="let item of features">
            <div class="d-flex bg-light shadow-sm border-top rounded mb-4" style="padding: 30px;">
              <i class="{{ item.icon }} h1 font-weight-normal text-primary mb-3"></i>
              <div class="pl-4">
                <h4>{{ item.title }}</h4>
                <p class="m-0">{{ item.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Equipe</span></p>
          <h1 class="mb-4">Nos enseignants</h1>
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
export class AboutPageComponent {
  protected readonly features = features;
  protected readonly teachers = teachers;
}
