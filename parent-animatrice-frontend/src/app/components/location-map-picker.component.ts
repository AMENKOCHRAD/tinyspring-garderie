import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';

declare const L: any;

export interface PickedLocation {
  address: string;
  latitude: number;
  longitude: number;
}

@Component({
  selector: 'app-location-map-picker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="map-picker">
      <div class="map-picker__toolbar">
        <span class="section-chip">Carte OpenStreetMap</span>
        <p class="muted">Cliquez sur la carte pour definir l'emplacement exact de la maison.</p>
      </div>

      <div #mapContainer class="map-picker__map"></div>

      <div class="map-picker__details" *ngIf="selectedAddress">
        <strong>Adresse detectee</strong>
        <p class="muted">{{ selectedAddress }}</p>
        <small class="muted">Lat {{ selectedLatitude | number: '1.4-4' }} • Lng {{ selectedLongitude | number: '1.4-4' }}</small>
      </div>

      <div class="map-picker__details" *ngIf="errorMessage">
        <strong>Carte indisponible</strong>
        <p class="muted">{{ errorMessage }}</p>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .map-picker {
        display: grid;
        gap: 14px;
      }

      .map-picker__toolbar {
        display: grid;
        gap: 8px;
      }

      .map-picker__map {
        height: 320px;
        border-radius: 20px;
        overflow: hidden;
        border: 1px solid rgba(193, 222, 218, 0.9);
        box-shadow: var(--shadow-card);
      }

      .map-picker__details {
        padding: 16px 18px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.8);
        border: 1px solid rgba(193, 222, 218, 0.7);
      }
    `
  ]
})
export class LocationMapPickerComponent implements AfterViewInit, OnChanges {
  @ViewChild('mapContainer', { static: true }) private readonly mapContainer?: ElementRef<HTMLDivElement>;

  @Input() latitude: number | null = null;
  @Input() longitude: number | null = null;
  @Input() selectedAddress = '';
  @Output() readonly locationSelected = new EventEmitter<PickedLocation>();

  protected selectedLatitude: number | null = null;
  protected selectedLongitude: number | null = null;
  protected errorMessage = '';

  private map: any;
  private marker: any;
  private readonly defaultCenter = { lat: 36.8506, lng: 10.2008 };

  ngAfterViewInit(): void {
    if (typeof L === 'undefined') {
      this.errorMessage = 'Leaflet n a pas pu etre charge. Verifiez votre connexion internet.';
      return;
    }

    const center = this.latitude !== null && this.longitude !== null
      ? { lat: this.latitude, lng: this.longitude }
      : this.defaultCenter;

    this.map = L.map(this.mapContainer?.nativeElement, {
      center: [center.lat, center.lng],
      zoom: 13
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.map.on('click', (event: any) => {
      const lat = event.latlng.lat as number;
      const lng = event.latlng.lng as number;
      this.setMarker(lat, lng);
      void this.reverseGeocode(lat, lng);
    });

    if (this.latitude !== null && this.longitude !== null) {
      this.setMarker(this.latitude, this.longitude);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }

    if ((changes['latitude'] || changes['longitude']) && this.latitude !== null && this.longitude !== null) {
      this.setMarker(this.latitude, this.longitude);
      this.map.setView([this.latitude, this.longitude], 15);
    }
  }

  private setMarker(lat: number, lng: number): void {
    this.selectedLatitude = lat;
    this.selectedLongitude = lng;

    if (!this.marker) {
      this.marker = L.marker([lat, lng]).addTo(this.map);
    } else {
      this.marker.setLatLng([lat, lng]);
    }
  }

  private async reverseGeocode(lat: number, lng: number): Promise<void> {
    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`,
        {
          headers: {
            Accept: 'application/json'
          }
        }
      );

      const data = await response.json();
      const address = (data?.display_name as string | undefined)?.trim();

      this.selectedAddress = address && address.length > 0 ? address : `Latitude ${lat.toFixed(5)}, Longitude ${lng.toFixed(5)}`;

      this.locationSelected.emit({
        address: this.selectedAddress,
        latitude: lat,
        longitude: lng
      });
    } catch {
      this.selectedAddress = `Latitude ${lat.toFixed(5)}, Longitude ${lng.toFixed(5)}`;
      this.locationSelected.emit({
        address: this.selectedAddress,
        latitude: lat,
        longitude: lng
      });
    }
  }
}
