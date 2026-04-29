export type MarketingSolutionType = 'DESCRIPTION' | 'PROMOTION' | 'TAGS';

export interface MarketingSolution {
  type: MarketingSolutionType | string;
  titre: string;
  impact_estime: 'Fort' | 'Moyen' | 'Faible' | string;
  automatisable: boolean;
}

export interface MarketingEmailSuggestion {
  objet: string;
  corps: string;
}

export interface MarketingAnalysis {
  produit_id: number;
  produit_nom: string;
  stock_actuel: number;
  ventes_recentes: number;
  diagnostic: string | Record<string, unknown> | null;
  score_urgence: number;
  solutions: MarketingSolution[];
  nouvelle_description: string;
  tags_suggeres: string[];
  promotion_recommandee: number;
  message_email: MarketingEmailSuggestion | null;
}

export interface MarketingAutomationRequest {
  types: string[];
  suggestion: Record<string, unknown>;
}

export interface MarketingAutomationResponse {
  actions_effectuees?: Array<string | Record<string, unknown>>;
  [key: string]: unknown;
}
