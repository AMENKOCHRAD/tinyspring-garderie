# tinyspring-garderie
Authentification avec JWT
- Gestion des utilisateurs et des rôles
- Gestion des demandes de transport
- Affectation des enfants aux trajets
- Recommandation de trajets
- Analyse IA des demandes de transport
- Prédiction de la demande transport
- ## Stack technique

- Backend: Spring Boot
- Frontend: Angular
- Base de données: MySQL
- IA transport: FastAPI / Python
- Sécurité: Spring Security + JWT

- ## Structure du projet

- `backend/garderie` : API Spring Boot
- `backend/ai-service` : microservice IA en Python/FastAPI
- `frontend/angular` : application frontend Angular

- ## Structure du projet

- `backend/garderie` : API Spring Boot
- `backend/ai-service` : microservice IA en Python/FastAPI
- `frontend/angular` : application frontend Angular

- ## Prérequis

- Java 17
- Maven
- Node.js
- Angular CLI
- MySQL
- Python 3.13

- ## Configuration

Le backend utilise par défaut :
- port `8081`
- base MySQL `garderie_app_db`

Le microservice IA utilise :
- port `8001`

Fichiers de configuration :
- `backend/garderie/src/main/resources/application.properties`
- `backend/garderie/src/main/resources/application-mysql.properties`
