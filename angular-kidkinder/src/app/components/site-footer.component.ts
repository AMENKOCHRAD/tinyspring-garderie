import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { adminNavItems, navItems, parentNavItems } from '../shared/site-data';

@Component({
  selector: 'app-site-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container-fluid bg-secondary text-white mt-5 py-5 px-sm-3 px-md-5">
      <div class="row pt-5">
        <div class="col-lg-3 col-md-6 mb-5">
          <a routerLink="/" class="navbar-brand font-weight-bold text-primary m-0 mb-4 p-0" style="font-size: 40px; line-height: 40px;">
            <i class="flaticon-043-teddy-bear"></i>
            <span class="text-white">TinySpring</span>
          </a>
          <p>Une vitrine Angular inspiree du template KidKinder, avec connexion par role et espaces dedies pour parent, animateur et administration.</p>
        </div>
        <div class="col-lg-3 col-md-6 mb-5">
          <h3 class="text-primary mb-4">Coordonnees</h3>
          <div class="d-flex">
            <h4 class="fa fa-map-marker-alt text-primary"></h4>
            <div class="pl-3">
              <h5 class="text-white">Adresse</h5>
              <p>Tunis, Tunisie</p>
            </div>
          </div>
          <div class="d-flex">
            <h4 class="fa fa-envelope text-primary"></h4>
            <div class="pl-3">
              <h5 class="text-white">Email</h5>
              <p>contact&#64;tinyspring.local</p>
            </div>
          </div>
          <div class="d-flex">
            <h4 class="fa fa-phone-alt text-primary"></h4>
            <div class="pl-3">
              <h5 class="text-white">Telephone</h5>
              <p>+216 00 000 000</p>
            </div>
          </div>
        </div>
        <div class="col-lg-3 col-md-6 mb-5">
          <h3 class="text-primary mb-4">Navigation</h3>
          <div class="d-flex flex-column justify-content-start">
            <a *ngFor="let item of items" class="text-white mb-2" [routerLink]="item.path">
              <i class="fa fa-angle-right mr-2"></i>{{ item.label }}
            </a>
            <a *ngFor="let item of parentItems" class="text-white mb-2" [routerLink]="item.path">
              <i class="fa fa-angle-right mr-2"></i>{{ item.label }}
            </a>
            <a *ngFor="let item of adminItems" class="text-white mb-2" [routerLink]="item.path">
              <i class="fa fa-angle-right mr-2"></i>{{ item.label }}
            </a>
          </div>
        </div>
        <div class="col-lg-3 col-md-6 mb-5">
          <h3 class="text-primary mb-4">Newsletter</h3>
          <form>
            <div class="form-group">
              <input type="text" class="form-control border-0 py-4" placeholder="Votre nom">
            </div>
            <div class="form-group">
              <input type="email" class="form-control border-0 py-4" placeholder="Votre email">
            </div>
            <div>
              <button class="btn btn-primary btn-block border-0 py-3" type="button">S inscrire</button>
            </div>
          </form>
        </div>
      </div>
      <div class="container-fluid pt-5" style="border-top: 1px solid rgba(23, 162, 184, .2);">
        <p class="m-0 text-center text-white">
          &copy; TinySpring. Conversion Angular du template KidKinder pour integration dans ton projet.
        </p>
      </div>
    </div>
  `
})
export class SiteFooterComponent {
  protected readonly items = navItems;
  protected readonly parentItems = parentNavItems;
  protected readonly adminItems = adminNavItems;
}
