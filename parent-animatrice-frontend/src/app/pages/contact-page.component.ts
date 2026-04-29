import { Component } from '@angular/core';
import { PageHeroComponent } from '../components/page-hero.component';

@Component({
  selector: 'app-contact-page',
  standalone: true,
  imports: [PageHeroComponent],
  template: `
    <app-page-hero title="Contact"></app-page-hero>

    <div class="container-fluid pt-5">
      <div class="container">
        <div class="text-center pb-2">
          <p class="section-title px-5"><span class="px-2">Prendre contact</span></p>
          <h1 class="mb-3">Une page prete pour ton formulaire Angular</h1>
          <h4 class="text-center mb-4">Tu peux maintenant brancher cette vue sur un endpoint Spring Boot ou un service Angular.</h4>
        </div>
        <div class="row">
          <div class="col-lg-7 mb-5">
            <div class="contact-form">
              <form>
                <div class="control-group mb-3">
                  <input type="text" class="form-control" placeholder="Votre nom">
                </div>
                <div class="control-group mb-3">
                  <input type="email" class="form-control" placeholder="Votre email">
                </div>
                <div class="control-group mb-3">
                  <input type="text" class="form-control" placeholder="Sujet">
                </div>
                <div class="control-group mb-3">
                  <textarea class="form-control" rows="6" placeholder="Message"></textarea>
                </div>
                <div>
                  <button class="btn btn-primary py-2 px-4" type="button">Envoyer</button>
                </div>
              </form>
            </div>
          </div>
          <div class="col-lg-5 mb-5">
            <div class="d-flex">
              <i class="fa fa-map-marker-alt d-inline-flex align-items-center justify-content-center bg-primary text-secondary rounded-circle" style="width: 45px; height: 45px;"></i>
              <div class="pl-3">
                <h5>Adresse</h5>
                <p>Tunis, Tunisie</p>
              </div>
            </div>
            <div class="d-flex">
              <i class="fa fa-envelope d-inline-flex align-items-center justify-content-center bg-primary text-secondary rounded-circle" style="width: 45px; height: 45px;"></i>
              <div class="pl-3">
                <h5>Email</h5>
                <p>contact&#64;tinyspring.local</p>
              </div>
            </div>
            <div class="d-flex">
              <i class="fa fa-phone-alt d-inline-flex align-items-center justify-content-center bg-primary text-secondary rounded-circle" style="width: 45px; height: 45px;"></i>
              <div class="pl-3">
                <h5>Telephone</h5>
                <p>+216 00 000 000</p>
              </div>
            </div>
            <div class="d-flex">
              <i class="far fa-clock d-inline-flex align-items-center justify-content-center bg-primary text-secondary rounded-circle" style="width: 45px; height: 45px;"></i>
              <div class="pl-3">
                <h5>Horaires</h5>
                <strong>Lundi - Vendredi</strong>
                <p class="m-0">08:00 - 17:00</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ContactPageComponent {}
