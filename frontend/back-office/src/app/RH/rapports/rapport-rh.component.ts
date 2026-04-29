import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

export interface RapportRH {
  id?: number;
  question: string;
  typeRapport?: string;
  periode?: string;
  contenu?: string;
  dateGeneration?: string;
}

@Component({
  selector: 'app-rapport-rh',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './rapport-rh.component.html',
  styleUrl: './rapport-rh.component.scss'
})
export class RapportRHComponent implements OnInit {

  question = '';
  isGenerating = false;
  rapportActuel: RapportRH | null = null;
  historiqueRapports: RapportRH[] = [];
  isLoadingHistorique = false;
  errorMessage = '';
  isExportingPDF = false;

  suggestions = [
    '📊 Génère le rapport mensuel des absences',
    '⚠️ Quelles animatrices ont dépassé leur quota ?',
    '🤖 Analyse les décisions automatiques du moteur',
    '📈 Donne-moi un bilan RH complet',
    '⏳ Liste toutes les demandes en attente',
    '👥 Quel est l\'état des animatrices ?',
    '📋 Prépare un rapport pour une réunion de direction',
  ];

  private apiUrl = 'http://localhost:8081/api/admin/rapports';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadHistorique();
  }

  loadHistorique(): void {
    this.isLoadingHistorique = true;
    this.http.get<RapportRH[]>(this.apiUrl).subscribe({
      next: (data) => {
        this.historiqueRapports = data;
        this.isLoadingHistorique = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoadingHistorique = false; }
    });
  }

  utiliserSuggestion(suggestion: string): void {
    this.question = suggestion.replace(/^[^\s]+\s/, '');
  }

  genererRapport(): void {
    if (!this.question.trim()) return;
    this.isGenerating = true;
    this.errorMessage = '';
    this.rapportActuel = null;

    this.http.post<RapportRH>(`${this.apiUrl}/generer`, {
      question: this.question
    }).subscribe({
      next: (rapport) => {
        this.rapportActuel = rapport;
        this.isGenerating = false;
        this.historiqueRapports.unshift(rapport);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la génération. Vérifiez qu\'Ollama est en cours d\'exécution.';
        this.isGenerating = false;
        this.cdr.detectChanges();
      }
    });
  }

  voirRapport(rapport: RapportRH): void {
    this.rapportActuel = rapport;
    this.cdr.detectChanges();
  }

  nouveauRapport(): void {
    this.rapportActuel = null;
    this.question = '';
  }

  /**
   * Télécharger le rapport en PDF via jsPDF
   */
  async telechargerPDF(): Promise<void> {
    if (!this.rapportActuel || !this.rapportActuel.contenu) return;

    this.isExportingPDF = true;

    try {
      // Charger jsPDF dynamiquement
      const { jsPDF } = await import('jspdf' as any);

      const doc = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4'
      });

      const pageWidth = doc.internal.pageSize.getWidth();
      const pageHeight = doc.internal.pageSize.getHeight();
      const margin = 20;
      const maxWidth = pageWidth - (margin * 2);
      let y = margin;

      // ===== EN-TÊTE =====
      doc.setFillColor(79, 70, 229);
      doc.rect(0, 0, pageWidth, 35, 'F');

      doc.setTextColor(255, 255, 255);
      doc.setFontSize(18);
      doc.setFont('helvetica', 'bold');
      doc.text('TinySpring Garderie', margin, 15);

      doc.setFontSize(11);
      doc.setFont('helvetica', 'normal');
      doc.text('Rapport RH — Assistant IA Local', margin, 25);

      // Date à droite
      doc.setFontSize(9);
      const dateStr = this.formatDate(this.rapportActuel.dateGeneration);
      doc.text(dateStr, pageWidth - margin - doc.getTextWidth(dateStr), 25);

      y = 45;

      // ===== TYPE ET PÉRIODE =====
      doc.setTextColor(79, 70, 229);
      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      const badge = `${this.getTypeIcon(this.rapportActuel.typeRapport)} ${this.rapportActuel.typeRapport || 'GENERAL'} — ${this.rapportActuel.periode || ''}`;
      doc.text(badge, margin, y);
      y += 8;

      // ===== QUESTION =====
      doc.setFillColor(248, 250, 252);
      doc.roundedRect(margin, y, maxWidth, 14, 2, 2, 'F');
      doc.setTextColor(100, 116, 139);
      doc.setFontSize(8);
      doc.setFont('helvetica', 'bold');
      doc.text('Question :', margin + 3, y + 5);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(55, 65, 81);
      doc.setFontSize(9);

      const questionTronquee = this.rapportActuel.question.substring(0, 80);
      doc.text(questionTronquee, margin + 22, y + 5);
      y += 20;

      // ===== CONTENU DU RAPPORT =====
      doc.setTextColor(55, 65, 81);

      // Nettoyer le contenu (supprimer les emojis pour PDF)
      const contenuNettoye = this.nettoyerPourPDF(this.rapportActuel.contenu);
      const lignes = contenuNettoye.split('\n');

      for (const ligne of lignes) {
        if (ligne.trim() === '') {
          y += 4;
          continue;
        }

        // Détecter les titres (toutes majuscules ou commence par •)
        const estTitre = ligne.trim() === ligne.trim().toUpperCase() &&
                         ligne.trim().length > 3 &&
                         !ligne.trim().startsWith('•');

        if (estTitre) {
          // Titre de section
          if (y > pageHeight - 30) {
            doc.addPage();
            y = margin;
          }
          y += 3;
          doc.setFont('helvetica', 'bold');
          doc.setFontSize(11);
          doc.setTextColor(79, 70, 229);

          // Ligne décorative sous le titre
          doc.setDrawColor(79, 70, 229);
          doc.setLineWidth(0.3);

          const lignesTexte = doc.splitTextToSize(ligne.trim(), maxWidth);
          doc.text(lignesTexte, margin, y);
          y += lignesTexte.length * 6;
          doc.line(margin, y, margin + maxWidth, y);
          y += 4;

        } else {
          // Texte normal
          doc.setFont('helvetica', 'normal');
          doc.setFontSize(9);
          doc.setTextColor(55, 65, 81);

          const lignesTexte = doc.splitTextToSize(ligne, maxWidth);

          for (const ligneTexte of lignesTexte) {
            if (y > pageHeight - 25) {
              doc.addPage();
              y = margin;
            }
            doc.text(ligneTexte, margin, y);
            y += 5.5;
          }
        }
      }

      // ===== PIED DE PAGE sur toutes les pages =====
      const totalPages = doc.internal.getNumberOfPages();
      for (let i = 1; i <= totalPages; i++) {
        doc.setPage(i);
        doc.setFillColor(248, 250, 252);
        doc.rect(0, pageHeight - 12, pageWidth, 12, 'F');
        doc.setTextColor(148, 163, 184);
        doc.setFontSize(7);
        doc.setFont('helvetica', 'normal');
        doc.text('TinySpring Garderie — Rapport genere par IA locale', margin, pageHeight - 5);
        doc.text(`Page ${i}/${totalPages}`, pageWidth - margin - 15, pageHeight - 5);
      }

      // ===== TÉLÉCHARGER =====
      const nomFichier = `rapport-rh-${this.rapportActuel.typeRapport?.toLowerCase() || 'general'}-${new Date().toISOString().split('T')[0]}.pdf`;
      doc.save(nomFichier);

    } catch (error) {
      console.error('Erreur PDF:', error);
      // Fallback : impression navigateur
      this.imprimerNavigateur();
    } finally {
      this.isExportingPDF = false;
      this.cdr.detectChanges();
    }
  }

  /**
   * Fallback : impression via le navigateur
   */
  imprimerNavigateur(): void {
    window.print();
  }

  /**
   * Nettoyer le texte pour le PDF (supprimer emojis)
   */
  private nettoyerPourPDF(texte: string): string {
    return texte
      .replace(/[\u{1F300}-\u{1F9FF}]/gu, '')
      .replace(/[\u{2600}-\u{26FF}]/gu, '')
      .replace(/[\u{2700}-\u{27BF}]/gu, '')
      .replace(/[✅❌⚠️⏳🚨💡📊📋📅📈📚👥🤖🌸🆕🔢]/g, '')
      .replace(/  +/g, ' ')
      .trim();
  }

  getTypeIcon(type?: string): string {
    switch (type) {
      case 'MENSUEL':     return 'Mensuel';
      case 'TRIMESTRIEL': return 'Trimestriel';
      case 'ANNUEL':      return 'Annuel';
      case 'ABSENCES':    return 'Absences';
      case 'FORMATIONS':  return 'Formations';
      case 'ANIMATRICES': return 'Animatrices';
      case 'ALERTES':     return 'Alertes';
      default:            return 'General';
    }
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatContenu(contenu?: string): string {
    if (!contenu) return '';
    return contenu
      .replace(/\n/g, '<br>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/^#{1,3}\s(.+)$/gm, '<h4>$1</h4>');
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.genererRapport();
    }
  }
}