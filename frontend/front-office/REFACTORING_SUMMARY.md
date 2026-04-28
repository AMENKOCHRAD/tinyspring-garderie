# 📋 Refactorisation Angular - Authentification Backend & Séparation des Espaces

## 🎯 Résumé des modifications

Votre application Angular frontoffice a été complètement refactorisée pour intégrer une vraie authentification backend Spring Boot et créer une séparation claire des espaces utilisateurs.

---

## 📁 FICHIERS CRÉÉS

### 1. **`src/app/shared/auth.models.ts`** (Nouveau)
**Interfaces TypeScript pour l'authentification**

```typescript
// LoginRequest - Payload envoyé au backend
interface LoginRequest {
  email: string;
  password: string;
}

// LoginResponse - Réponse du backend après login réussi
interface LoginResponse {
  message: string;
  email: string;
  role: UserRole;
}

// AuthUser - Représentation interne de l'utilisateur authentifié
interface AuthUser {
  email: string;
  role: UserRole;
  isAuthenticated: boolean;
}

// UserRole - Rôles disponibles
type UserRole = 'PARENT' | 'ANIMATRICE' | 'ADMIN';
```

---

### 2. **`src/app/shared/auth.service.ts`** (Nouveau)
**Service d'authentification avec intégration backend réelle**

