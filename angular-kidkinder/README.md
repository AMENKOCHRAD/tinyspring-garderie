# Conversion Angular du template KidKinder

Cette version transforme le template HTML statique en une petite application Angular standalone avec connexion par role.

## Ce qui a ete fait

- routing Angular pour `Accueil`, `A propos`, `Classes`, `Equipe`, `Contact`, `Connexion`, `Portail parent`, `Portail animateur` et `Dashboard`
- header/footer factorises en composants
- donnees centralisees dans `src/app/shared/site-data.ts`
- assets copies depuis le template original vers `src/assets/kidkinder`
- suppression de la dependance au `js/main.js` et au jQuery du template
- ajout des gestions parent: evenements, menu, transport, classes, boutique, staff, messagerie, reclamations
- ajout de la gestion transport cote admin avec workflow demande en attente -> acceptation/refus -> affectation

## Integration dans ton projet TinySpring

Si ton frontend TinySpring est deja en Angular, tu peux surtout recuperer :

- `src/app/**`
- `src/assets/kidkinder/**`
- `src/styles.css` ou au minimum l import vers `/assets/kidkinder/css/style.css`

Ensuite :

1. ajoute les routes de `src/app/app.routes.ts` dans ton routeur existant
2. branche les pages sur tes vrais services Angular et endpoints Spring Boot
3. remplace les contenus statiques de `site-data.ts` par des appels API si besoin

## Lancer localement

```bash
npm install
npm start
```

Je n ai pas lance `npm install` ici, donc pense a verifier le build une fois integre dans ton vrai projet.
