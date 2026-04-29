# Angular Split Architecture

## Applications

```text
frontend/angular
├── angular.json
├── package.json
├── src/                              # Admin application kept intact
│   ├── app/
│   │   ├── demo/
│   │   ├── services/
│   │   └── theme/
│   └── environments/
└── projects/
    └── user-app/                     # Parent + Animatrice application
        ├── src/
        │   ├── app/
        │   │   ├── auth/
        │   │   ├── components/
        │   │   │   ├── animatrice/
        │   │   │   ├── demandes/
        │   │   │   ├── layout/
        │   │   │   ├── parent/
        │   │   │   └── shared/
        │   │   ├── core/
        │   │   │   └── interceptors/
        │   │   ├── models/
        │   │   └── services/
        │   ├── environments/
        │   ├── index.html
        │   ├── main.ts
        │   └── styles.scss
        └── tsconfig.app.json
```

## Routing Summary

- `ADMIN` stays in the existing admin app and lands on `/analytics`.
- `PARENT` is redirected to `user-app` and lands on `/parent`.
- `ANIMATRICE` is redirected to `user-app` and lands on `/animatrice`.

## User App Feature Areas

- `auth/`: login, auth session storage, guards, role redirection.
- `components/parent/`: TinySpring parent dashboard.
- `components/demandes/`: full parent CRUD for `DemandeTransport`.
- `components/animatrice/`: simple dashboard without transport features.
- `services/`: `DemandesService`, `TrajetsService`, `ChildrenService`.
- `models/`: auth and transport domain models.
