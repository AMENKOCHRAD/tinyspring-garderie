# 🚀 Guide de Déploiement - Authentification Backend

## ⚡ Démarrage rapide

### 1. Options de démarrage

**Option A : Avec le serveur backend Spring Boot (recommandé)**
```bash
# Terminal 1 - Backend Spring Boot
cd ~/TinySpring  # ou votre dossier backend
./mvnw spring-boot:run
# Le backend doit être accessible sur http://localhost:8081

# Terminal 2 - Frontend Angular
cd ~/angular-kidkinder
npm start
# L'app Angular sera sur http://localhost:4200
```

**Option B : Développement sans backend (page login restant vide)** 
```bash
npm start
# Vous pouvez naviguer sur les routes publiques
# Mais /parent/portal, /animateur/portal et /dashboard nécessitent auth
```

---

## 🧪 Scénarios de Test

### Préalable : Comptes de test créés sur le backend

Assurez-vous que les comptes suivants existent sur votre backend Spring Boot avec les bons rôles :

| Email | Mot de passe | Rôle | Redirection attendue |
|-------|-------------|------|---------------------|
| parent@garderie.com | password | PARENT | /parent/portal |
| animatrice@garderie.com | password | ANIMATRICE | /animateur/portal |
| admin@garderie.com | password | ADMIN | /dashboard |

### Test 1 : Login réussi (Parent)

```
Étapes :
1. Aller sur http://localhost:4200
2. Cliquer "Connexion"
3. Saisir : parent@garderie.com / password
4. Cliquer "Connexion"

Résultat attendu :
✅ Redirection vers http://localhost:4200/parent/portal
✅ Affichage du portail parent
✅ Header affiche "parent@garderie.com" + "(Parent)"
✅ Bouton "Déconnexion" disponible
```

### Test 2 : Login réussi (Animatrice)

```
Étapes :
1. Aller sur http://localhost:4200/login
2. Saisir : animatrice@garderie.com / password
3. Cliquer "Connexion"

Résultat attendu :
✅ Redirection vers http://localhost:4200/animateur/portal
✅ Affichage de l'espace animateur
✅ Header affiche "animatrice@garderie.com" + "(Animateur)"
```

### Test 3 : Login réussi (Admin)

```
Étapes :
1. Aller sur http://localhost:4200/login
2. Saisir : admin@garderie.com / password
3. Cliquer "Connexion"

Résultat attendu :
✅ Redirection vers http://localhost:4200/dashboard
✅ Affichage du tableau de bord admin
✅ Header affiche "admin@garderie.com" + "(Administrateur)"
✅ Menu latéral visible
```

### Test 4 : Erreur 404 (Utilisateur inexistant)

```
Étapes :
1. Aller sur http://localhost:4200/login
2. Saisir : inexistant@garderie.com / password
3. Cliquer "Connexion"

Résultat attendu :
❌ Message d'erreur : "Utilisateur introuvable"
❌ Rester sur la page /login
⏳ Le bouton doit redevenir actif
```

### Test 5 : Erreur 401 (Mauvais mot de passe)

```
Étapes :
1. Aller sur http://localhost:4200/login
2. Saisir : parent@garderie.com / motdepasseerroné
3. Cliquer "Connexion"

Résultat attendu :
❌ Message d'erreur : "Mot de passe incorrect"
❌ Rester sur la page /login
⏳ Le bouton doit redevenir actif
```

### Test 6 : Backend indisponible

```
Étapes :
1. Arrêter le serveur backend (Ctrl+C)
2. Aller sur http://localhost:4200/login
3. Saisir : parent@garderie.com / password
4. Cliquer "Connexion"

Résultat attendu :
❌ Message d'erreur : "Erreur de connexion au serveur. Veuillez vérifier..."
❌ Rester sur la page /login
```

### Test 7 : Déconnexion

```
Étapes (après login réussi) :
1. Être connecté sur http://localhost:4200/parent/portal
2. Cliquer le bouton "Déconnexion" (header ou portail)

Résultat attendu :
✅ localStorage vidé (dev tools → Application)
✅ Redirection vers http://localhost:4200 (home)
✅ Header affiche "Connexion" (plus de "Déconnexion")
✅ Impossible d'accéder à /parent/portal sans reconnexion
```

