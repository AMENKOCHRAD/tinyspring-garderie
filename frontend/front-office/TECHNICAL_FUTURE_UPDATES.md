# 🔧 Quick Reference - Modifications Futures

## 📍 Où faire quoi

### 1. Ajouter un nouveau rôle
**Fichiers à modifier :**
```
src/app/shared/auth.models.ts          → Ajouter le rôle à UserRole type
src/app/shared/role.guard.ts            → Ajouter la redirection par rôle
src/app/app.routes.ts                   → Ajouter les routes + guards avec ce rôle
src/app/components/site-header.component.ts → Ajouter la nav du nouveau rôle (optionnel)
```

**Exemple : Ajouter le rôle BABYSITTER**
```typescript
// auth.models.ts
export type UserRole = 'PARENT' | 'ANIMATRICE' | 'ADMIN' | 'BABYSITTER';  // ← ADD

// role.guard.ts
private redirectByRole(role: UserRole | null): void {
  const redirectMap: Record<UserRole, string> = {
    'PARENT': '/parent/portal',
    'ANIMATRICE': '/animateur/portal',
    'ADMIN': '/dashboard',
    'BABYSITTER': '/babysitter/portal'  // ← ADD
  };
  // ...
}

// app.routes.ts
{
  path: 'babysitter/portal',
  component: BabysitterPortalPageComponent,  // ← Créer ce composant
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['BABYSITTER'] }
}
```

---

### 2. Intégrer un JWT quand le backend l'envoie
**Fichiers à créer/modifier :**
```
src/app/shared/auth.interceptor.ts      → CRÉER (nouveau)
src/app/shared/auth.models.ts           → Ajouter token à AuthUser
src/app/shared/auth.service.ts          → Stocker le token
src/app/app.config.ts                   → Fournir l'interceptor
```

