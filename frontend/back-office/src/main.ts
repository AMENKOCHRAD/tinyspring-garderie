/// <reference types="@angular/localize" />

import { enableProdMode, importProvidersFrom } from '@angular/core';
<<<<<<< HEAD:frontend/back-office/src/main.ts
=======
import { provideHttpClient, withInterceptors } from '@angular/common/http';

>>>>>>> origin/gestion-transports:frontend/angular/src/main.ts
import { environment } from './environments/environment';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { AppRoutingModule } from './app/app-routing.module';
import { AppComponent } from './app/app.component';
<<<<<<< HEAD:frontend/back-office/src/main.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http'; // ← ajoute withInterceptors
import { authInterceptor } from './app/auth.interceptor'; // ← ajoute cet import
=======
import { authInterceptor } from './app/services/auth.interceptor';
>>>>>>> origin/gestion-transports:frontend/angular/src/main.ts

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(BrowserModule, AppRoutingModule),
<<<<<<< HEAD:frontend/back-office/src/main.ts
     provideHttpClient(withInterceptors([authInterceptor])) // ← modifie cette ligne
=======
    provideHttpClient(withInterceptors([authInterceptor]))
>>>>>>> origin/gestion-transports:frontend/angular/src/main.ts
  ]
}).catch((err) => console.error(err));
