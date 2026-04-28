import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GroupeService } from '../services/groupe.service';
import { Groupe } from '../models/groupe.model';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './planning.component.html'
})
export class PlanningComponent implements OnInit {
  groupes: Groupe[] = [];
  hours: string[] = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'];
  salleMap: Map<string, Groupe[]> = new Map();

  constructor(private groupeService: GroupeService) {}

  ngOnInit(): void {
    this.groupeService.getAllGroupes().subscribe({
      next: (data) => {
        this.groupes = data;
        this.groupBySalle();
      },
      error: (err) => console.error('Erreur Planning', err)
    });
  }

  groupBySalle() {
    this.salleMap.clear();
    this.groupes.forEach(g => {
      // Navigation sécurisée
      // @ts-ignore
      const salleName = (g.classe && g.classe.salle && g.classe.salle.nom) ? g.classe.salle.nom : 'Salle Inconnue';
      if (!this.salleMap.has(salleName)) {
        this.salleMap.set(salleName, []);
      }
      this.salleMap.get(salleName)?.push(g);
    });
  }

  getStyle(g: Groupe): any {
    if (!g.horaireDebut || !g.horaireFin) return { display: 'none' };
    
    // Format attendu "HH:mm:ss" ou "HH:mm"
    const startParts = g.horaireDebut.split(':');
    const startHour = parseInt(startParts[0], 10);
    const startMin = parseInt(startParts[1], 10);
    
    const endParts = g.horaireFin.split(':');
    const endHour = parseInt(endParts[0], 10);
    const endMin = parseInt(endParts[1], 10);

    // Total = 10 heures (08:00 à 18:00) = 600 minutes
    const startOffsetMinutes = (startHour - 8) * 60 + startMin;
    const durationMinutes = (endHour * 60 + endMin) - (startHour * 60 + startMin);

    const leftPercent = (startOffsetMinutes / 600) * 100;
    const widthPercent = (durationMinutes / 600) * 100;

    return {
      'left': leftPercent + '%',
      'width': widthPercent + '%',
      'position': 'absolute',
      'height': '46px',
      'border-radius': '6px',
      'background-color': this.getColor(g.nom),
      'color': 'white',
      'font-size': '12.5px',
      'padding': '6px 10px',
      'overflow': 'hidden',
      'white-space': 'nowrap',
      'text-overflow': 'ellipsis',
      'box-shadow': '0 3px 6px rgba(0,0,0,0.1)',
      'display': 'flex',
      'align-items': 'center',
      'justify-content': 'flex-start',
      'z-index': '10',
      'top': '2px'
    };
  }

  getColor(nom: string): string {
    const colors = ['#0dcaf0', '#198754', '#ffc107', '#dc3545', '#6610f2', '#fd7e14'];
    let hash = 0;
    for (let i = 0; i < nom.length; i++) hash = nom.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }
}
