import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
  apiUrl: 'http://localhost:8082/api',
  apiBaseUrl: 'http://localhost:8082/api',
  adminAppUrl: 'http://localhost:4200',
  userAppUrl: 'http://localhost:4201'
};
