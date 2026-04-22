import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { landingMissionCards, landingServices, landingSteps } from '../shared/tinyspring-data';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css'
})
export class HomePageComponent {
  protected readonly missionCards = landingMissionCards;
  protected readonly services = landingServices;
  protected readonly steps = landingSteps;
}
