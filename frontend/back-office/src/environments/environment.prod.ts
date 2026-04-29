import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
<<<<<<< HEAD:frontend/back-office/src/environments/environment.prod.ts
  apiUrl: '/api'
=======
  apiUrl: 'http://localhost:8081/api',
  apiBaseUrl: 'http://localhost:8081/api',
  adminAppUrl: 'http://localhost:4200',
  userAppUrl: 'http://localhost:4201'
>>>>>>> origin/gestion-transports:frontend/angular/src/environments/environment.prod.ts
};
