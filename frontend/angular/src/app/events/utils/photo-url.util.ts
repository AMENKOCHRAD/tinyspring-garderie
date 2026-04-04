import { environment } from 'src/environments/environment';

export function getSafeEventPhotoUrl(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }

  const trimmedValue = value.trim();

  if (!trimmedValue) {
    return null;
  }

  // Reject local filesystem paths and malformed browser-unsafe values.
  if (/^[a-zA-Z]:\\/.test(trimmedValue) || trimmedValue.includes('\\')) {
    return null;
  }

  if (trimmedValue.startsWith('http://') || trimmedValue.startsWith('https://')) {
    return trimmedValue;
  }

  if (trimmedValue.startsWith('/uploads/') || trimmedValue.startsWith('/assets/')) {
    return trimmedValue.startsWith('/uploads/')
      ? `${environment.apiUrl}${trimmedValue}`
      : trimmedValue;
  }

  if (trimmedValue.startsWith('uploads/') || trimmedValue.startsWith('assets/')) {
    return trimmedValue.startsWith('uploads/')
      ? `${environment.apiUrl}/${trimmedValue}`
      : `/${trimmedValue}`;
  }

  return null;
}
