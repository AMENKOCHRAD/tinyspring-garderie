// Angular Import
import { Component, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterModule } from '@angular/router';

// project import
import { SpinnerComponent } from './theme/shared/components/spinner/spinner.component';
import { ToastNotificationComponent } from './components/toast-notification/toast-notification.component';
import { NotificationService } from './services/notification.service';
import { AuthService } from './services/auth.service';
import { PredictionService } from './services/boutique/prediction.service';

@Component({
  selector: 'app-root',
  imports: [RouterModule, SpinnerComponent, ToastNotificationComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  private router = inject(Router);
  private notifService = inject(NotificationService);
  private authService = inject(AuthService);
  private predictionService = inject(PredictionService);

  ngOnInit() {
    // Démarre le polling si un admin est déjà connecté (page refresh)
    if (this.authService.isAdmin()) {
      this.notifService.startPolling();
      this.predictionService.startAlertPolling();
    }

    this.router.events.subscribe((evt) => {
      if (!(evt instanceof NavigationEnd)) return;

      window.scrollTo(0, 0);

      // Arrête le polling quand on revient sur la page de connexion (logout)
      if (evt.url === '/sign-in' || evt.url === '/login' || evt.url === '/') {
        this.notifService.stopPolling();
        this.predictionService.stopAlertPolling();
      }
    });
  }
}