### Test 8 : Persistance au refresh

```
Étapes :
1. Login réussi sur parent@garderie.com
2. Être sur http://localhost:4200/parent/portal
3. Appuyer F5 (refresh)

Résultat attendu :
✅ Rester sur /parent/portal (pas redirection à /login)
✅ Données d'auth toujours disponibles
✅ Header affiche toujours email + "(Parent)"
```

### Test 9 : Accès réstricé (Parent tentant d'accéder au portail Animatrice)

```
Étapes :
1. Login en tant que parent@garderie.com
2. Taper manuellement http://localhost:4200/animateur/portal dans la barre

Résultat attendu :
❌ Redirection automatique vers http://localhost:4200/parent/portal
❌ Le RoleGuard bloque l'accès et redirige d'après le rôle
```

### Test 10 : Accès non authentifié

```
Étapes :
1. Logout complètement
2. Taper directement http://localhost:4200/parent/portal

Résultat attendu :
❌ Redirection vers http://localhost:4200/login
✅ Les routes privées sont protégées
```

### Test 11 : Routes publiques non affectées

```
Étapes :
1. Aller sur http://localhost:4200 (home)
2. Cliquer "A propos", "Classes", "Equipe", "Contact"

Résultat attendu :
✅ Toutes les routes publiques fonctionnent
✅ Pas de redirection vers /login
✅ Pages visibles sans authentification
```

### Test 12 : Suppression du localStorage et reconnexion

```
Étapes :
1. Être connecté sur /parent/portal
2. Dev Tools → Application → localStorage → supprimer la clé "auth_user"
3. Appuyer F5

Résultat attendu :
❌ Redirection vers /login (localStorage vide)
✅ Déconnexion forcée détectée
```

---

## 🔍 Vérifications Console

### Vérifier le stockage

```javascript
// Dev Tools → Console

// Voir les données d'auth stockées
console.log(JSON.parse(localStorage.getItem('auth_user')));

// Résultat attendu (après login) :
{
  email: "parent@garderie.com",
  role: "PARENT",
  isAuthenticated: true
}
```

### Vérifier les requêtes HTTP

```
Dev Tools → Network

Lors d'un login :
✅ POST http://localhost:8081/api/auth/login
   Status: 200 (succès) ou 401/404 (erreur)
   Headers: Content-Type: application/json
   Body: { email, password }
```

---

## 🐛 Troubleshooting

### Erreur : "Cannot match any routes: 'parent/portal'"
**Cause** : Routes non mises à jour ou build incomplet
**Solution** : 
```bash
npm run build
```

### Erreur : "Can't resolve '@angular/common/http'"
**Cause** : HttpClientModule non fourni dans app.config.ts
**Solution** : Vérifier que `provideHttpClient()` est dans appConfig.providers

### Login répond avec CORS error
**Cause** : Backend Spring Boot ne permet pas les requêtes CORS depuis localhost:4200
**Solution** : Ajouter CORS au backend
```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
        }
    };
}
```

### Le backend n'est pas accessible
**Symptôme** : Message d'erreur "Erreur de connexion au serveur"
**Vérification** :
```bash
# Vérifier que le backend tourne
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}'

# Doit recevoir une réponse (200, 401, 404, etc.) pas une CORS error
```

---

## 📋 Checklist Avant Production

- [ ] Backend Spring Boot sur http://localhost:8081 ✅
- [ ] Endpoint POST /api/auth/login fonctionne ✅
- [ ] Comptes de test créés avec les bons rôles ✅
- [ ] CORS configuré pour http://localhost:4200 ✅
- [ ] npm run build compile sans erreur ✅
- [ ] Tous les tests (Test 1-12) passent ✅
- [ ] localStorage vidé avant de passer en production ✅
- [ ] Logs backend consultés pour diagnostiquer les erreurs ✅

---

## 📱 Architecture de l'Auth

