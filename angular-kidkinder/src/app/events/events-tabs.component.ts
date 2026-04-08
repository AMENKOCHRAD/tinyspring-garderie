import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

export type ParentEventsTab = 'events' | 'participations' | 'menus';

@Component({
  selector: 'app-events-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="events-tab-selector">
      <button
        type="button"
        class="events-tab-button"
        [class.active]="activeTab === 'events'"
        (click)="select('events')">
        Evenements
      </button>
      <button
        type="button"
        class="events-tab-button"
        [class.active]="activeTab === 'participations'"
        (click)="select('participations')">
        Participations
      </button>
      <button
        type="button"
        class="events-tab-button"
        [class.active]="activeTab === 'menus'"
        (click)="select('menus')">
        Menus
      </button>
    </div>
  `,
  styles: [`
    .events-tab-selector {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 0.75rem;
      padding: 0.5rem;
      border-radius: 32px;
      background: #ede7df;
      box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.04);
    }

    .events-tab-button {
      min-height: 64px;
      border: 0;
      border-radius: 24px;
      background: transparent;
      color: #5f6c84;
      font-size: 1.05rem;
      font-weight: 700;
      transition: all 0.2s ease;
    }

    .events-tab-button.active {
      background: linear-gradient(135deg, #16b7d4, #1a78b8);
      color: #fff;
      box-shadow: 0 12px 28px rgba(22, 183, 212, 0.28);
    }

    @media (max-width: 767.98px) {
      .events-tab-selector {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class EventsTabsComponent {
  @Input() public activeTab: ParentEventsTab = 'events';
  @Output() public tabChange = new EventEmitter<ParentEventsTab>();

  protected select(tab: ParentEventsTab): void {
    this.tabChange.emit(tab);
  }
}
