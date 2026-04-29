export type TypeAbsenceConge = 'ABSENCE' | 'CONGE_ANNUEL' | 'CONGE_MALADIE' | 'CONGE_MATERNITE';
export type StatutAbsenceConge = 'EN_ATTENTE' | 'APPROUVE' | 'REFUSE';

export interface AbsenceConge {
  id?: number;
  animatriceId: number;
  animatriceNom?: string;
  animatricePrenom?: string;
  type: TypeAbsenceConge;
  dateDebut: string;
  dateFin: string;
  motif?: string;
  statut?: StatutAbsenceConge;
  nbJours?: number;
}