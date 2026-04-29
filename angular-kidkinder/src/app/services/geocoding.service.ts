import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ReverseGeocodeResponse {
  lat: number;
  lng: number;
  adresse: string;
}

@Injectable({ providedIn: 'root' })
export class GeocodingService {
  private readonly http = inject(HttpClient);

  reverse(lat: number, lng: number): Observable<ReverseGeocodeResponse> {
    return this.http.get<ReverseGeocodeResponse>(
      `/api/geocode/reverse?lat=${encodeURIComponent(String(lat))}&lng=${encodeURIComponent(String(lng))}`
    );
  }
}

