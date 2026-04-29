import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { GeocodingService } from '../services/geocoding.service';

declare const L: any;

@Component({
  selector: 'app-tunisia-address-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tunisia-address-picker.component.html',
  styleUrl: './tunisia-address-picker.component.css'
})
export class TunisiaAddressPickerComponent implements AfterViewInit, OnDestroy {
  private readonly geocoding = inject(GeocodingService);

  @Input() address = '';
  @Output() addressChange = new EventEmitter<string>();

  @Input() lat: number | null = null;
  @Output() latChange = new EventEmitter<number | null>();

  @Input() lng: number | null = null;
  @Output() lngChange = new EventEmitter<number | null>();

  autoFillAddress = true;

  @ViewChild('mapEl', { static: true }) mapEl!: ElementRef<HTMLDivElement>;

  private map: any = null;
  private marker: any = null;
  private reverseSub: Subscription | null = null;
  private initAttempts = 0;
  private initTimer: any = null;

  ngAfterViewInit(): void {
    this.tryInitMap();
  }

  private tryInitMap(): void {
    if (this.map) {
      return;
    }

    if (typeof L === 'undefined') {
      this.initAttempts++;
      if (this.initAttempts > 50) {
        return;
      }
      this.initTimer = setTimeout(() => this.tryInitMap(), 120);
      return;
    }

    const center = [34.0, 9.0]; // Tunisia

    this.map = L.map(this.mapEl.nativeElement, {
      zoomControl: true,
      attributionControl: true
    }).setView(center, 6);

    // Bounds roughly around Tunisia
    const southWest = L.latLng(30.0, 7.0);
    const northEast = L.latLng(38.0, 12.5);
    this.map.setMaxBounds(L.latLngBounds(southWest, northEast));

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    // initialize marker if existing coords
    if (this.lat != null && this.lng != null) {
      this.setMarker(this.lat, this.lng, false);
      this.map.setView([this.lat, this.lng], 13);
    }

    this.map.on('click', (e: any) => {
      const lat = Number(e?.latlng?.lat);
      const lng = Number(e?.latlng?.lng);
      if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
        return;
      }
      this.setMarker(lat, lng, true);
    });
  }

  ngOnDestroy(): void {
    if (this.reverseSub) {
      this.reverseSub.unsubscribe();
      this.reverseSub = null;
    }
    if (this.initTimer) {
      clearTimeout(this.initTimer);
      this.initTimer = null;
    }
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  onAddressInput(value: string): void {
    this.address = value;
    this.addressChange.emit(value);
  }

  clearLocation(): void {
    this.lat = null;
    this.lng = null;
    this.latChange.emit(null);
    this.lngChange.emit(null);
    if (this.marker && this.map) {
      this.map.removeLayer(this.marker);
      this.marker = null;
    }
  }

  private setMarker(lat: number, lng: number, reverse: boolean): void {
    this.lat = lat;
    this.lng = lng;
    this.latChange.emit(lat);
    this.lngChange.emit(lng);

    if (!this.map) {
      return;
    }

    const icon = L.divIcon({
      className: 'ts-map-pin',
      html: '<div class="ts-map-pin__dot"></div>',
      iconSize: [18, 18],
      iconAnchor: [9, 9]
    });

    if (!this.marker) {
      this.marker = L.marker([lat, lng], { icon }).addTo(this.map);
    } else {
      this.marker.setLatLng([lat, lng]);
    }

    if (reverse && this.autoFillAddress) {
      if (this.reverseSub) {
        this.reverseSub.unsubscribe();
        this.reverseSub = null;
      }
      this.reverseSub = this.geocoding.reverse(lat, lng).subscribe({
        next: (data) => {
          const addr = (data?.adresse ?? '').trim();
          if (addr) {
            this.address = addr;
            this.addressChange.emit(addr);
          }
        },
        error: () => {
          // ignore: keep manual address
        }
      });
    }
  }
}
