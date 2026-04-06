import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-hero',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container-fluid bg-primary mb-5">
      <div class="d-flex flex-column align-items-center justify-content-center page-header">
        <h3 class="display-3 font-weight-bold text-white text-center">{{ title }}</h3>
        <div class="d-inline-flex text-white">
          <p class="m-0"><a class="text-white" routerLink="/">Accueil</a></p>
          <p class="m-0 px-2">/</p>
          <p class="m-0">{{ title }}</p>
        </div>
      </div>
    </div>
  `
})
export class PageHeroComponent {
  @Input({ required: true }) title!: string;
}
