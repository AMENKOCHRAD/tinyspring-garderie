import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="container-fluid bg-primary px-0 px-md-5 mb-5">
      <div class="row align-items-center px-3">
        <div class="col-lg-6 text-center text-lg-left">
          <h4 class="text-white mb-4 mt-5 mt-lg-0">Creche et jardin d enfants</h4>
          <h1 class="display-3 font-weight-bold text-white">Une version Angular prete pour TinySpring</h1>
          <p class="text-white mb-4">
            Le template TinySpring reste la page visiteur de reference. Apres connexion, le parent retrouve son
            dashboard et une page dediee a la gestion de transport.
          </p>
          <a routerLink="/login" class="btn btn-secondary mt-1 py-3 px-5 mr-2">Connexion</a>
          <a routerLink="/demandes" class="btn btn-outline-light mt-1 py-3 px-5">Transport</a>
        </div>
        <div class="col-lg-6 text-center text-lg-right">
          <img class="img-fluid mt-5 hero-illustration" src="/assets/kidkinder/img/header.png" alt="Illustration enfant" />
        </div>
      </div>
    </div>

    <section class="container-fluid py-5">
      <div class="container">
        <div class="row">
          <div class="col-lg-4 mb-4">
            <div class="portal-card bg-light p-4 h-100">
              <h4>Page visiteur</h4>
              <p>Cette page reste synchronisee avec le template public de ton groupe.</p>
            </div>
          </div>
          <div class="col-lg-4 mb-4">
            <div class="portal-card bg-light p-4 h-100">
              <h4>Dashboard parent</h4>
              <p>Apres connexion, le parent arrive sur son espace dedie en conservant le meme langage visuel.</p>
            </div>
          </div>
          <div class="col-lg-4 mb-4">
            <div class="portal-card bg-light p-4 h-100">
              <h4>Gestion transport</h4>
              <p>Une page supplementaire permet de creer, modifier et supprimer les demandes de transport.</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class HomePageComponent {}