**Étapes :**
1. Créer `auth.interceptor.ts` :
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const user = this.authService.getCurrentUser();
    
    if (user && user.token) {
      // Ajouter le header Authorization sur TOUTES les requêtes
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${user.token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

2. Modifier `AuthUser` dans `auth.models.ts` :
```typescript
interface AuthUser {
  email: string;
  role: UserRole;
  isAuthenticated: boolean;
  token?: string;  // ← ADD
}
```

3. Dans `AuthService.login()`, stocker le token :
```typescript
tap((response: LoginResponse) => {
  const authUser: AuthUser = {
    email: response.email,
    role: response.role,
    isAuthenticated: true,
    token: response.token  // ← ADD (quand backend l'envoie)
  };
  // ...
})
```

4. Dans `app.config.ts` :
```typescript
import { AuthInterceptor } from './shared/auth.interceptor';
import { HTTP_INTERCEPTORS } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideRouter(...),
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }  // ← ADD
  ]
};
```

---

### 3. Ajouter userId et nom à la réponse de login
**Fichier à modifier :**
```
src/app/shared/auth.models.ts           → Ajouter userId et name à LoginResponse et AuthUser
src/app/shared/auth.service.ts          → Exposer les getters
src/app/components/site-header.component.ts → Afficher le nom au lieu juste email (optionnel)
```

**Exemple :**
```typescript
// auth.models.ts
interface LoginResponse {
  message: string;
  email: string;
  role: UserRole;
  userId?: string;   // ← ADD
  name?: string;     // ← ADD
}

interface AuthUser {
  email: string;
  role: UserRole;
  isAuthenticated: boolean;
  userId?: string;   // ← ADD
  name?: string;     // ← ADD
}

// auth.service.ts
public getUserId(): string | null {
  return this.authSubject.value?.userId ?? null;
}

public getUserName(): string | null {
  return this.authSubject.value?.name ?? null;
}
```

---

### 4. Ajouter un interceptor d'erreur global (401 = logout)
**Fichier à créer :**
```
src/app/shared/error.interceptor.ts     → CRÉER (nouveau)
```

**Code :**
```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expiré ou invalide
          this.authService.logout();
          void this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
```

Puis ajouter à `app.config.ts` :
```typescript
{ provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true }
```

---

### 5. Ajouter une page de "Compte" avec les infos de l'utilisateur
**Fichiers à créer :**
```
src/app/pages/account-page.component.ts → CRÉER
src/app/app.routes.ts                   → Ajouter la route (protégée par AuthGuard)
```

**Structure :**
```typescript
// account-page.component.ts
@Component({
  selector: 'app-account-page',
  standalone: true,
  template: `
    <div>
      <h1>Mon compte</h1>
      <p>Email: {{ currentUser?.email }}</p>
      <p>Rôle: {{ currentUser?.role }}</p>
      <button (click)="logout()">Déconnexion</button>
    </div>
  `
})
export class AccountPageComponent {
  protected currentUser = this.authService.getCurrentUser();
  
  constructor(private authService: AuthService, private router: Router) {}
  
  protected logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}

// app.routes.ts
{
  path: 'account',
  component: AccountPageComponent,
  canActivate: [AuthGuard]  // Protégé mais accessible à tous les rôles
}
```

---

### 6. Ajouter un refresh token automatique
**Fichier à modifier :**
```
src/app/shared/auth.service.ts          → Ajouter logic de refresh automatique
```

**Concept :**
```typescript
public loginWithRefreshToken(): Observable<LoginResponse> {
  // Appeler un endpoint /api/auth/refresh
  // Stocker le nouveau token
  // Continuer
}

// Dans error.interceptor.ts, si 401 :
// 1. Essayer refreshToken
// 2. Rejouer la requête initiale
// 3. Si refresh échoue aussi, logout
```

---

### 7. Tester l'auth avec Cypress ou Playwight
**Fichier à créer :**
```
e2e/auth.cy.ts                          → Test Cypress
```

**Exemple Cypress :**
```typescript
describe('Authentication Flow', () => {
  it('should login successfully with PARENT role', () => {
    cy.visit('http://localhost:4200/login');
    cy.get('input[name="email"]').type('parent@garderie.com');
    cy.get('input[name="password"]').type('password');
    cy.get('button[type="submit"]').click();
    
    // Vérifier redirection
    cy.url().should('include', '/parent/portal');
    
    // Vérifier header affiche email
    cy.get('header').should('contain', 'parent@garderie.com');
  });

  it('should show error for invalid password', () => {
    cy.visit('http://localhost:4200/login');
    cy.get('input[name="email"]').type('parent@garderie.com');
    cy.get('input[name="password"]').type('wrongpassword');
    cy.get('button[type="submit"]').click();
    
    cy.get('.alert-danger').should('contain', 'Mot de passe incorrect');
    cy.url().should('include', '/login');
  });
});
```

---

### 8. Ajouter 2FA (Two-Factor Authentication)
**Approche simple :**
1. Login envoie code OTP au SMS/Email
2. Créer component `OTP verification` 
3. Modifier AuthService pour avoir un état intermédiaire "AWAITING_OTP"
4. Après OTP, login réel

**Fichiers :**
```
src/app/shared/auth.service.ts          → Ajouter verifyOtp()
src/app/pages/otp-verification-page.component.ts → Créer
src/app/app.routes.ts                   → Ajouter /otp-verification
```

---

## 🗺️ Map des fichiers d'authentification

```
src/app/
├── shared/
│   ├── auth.models.ts          ← Types et interfaces
│   ├── auth.service.ts         ← Logique d'auth
│   ├── auth.guard.ts           ← Protection générale
│   ├── role.guard.ts           ← Protection par rôle
│   ├── auth.interceptor.ts     ← [FUTUR] Ajouter token au header
│   └── error.interceptor.ts    ← [FUTUR] Gérer 401 global
│
├── pages/
│   ├── login-page.component.ts ← Formulaire login
│   ├── parent-portal-page.component.ts
│   ├── animator-portal-page.component.ts
│   └── account-page.component.ts ← [FUTUR] Compte utilisateur
│
├── components/
│   ├── site-header.component.ts ← Navbar avec auth state
│   └── dashboard-layout.component.ts
│
├── app.routes.ts               ← Routes avec guards
├── app.config.ts               ← Providers (HttpClient, etc.)
└── app.component.ts
```

---

## 📊 État des tâches futures

| Tâche | Priorité | Effort | Notes |
|-------|----------|--------|-------|
| JWT Integration | Haute | Moyen | Quand le backend envoie JWT |
| Error Interceptor 401 | Moyenne | Faible | Logout auto si token expiré |
| Add userId + name | Moyenne | Faible | Quand le backend les envoie |
| Account Page | Basse | Faible | Page de profil utilisateur |
| 2FA / OTP | Basse | Moyen | Si client demande |
| Tests E2E Cypress | Moyenne | Moyen | Pour chaque grand changement |
| Access Logs | Basse | Moyen | Audit des connexions |
| Remember Me | Basse | Faible | Option "Rester connecté" |

---

## ⚡ Commandes utiles

```bash
# Compiler
npm run build

# Démarrer dev
npm start

# Tests (une fois configurés)
npm test

# Tests E2E (une fois Cypress installé)
npx cypress open

# Linter
npm run lint
```

---

## 🚀 Checklist avant une modification

- [ ] Créer une branche : `git checkout -b feature/auth-xxx`
- [ ] Faire les changements
- [ ] Compiler : `npm run build`
- [ ] Tester manuellement les scénarios pertinents
- [ ] Vérifier console (erreurs) et Networks (requêtes HTTP)
- [ ] Commit avec message clair : "feat: add JWT support to AuthService"
- [ ] Merger vers main

---

## 💡 Conseils

1. **Ne jamais stocker les mots de passe** → Toujours côté backend
2. **Valider côté backend** → Pas seulement frontend
3. **Utiliser HTTPS en production** → Protéger localStorage
4. **Tests** → Couvrir tous les rôles et cas d'erreur
5. **Logging** → Garder trace des connexions (backend)
6. **Timeouts** → Sessions expirant automatiquement
7. **CORS** → Configuré strictement (pas * en production)

---

## 📞 Ressources

- [Angular HttpClient](https://angular.io/guide/http)
- [Angular Guards](https://angular.io/guide/router#preventing-unauthorized-access)
- [RxJS Observables](https://rxjs.dev/)
- [localStorage MDN](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)
- [JWT.io](https://jwt.io/)

---

**Dernière mise à jour :** 2026-04-07  
**Version :** 1.0  
**Auteur :** Team Dev
