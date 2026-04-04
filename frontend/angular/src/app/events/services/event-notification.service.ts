import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface EventToastMessage {
  kind: 'success' | 'error';
  text: string;
}

@Injectable({
  providedIn: 'root'
})
export class EventNotificationService {
  private readonly messageSubject = new BehaviorSubject<EventToastMessage | null>(null);

  readonly message$ = this.messageSubject.asObservable();

  showSuccess(text: string): void {
    this.messageSubject.next({ kind: 'success', text });
  }

  clear(): void {
    this.messageSubject.next(null);
  }
}
