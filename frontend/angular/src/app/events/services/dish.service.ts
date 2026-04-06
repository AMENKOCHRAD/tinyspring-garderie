import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';
import { Dish, MealCategory } from '../models/dish.model';

export interface DishRequest {
  dailyMenuId: number;
  mealType: MealCategory;
  name: string;
  description?: string;
  photoUrl?: string;
  allergens?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DishService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/menus/dishes`;

  create(payload: DishRequest): Observable<Dish> {
    return this.http.post<Dish>(this.apiUrl, payload);
  }

  update(id: number, payload: DishRequest): Observable<Dish> {
    return this.http.put<Dish>(`${this.apiUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  uploadPhoto(id: number, file: File): Observable<Dish> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Dish>(`${this.apiUrl}/${id}/upload-photo`, formData);
  }
}
