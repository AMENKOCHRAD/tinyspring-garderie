import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import listPlugin from '@fullcalendar/list';
import interactionPlugin from '@fullcalendar/interaction';

@Component({
  selector: 'app-rh-calendrier',
  standalone: true,
  imports: [CommonModule, FullCalendarModule],
  templateUrl: './rh-calendrier.component.html',
  styleUrl: './rh-calendrier.component.scss'
})
export class RhCalendrierComponent implements OnInit {

  isLoading = true;
  calendarOptions!: CalendarOptions;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  // ✅ Supprimé getHeaders() — l'intercepteur JWT gère ça automatiquement

  loadEvents(): void {
    this.http.get<any[]>('http://localhost:8081/api/admin/calendrier/events')
    .subscribe({
      next: (events) => {
        this.calendarOptions = {
          plugins: [dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
          initialView: 'dayGridMonth',
          locale: 'fr',
          headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,listMonth'
          },
          buttonText: {
            today: "Aujourd'hui",
            month: 'Mois',
            week: 'Semaine',
            list: 'Liste'
          },
          events: events,
          eventDisplay: 'block',
          dayMaxEvents: 3,
          height: 'auto',
          eventClick: (info) => {
            alert(`📅 ${info.event.title}\nDu : ${info.event.startStr}\nAu : ${info.event.endStr}`);
          }
        };
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Erreur calendrier', err);
        this.isLoading = false;
      }
    });
  }
}