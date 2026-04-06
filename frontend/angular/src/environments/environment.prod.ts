import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
  apiUrl: 'http://localhost:8081/api',
  apiBaseUrl: 'http://localhost:8081/api',
  adminAppUrl: 'http://localhost:4200',
  userAppUrl: 'http://localhost:4201'
};
