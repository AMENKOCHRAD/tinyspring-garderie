import { environment } from 'src/environments/environment';

export function getImageUrl(path: string | null | undefined): string | null {
  if (!path) {
    return null;
  }

  const trimmedPath = path.trim();

  if (!trimmedPath) {
    return null;
  }

  if (/^[a-zA-Z]:\\/.test(trimmedPath) || trimmedPath.includes('\\')) {
    return null;
  }

  if (trimmedPath.startsWith('http://') || trimmedPath.startsWith('https://')) {
    return trimmedPath;
  }

  if (trimmedPath.startsWith('/')) {
    return `${environment.apiUrl}${trimmedPath}`;
  }

  if (trimmedPath.startsWith('uploads/')) {
    return `${environment.apiUrl}/${trimmedPath}`;
  }

  if (trimmedPath.startsWith('assets/')) {
    return `/${trimmedPath}`;
  }

  return null;
}

export function getSafeEventPhotoUrl(value: string | null | undefined): string | null {
  return getImageUrl(value);
}