```
┌─────────────────────────────────────────────────────────┐
│                   ANGULAR FRONTEND                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  LoginPageComponent                                     │
│  ├── Email input                                        │
│  └── Password input                                     │
│      │                                                  │
│      ▼                                                  │
│  AuthService.login(email, password)                    │
│  ├── POST /api/auth/login                              │
│  ├── Parse response                                    │
│  ├── Store in localStorage                             │
│  └── Emit via auth$ Observable                         │
│      │                                                  │
│      ▼                                                  │
│  Router + Guards (AuthGuard, RoleGuard)                │
│  ├── Check isAuthenticated()                           │
│  ├── Check role match                                  │
│  └── Allow/Redirect                                    │
│                                                         │
│  SiteHeaderComponent                                    │
│  ├── Subscribe auth$                                   │
│  ├── Show email + role or "Connexion"                  │
│  └── Logout button calls authService.logout()          │
│                                                         │
└─────────────────────────────────────────────────────────┘
                          │
                          │ HTTP
                          ▼
┌─────────────────────────────────────────────────────────┐
│              SPRING BOOT BACKEND                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  AuthController                                        │
│  └── POST /api/auth/login                              │
│      ├── Validate email exists                         │
│      ├── Validate password matches                     │
│      ├── Fetch user role                               │
│      └── Return { message, email, role }               │
│          OR 404/401 with error                         │
│                                                         │
│  Database                                              │
│  └── Users table with email, password, role            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎓 Pour les futurs développeurs

### Si vous devez ajouter un nouveau rôle

1. Ajouter le rôle à `UserRole` type dans `auth.models.ts`
2. Ajouter la mapping dans `RoleGuard.redirectByRole()`
3. Ajouter la route avec les guards et le rôle requis dans `app.routes.ts`

### Si le backend envoie un JWT

1. Stocker le token dans localStorage
2. Créer un `AuthInterceptor` pour rajouter le header `Authorization: Bearer <token>`
3. Ajouter l'interceptor à `appConfig.providers`

### Si le backend envoie plus d'infos (userId, nom, etc.)

1. Ajouter les champs à `LoginResponse` et `AuthUser`
2. Mettre à jour `AuthService` pour stocker/transmettre ces données
3. Les composants accèdent via `authService.getCurrentUser()`

---

## 📞 Questions fréquentes

**Q : Où est stockée l'info d'auth ?**  
R : Dans `localStorage` sous la clé `auth_user` + en mémoire dans le BehaviorSubject de `AuthService`

**Q : Qu'advient-il du token si le backend l'envoie un jour ?**  
R : Ajouter un champ `token` à `AuthUser` et le stocker aussi (prêt pour ce moment)

**Q : Comment les routes "/parent/portal", "/animateur/portal" sont protégées ?**  
R : Avec `canActivate: [AuthGuard, RoleGuard]` et `data: { roles: ['PARENT'] }` (ou autre rôle)

**Q : Que se passe-t-il si je visite /parent/portal sans être connecté ?**  
R : AuthGuard redirige vers /login

**Q : Que se passe-t-il si je suis connecté en PARENT et je vire d'accéder à /animateur/portal ?**  
R : RoleGuard redirige vers /parent/portal (mon rôle)

---

## ✅ Statut de la refactorisation

| Feature | Statut | Notes |
|---------|--------|-------|
| Login avec email/password | ✅ | Backend réel intégré |
| Suppression du select rôle | ✅ | Éliminé, rôle du backend |
| Guards (Auth + Role) | ✅ | Routes protégées |
| Persistance localStorage | ✅ | Au refresh = toujours connecté |
| Déconnexion | ✅ | Partout (header + portails) |
| Navigation par rôle | ✅ | Parent → /parent/portal, etc. |
| Messages d'erreur | ✅ | 404, 401, CORS gérés |
| Loading indicator | ✅ | Pendant la requête |
| JWT support | ⏳ | Prêt, juste besoin d'interceptor |
| userId dans response | ⏳ | Peut être ajouté au backend |
| Nom dans response | ⏳ | Peut être ajouté au backend |