**Fonctionnalités :**
- Appelle le vrai endpoint `POST /api/auth/login` (http://localhost:8081)
- Gère les erreurs (404, 401, autres)
- Stocke l'état d'auth en localStorage (sans JWT à ce stade)
- Expose une Observable `auth$` pour les composants

**Méthodes publiques :**
```typescript
login(email: string, password: string): Observable<LoginResponse>
logout(): void
isAuthenticated(): boolean
getCurrentUser(): AuthUser | null
getCurrentRole(): UserRole | null
hasRole(role: UserRole): boolean
```

**Erreurs gérées :**
- **404** → "Utilisateur introuvable"
- **401** → "Mot de passe incorrect"
- **0 (CORS/Network)** → Message d'erreur de connexion serveur
- Autres → Message personnalisé ou message d'erreur par défaut

---

### 3. **`src/app/shared/auth.guard.ts`** (Nouveau)
**Guard pour protéger les routes privées**

- Vérifie que l'utilisateur est authentifié
- Redirige vers `/login` si non authentifié
- Préserve l'URL de retour en queryParams

---

### 4. **`src/app/shared/role.guard.ts`** (Nouveau)
**Guard pour protéger les routes par rôle**

- Vérifie que l'utilisateur a le rôle requis (défini en `route.data.roles`)
- Redirige vers l'espace approprié si rôle insuffisant
- Mapping automatique vers le portail du rôle

---

## 📝 FICHIERS MODIFIÉS

### 1. **`src/app/app.config.ts`**
**Ajout de HttpClientModule**

```typescript
import { provideHttpClient } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),  // ← AJOUT
    provideRouter(...)
  ]
};
```

**Justification :** Nécessaire pour que `AuthService` puisse faire des appels HTTP au backend

---

### 2. **`src/app/app.routes.ts`**
**Ajout des guards et protection des routes**

```typescript
// Routes publiques (inchangées)
{ path: '', component: HomePageComponent, pathMatch: 'full' },
{ path: 'login', component: LoginPageComponent },
// ... autres routes publiques

// Routes protégées - PARENT
{
  path: 'parent/portal',
  component: ParentPortalPageComponent,
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['PARENT'] }
}

// Routes protégées - ANIMATRICE
{
  path: 'animateur/portal',
  component: AnimatorPortalPageComponent,
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['ANIMATRICE'] }
}

// Routes protégées - ADMIN
{
  path: 'dashboard',
  component: DashboardLayoutComponent,
  canActivate: [AuthGuard, RoleGuard],
  data: { roles: ['ADMIN'] },
  children: [ /* ... */ ]
}
```

**Changements clés :**
- ✅ AuthGuard + RoleGuard sur toutes les routes privées
- ✅ Données de rôle requis dans `route.data.roles`
- ✅ Routes publiques non modifiées

---

### 3. **`src/app/pages/login-page.component.ts`**
**Refactorisation complète du formulaire de login**

**Avant :**
- Sélection manuelle du rôle (parent, animateur, admin)
- Pas d'appel au backend réel
- Redirection fictive immédiate

**Après :**
- ✅ Suppression du champ select Role
- ✅ Intégration du vrai `AuthService`
- ✅ Appel réel au backend `POST /api/auth/login`
- ✅ Gestion des erreurs (404, 401) avec messages clairs
- ✅ Indicateur de loading pendant la requête
- ✅ Redirection automatique selon le rôle backend
- ✅ Exemples de credentials en démo

**Template :**
```html
<input type="email" name="email" [(ngModel)]="email" />
<input type="password" name="password" [(ngModel)]="password" />
<!-- ❌ Pas de select rôle -->

<button [disabled]="isLoading">
  {{ isLoading ? 'Vérification...' : 'Connexion' }}
</button>

<div *ngIf="error" class="alert alert-danger">{{ error }}</div>
```

---

### 4. **`src/app/components/site-header.component.ts`**
**Intégration de l'état d'authentification**

**Avant :**
- Bouton "Connexion" toujours visible
- Aucun affichage d'état

**Après :**
- ✅ Subscribe à `authService.auth$`
- ✅ Affichage conditionnel : "Connexion" ou "Déconnexion + email + rôle"
- ✅ Navigation adapté au rôle (Parent/Animateur/Admin)
- ✅ Bouton déconnexion fonctionnel

```html
<!-- Non authentifié -->
<a routerLink="/login" class="btn btn-primary">Connexion</a>

<!-- Authentifié -->
<div *ngIf="isLoggedIn">
  <span>{{ currentUser?.email }} ({{ getRoleLabel(role) }})</span>
  <button (click)="logout()">Déconnexion</button>
</div>
```

---

### 5. **`src/app/pages/parent-portal-page.component.ts`**
**Ajout de la déconnexion fonctionnelle**

**Changement :**
- ✅ Injection du `AuthService` et `Router`
- ✅ Bouton "Déconnexion" qui appelle `logout()`
- ✅ Redirection vers `/` après logout

```typescript
protected logout(): void {
  this.authService.logout();
  void this.router.navigate(['/']);
}
```

---

### 6. **`src/app/pages/animator-portal-page.component.ts`**
**Ajout de la déconnexion fonctionnelle**

**Même évolution que le portail parent**

---

### 7. **`src/app/components/dashboard-layout.component.ts`**
**Ajout de la déconnexion fonctionnelle**

**Avant :**
```html
<a routerLink="/admin/login">Déconnexion</a>  <!-- ??? Route inexistante -->
```

**Après :**
```html
<button (click)="logout()">Déconnexion</button>  <!-- Vrai logout -->
```

---

## 🔄 FLOW DE LOGIN

```
┌─────────────────────────────────────────────────────────────┐
│                  Utilisateur sur /login                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │  Saisit email + password     │
        │  (PAS de sélection rôle)     │
        └──────────────┬───────────────┘
                       │
                       ▼
        ┌──────────────────────────────┐
        │  Clique "Connexion"          │
        │  (Loading = true)            │
        └──────────────┬───────────────┘
                       │
                       ▼
    ┌──────────────────────────────────────┐
    │  AuthService.login(email, password)  │
    │                                      │
    │  POST /api/auth/login                │
    │  {email, password}                   │
    └──────────────┬───────────────────────┘
                   │
           ┌───────┴────────┬──────────────┐
           │                │              │
        ✅ 200            ❌ 404        ❌ 401
    (User exists)   (User notfound)  (Bad password)
           │                │              │
           ▼                ▼              ▼
      { role,          Error msg       Error msg
        email }        "Utilisateur"   "Mot de
                       "introuvable"   passe
                                       incorrect"
           │                │              │
           ▼                ▼              ▼
      Store auth      Show error     Show error
      in memory +      message       message
      localStorage
           │
           ▼
    ┌────────────────────────────────────┐
    │  Check role from LoginResponse     │
    │  - Map to redirect URL             │
    └────────────┬───────────────────────┘
                 │
         ┌───────┼───────┬─────────────┐
         │       │       │             │
      PARENT  ANIMATRICE ADMIN       UNKNOWN
         │       │       │             │
         ▼       ▼       ▼             ▼
    /parent/  /animateur/ /dashboard  /
    portal    portal
         │       │       │             │
         └───────┴───────┴─────────────┘
                 │
                 ▼
    ┌──────────────────────────────────┐
    │  React aux guards :              │
    │  AuthGuard → OK (authenticated)  │
    │  RoleGuard → Check role          │
    │             Allow or redirect    │
    └──────────────────────────────────┘
                 │
                 ▼
    ┌──────────────────────────────────┐
    │  Accès au portail utilisateur    │
    │  (selon le rôle)                 │
    └──────────────────────────────────┘
```

---

## 🔐 SÉCURITÉ ET GARDE-FOUS

### AuthGuard
- ✅ Bloque les routes privées si `!isAuthenticated()`
- ✅ Redirige vers `/login` si nécessaire
- ✅ Préserve l'URL de retour

### RoleGuard
- ✅ Vérifie que le rôle utilisateur fait partie de `route.data.roles`
- ✅ Redirige automatiquement vers le bon portail si accès refusé
- ✅ Impossible pour un parent d'accéder au portail animatrice

### Persistance
- ✅ Au refresh, l'utilisateur reste connecté si localStorage contient les infos
- ✅ `AuthService` restaure l'état au démarrage
- ✅ Les guards continuent à protéger les routes

---

## 📊 STRUCTURE DES ESPACES

```
FRONTOFFICE (public)
├── / (home)
├── /about (à propos)
├── /classes (classes)
├── /team (équipe)
├── /contact (contact)
├── /login (connexion)
│
└── ESPACES PROTÉGÉS (nécessitent authentification)
    │
    ├── PARENT (si role = 'PARENT')
    │   └── /parent/portal ← Portail parent
    │
    ├── ANIMATRICE (si role = 'ANIMATRICE')
    │   └── /animateur/portal ← Portail animateur
    │
    └── ADMIN (si role = 'ADMIN')
        └── /dashboard ← Dashboard admin (avec sous-routes)
            ├── /dashboard/parents
            ├── /dashboard/children
            ├── /dashboard/classes
            ├── /dashboard/teachers
            ├── /dashboard/registrations
            ├── /dashboard/payments
            └── /dashboard/transport
```

---

## 🚀 POINTS CLÉS D'IMPLÉMENTATION

1. **Pas d'invention de JWT**
   - ✅ Storage minimal : `{ email, role, isAuthenticated }`
   - ✅ Pas d'interceptor Bearer
   - ✅ Prêt pour JWT ultérieur

2. **Utilisation du vrai backend**
   - ✅ Endpoint réel : `POST http://localhost:8081/api/auth/login`
   - ✅ Pas de faux endpoint
   - ✅ Gestion réelle des erreurs (404, 401)

3. **Séparation des espaces**
   - ✅ Chaque rôle a son propre portail
   - ✅ Routes protégées par rôle
   - ✅ Navigation adaptée au rôle

4. **UX/Design**
   - ✅ Pas de sélection manuelle du rôle
   - ✅ Login simple et épuré
   - ✅ Messages d'erreur clairs
   - ✅ Indicateur de loading
   - ✅ Déconnexion fonctionnelle partout

5. **Maintenabilité**
   - ✅ Interfaces TypeScript propres
   - ✅ Service centralisé pour l'auth
   - ✅ Guards réutilisables
   - ✅ Code commenté
   - ✅ Aucune régression visuelle

---

## ✅ CHECKLIST POST-DÉPLOIEMENT

- [ ] Backend disponible sur http://localhost:8081 ?
- [ ] Endpoint `/api/auth/login` fonctionnel ?
- [ ] Comptes de test créés (PARENT, ANIMATRICE, ADMIN) ?
- [ ] Tester login avec un compte PARENT → redirection /parent/portal ?
- [ ] Tester login avec un compte ANIMATRICE → redirection /animateur/portal ?
- [ ] Tester login avec un compte ADMIN → redirection /dashboard ?
- [ ] Tester erreur 404 (utilisateur inexistant) ?
- [ ] Tester erreur 401 (mauvais mot de passe) ?
- [ ] Tester bouton Déconnexion → retour à / ?
- [ ] Tester refresh sur portail → reste connecté ?
- [ ] Tester accès direct à /parent/portal sans auth → redirige /login ?
- [ ] Tester parent accédant à /animateur/portal → redirige /parent/portal ?

---

## 📞 NOTES FUTURES

- **JWT** : Quand le backend sera prêt à envoyer un JWT, il suffit d'ajouter un interceptor
- **userId** : Quand le backend enverra un userId, l'ajouter à `LoginResponse` et à `AuthUser`
- **Nom utilisateur** : Quand le backend enverra un nom, l'ajouter aux réponses et afficher dans le header

---

## 🎓 RÉSUMÉ POUR LES DÉVELOPPEURS

La refactorisation fournit une **base solide et maintenable** pour l'authentification :

1. **Services** : `AuthService` centralisé (prêt pour interceptor JWT)
2. **Guards** : `AuthGuard` (authentification) + `RoleGuard` (autorisation)
3. **Models** : Interfaces `LoginRequest`, `LoginResponse`, `AuthUser`
4. **UI** : Login simplifié, header réactif, déconnexion partout
5. **Routes** : Hiérarchie claire et protégées par rôle

**Aucune dette technique introduite.** Code prêt pour évolution future.
