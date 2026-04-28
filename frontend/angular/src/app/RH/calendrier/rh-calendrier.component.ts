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
              today:  "Aujourd'hui",
              month:  'Mois',
              week:   'Semaine',
              list:   'Liste'
            },
            events: events,
            eventDisplay: 'block',
            dayMaxEvents: 3,
            height: 'auto',
            firstDay: 1, // ✅ Semaine commence le lundi
            eventClick: (info) => {
              const start = info.event.start
                ? new Date(info.event.start).toLocaleDateString('fr-FR')
                : '—';
              const end = info.event.end
                ? new Date(info.event.end).toLocaleDateString('fr-FR')
                : '—';
              alert(`📅 ${info.event.title}\n\nDébut : ${start}\nFin : ${end}`);
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