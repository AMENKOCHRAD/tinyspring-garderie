import { Injectable, signal } from '@angular/core';

export type ToastTone = 'success' | 'info' | 'error';

export interface ToastItem {
  id: number;
  message: string;
  tone: ToastTone;
  icon: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<ToastItem[]>([]);

  success(message: string): void {
    this.pushToast(message, 'success', '✓');
  }

  info(message: string): void {
    this.pushToast(message, 'info', 'ℹ');
  }

  error(message: string): void {
    this.pushToast(message, 'error', '✕');
  }

  dismiss(id: number): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }

  private pushToast(message: string, tone: ToastTone, icon: string): void {
    const toast: ToastItem = {
      id: this.nextId++,
      message,
      tone,
      icon
    };

    this.toasts.update((items) => [...items.slice(-2), toast]);
    window.setTimeout(() => this.dismiss(toast.id), 3000);
  }
}
