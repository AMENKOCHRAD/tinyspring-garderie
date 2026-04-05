import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-events-module-switcher',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './events-module-switcher.component.html',
  styleUrls: ['./events-module-switcher.component.scss']
})
export class EventsModuleSwitcherComponent {}
