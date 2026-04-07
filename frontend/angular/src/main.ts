/// <reference types="@angular/localize" />

import { enableProdMode, importProvidersFrom } from '@angular/core';
import { environment } from './environments/environment';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { AppRoutingModule } from './app/app-routing.module';
import { AppComponent } from './app/app.component';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; // ← ajoute withInterceptors
import { authInterceptor } from './app/auth.interceptor'; // ← ajoute cet import

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(BrowserModule, AppRoutingModule),
     provideHttpClient(withInterceptors([authInterceptor])) // ← modifie cette ligne
  ]
}).catch((err) => console.error(err));