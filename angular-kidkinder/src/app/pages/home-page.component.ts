import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { blogPosts, classes, features, teachers, testimonials } from '../shared/site-data';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container-fluid bg-primary px-0 px-md-5 mb-5">
      <div class="row align-items-center px-3">
        <div class="col-lg-6 text-center text-lg-left">
          <h4 class="text-white mb-4 mt-5 mt-lg-0">Creche et jardin d enfants</h4>
          <h1 class="display-3 font-weight-bold text-white">TinySpring</h1>
          <p class="text-white mb-4">Une solution moderne pour la gestion de votre garderie</p>
          <a routerLink="/login" class="btn btn-secondary mt-1 py-3 px-5 mr-2">Connexion</a>
          <a routerLink="/classes" class="btn btn-outline-light mt-1 py-3 px-5">Explorer</a>
        </div>
        <div class="col-lg-6 text-center text-lg-right">
          <img class="img-fluid mt-5 hero-illustration" src="/assets/kidkinder/img/header.png" alt="Illustration enfant">
        </div>
      </div>
    </div>

    <section class="container-fluid pt-5">
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
    </section>

    <section class="container-fluid py-5">
      <div class="container">
        <div class="row align-items-center">
          <div class="col-lg-5">
            <img class="img-fluid rounded mb-5 mb-lg-0" src="/assets/kidkinder/img/about-1.jpg" alt="A propos">
          </div>
          <div class="col-lg-7">
            <p class="section-title pr-5"><span class="pr-2">A propos</span></p>
            <h1 class="mb-4">TinySpring</h1>
            <p>TinySpring est une solution moderne pour la gestion de votre garderie, en combinant la puissance des technologies modernes et la simplicité d'utilisation.</p>
            <div class="row pt-2 pb-4">
              <div class="col-6 col-md-4">
                <img class="img-fluid rounded" src="/assets/kidkinder/img/about-2.jpg" alt="Classe">
              </div>
              <div class="col-6 col-md-8">
                <ul class="list-inline m-0">
                  <li class="py-2 border-top border-bottom"><i class="fa fa-check text-primary mr-3"></i><span class="font-weight-bold">Innovation</span></li>
                  <li class="py-2 border-bottom"><i class="fa fa-check text-primary mr-3"></i><span class="font-weight-bold">Simplicité d'utilisation</span></li>
                  <li class="py-2 border-bottom"><i class="fa fa-check text-primary mr-3"></i><span class="font-weight-bold">Design responsive</span></li>
                </ul>
              </div>
            </div>
            <a routerLink="/classes" class="btn btn-primary mt-2 py-2 px-4">Voir les classes</a>
          </div>
        </div>
      </div>
    </section>

    <section class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Classes</span></p>
          <h1 class="mb-4">Nos classes</h1>
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
              <a routerLink="/contact" class="btn btn-primary px-4 mx-auto mb-4">S inscrire</a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Equipe</span></p>
          <h1 class="mb-4">L equipe pedagogique</h1>
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
    </section>

    <section class="container-fluid py-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Temoignages</span></p>
          <h1 class="mb-4">Ce que disent les parents</h1>
        </div>
        <div class="row">
          <div class="col-lg-4 mb-4" *ngFor="let item of testimonials">
            <div class="bg-light shadow-sm rounded h-100 p-4">
              <h3 class="fas fa-quote-left text-primary mr-3"></h3>
              <p>{{ item.text }}</p>
              <div class="d-flex align-items-center pt-2">
                <img class="rounded-circle" [src]="item.image" [alt]="item.name" style="width: 70px; height: 70px;">
                <div class="pl-3">
                  <h5>{{ item.name }}</h5>
                  <i>{{ item.role }}</i>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Actualites</span></p>
          <h1 class="mb-4"> section blog</h1>
        </div>
        <div class="row pb-3">
          <div class="col-lg-4 mb-4" *ngFor="let post of blogPosts">
            <div class="card border-0 shadow-sm mb-2 h-100">
              <img class="card-img-top mb-2" [src]="post.image" [alt]="post.title">
              <div class="card-body bg-light text-center p-4">
                <h4>{{ post.title }}</h4>
                <p>{{ post.text }}</p>
                <a routerLink="/contact" class="btn btn-primary px-4 mx-auto my-2">Lire plus</a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `
})
export class HomePageComponent {
  protected readonly features = features;
  protected readonly classes = classes;
  protected readonly teachers = teachers;
  protected readonly testimonials = testimonials;
  protected readonly blogPosts = blogPosts;
}
