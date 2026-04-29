export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export function createEmptySpringPage<T>(size = 10): SpringPage<T> {
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size,
    first: true,
    last: true
  };
}
