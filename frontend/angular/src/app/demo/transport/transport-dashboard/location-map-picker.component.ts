import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
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
        <span class="map-picker__chip">Carte OpenStreetMap</span>
        <p class="map-picker__hint">{{ helperText }}</p>
      </div>

      <div #mapContainer class="map-picker__map"></div>

      <div class="map-picker__details" *ngIf="selectedAddress">
        <strong>{{ resultLabel }}</strong>
        <p class="map-picker__address">{{ selectedAddress }}</p>
        <small class="map-picker__coords">
          Lat {{ selectedLatitude | number: '1.4-4' }} | Lng {{ selectedLongitude | number: '1.4-4' }}
        </small>
      </div>

      <div class="map-picker__details map-picker__details--error" *ngIf="errorMessage">
        <strong>Carte indisponible</strong>
        <p class="map-picker__address">{{ errorMessage }}</p>
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

      .map-picker__chip {
        display: inline-flex;
        width: fit-content;
        padding: 4px 10px;
        border-radius: 999px;
        background: rgba(232, 241, 255, 0.95);
        color: #1f5fbf;
        font-size: 12px;
        font-weight: 700;
      }

      .map-picker__hint,
      .map-picker__address,
      .map-picker__coords {
        margin: 0;
        color: #5f6b7a;
      }

      .map-picker__map {
        height: 320px;
        min-height: 320px;
        border-radius: 20px;
        overflow: hidden;
        border: 1px solid rgba(193, 222, 218, 0.9);
        box-shadow: 0 12px 28px rgba(31, 95, 191, 0.12);
      }

      .map-picker__details {
        display: grid;
        gap: 4px;
        padding: 16px 18px;
        border-radius: 18px;
        background: rgba(255, 255, 255, 0.82);
        border: 1px solid rgba(193, 222, 218, 0.7);
      }

      .map-picker__details--error {
        background: #fff7f7;
        border-color: #f3d0d0;
      }
    `
  ]
})
export class LocationMapPickerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapContainer', { static: true }) private readonly mapContainer?: ElementRef<HTMLDivElement>;

  @Input() latitude: number | null = null;
  @Input() longitude: number | null = null;
  @Input() selectedAddress = '';
  @Input() helperText = 'Cliquez sur la carte pour choisir la destination exacte du trajet.';
  @Input() resultLabel = 'Adresse selectionnee';
  @Output() readonly locationSelected = new EventEmitter<PickedLocation>();

  protected selectedLatitude: number | null = null;
  protected selectedLongitude: number | null = null;
  protected errorMessage = '';

  private map: any;
  private marker: any;
  private resizeObserver?: ResizeObserver;
  private invalidateTimer?: ReturnType<typeof setTimeout>;
  private readonly defaultCenter = { lat: 36.8065, lng: 10.1815 };

  ngAfterViewInit(): void {
    if (typeof L === 'undefined') {
      this.errorMessage = 'Leaflet n a pas pu etre charge. Verifiez votre connexion internet.';
      return;
    }

    const center =
      this.latitude !== null && this.longitude !== null
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

    this.scheduleInvalidateMap();
    this.startResizeObserver();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }

    if ((changes['latitude'] || changes['longitude']) && this.latitude !== null && this.longitude !== null) {
      this.setMarker(this.latitude, this.longitude);
      this.map.setView([this.latitude, this.longitude], 15);
      this.scheduleInvalidateMap();
    }
  }

  ngOnDestroy(): void {
    if (this.invalidateTimer) {
      clearTimeout(this.invalidateTimer);
    }

    this.resizeObserver?.disconnect();

    if (this.map) {
      this.map.remove();
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
    const fallbackAddress = `Latitude ${lat.toFixed(5)}, Longitude ${lng.toFixed(5)}`;

    try {
      const response = await fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`, {
        headers: {
          Accept: 'application/json'
        }
      });

      const data = await response.json();
      const address = (data?.display_name as string | undefined)?.trim();

      this.selectedAddress = address && address.length > 0 ? address : fallbackAddress;
    } catch {
      this.selectedAddress = fallbackAddress;
    }

    this.locationSelected.emit({
      address: this.selectedAddress,
      latitude: lat,
      longitude: lng
    });
  }

  private startResizeObserver(): void {
    if (!this.mapContainer?.nativeElement || typeof ResizeObserver === 'undefined') {
      return;
    }

    this.resizeObserver = new ResizeObserver(() => {
      this.scheduleInvalidateMap();
    });

    this.resizeObserver.observe(this.mapContainer.nativeElement);
  }

  private scheduleInvalidateMap(): void {
    if (!this.map) {
      return;
    }

    if (this.invalidateTimer) {
      clearTimeout(this.invalidateTimer);
    }

    this.invalidateTimer = setTimeout(() => {
      this.map.invalidateSize();
    }, 150);
  }
}
