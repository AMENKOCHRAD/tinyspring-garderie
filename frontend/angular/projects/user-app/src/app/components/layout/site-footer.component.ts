import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-site-footer',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container-fluid bg-secondary text-white mt-5 py-5 px-sm-3 px-md-5">
      <div class="row pt-5">
        <div class="col-lg-4 col-md-6 mb-5">
          <a routerLink="/parent" class="navbar-brand font-weight-bold text-primary m-0 mb-4 p-0" style="font-size: 40px; line-height: 40px;">
            <i class="flaticon-043-teddy-bear"></i>
            <span class="text-white">TinySpring</span>
          </a>
          <p>Portail parent Angular complet, separe du back-office admin, connecte au meme backend Spring Boot.</p>
        </div>
        <div class="col-lg-4 col-md-6 mb-5">
          <h3 class="text-primary mb-4">Navigation</h3>
          <div class="d-flex flex-column justify-content-start">
            <a class="text-white mb-2" routerLink="/"><i class="fa fa-angle-right mr-2"></i>Accueil</a>
            <a class="text-white mb-2" routerLink="/login"><i class="fa fa-angle-right mr-2"></i>Connexion</a>
            <a class="text-white mb-2" routerLink="/parent"><i class="fa fa-angle-right mr-2"></i>Dashboard parent</a>
            <a class="text-white mb-2" routerLink="/demandes"><i class="fa fa-angle-right mr-2"></i>Gestion transport</a>
          </div>
        </div>
        <div class="col-lg-4 col-md-6 mb-5">
          <h3 class="text-primary mb-4">Contact</h3>
          <p class="mb-2">Tunis, Tunisie</p>
          <p class="mb-2">contact&#64;tinyspring.local</p>
          <p class="mb-0">+216 00 000 000</p>
        </div>
      </div>
    </div>
  `
})
export class SiteFooterComponent {}
