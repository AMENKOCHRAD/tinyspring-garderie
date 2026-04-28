import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../services/ai.service';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

@Component({
  selector: 'app-ai-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-report.component.html'
})
export class AiReportComponent {
  childName: string = '';
  animatorNotes: string = '';
  currentDate: Date = new Date();
  expectedLengthWords: number = 50;

  isGenerating = false;
  generatedReport = '';
  errorMessage = '';

  constructor(
    private aiService: AiService,
    private cdr: ChangeDetectorRef
  ) {}

  generateReport(): void {
    if (!this.childName || !this.animatorNotes) {
      this.errorMessage = "Veuillez remplir le nom de l'enfant et vos notes.";
      return;
    }

    this.isGenerating = true;
    this.errorMessage = '';
    this.generatedReport = '';

    const enrichedNotes = `${this.animatorNotes} (Rapport désiré : professionnel, rassurant)`;

    this.aiService.generateDailyReport(this.childName, enrichedNotes).subscribe({
      next: (res) => {
        this.generatedReport = res.response;
        this.isGenerating = false;
        this.cdr.detectChanges(); // Force l'UI à se rafraîchir immédiatement
      },
      error: (err) => {
        this.errorMessage = "Impossible de générer le rapport. Veuillez réessayer.";
        this.isGenerating = false;
        this.cdr.detectChanges(); // Force l'UI à se rafraîchir immédiatement
      }
    });
  }

  copyToClipboard(): void {
    if (this.generatedReport) {
      navigator.clipboard.writeText(this.generatedReport).then(() => {
        alert('Rapport copié dans le presse-papier !');
      });
    }
  }

  downloadPDF(): void {
    const data = document.getElementById('pdf-report-content');
    if (data) {
      html2canvas(data, { scale: 2 }).then(canvas => {
        const imgWidth = 208;
        const pageHeight = 295;
        const imgHeight = canvas.height * imgWidth / canvas.width;
        
        const pdf = new jsPDF('p', 'mm', 'a4');
        const imgData = canvas.toDataURL('image/png');
        
        pdf.addImage(imgData, 'PNG', 0, 0, imgWidth, imgHeight);
        pdf.save(`Rapport_Journalier_${this.childName || 'Enfant'}.pdf`);
      });
    }
  }
}
