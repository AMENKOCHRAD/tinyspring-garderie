export interface UserCategorieScoreDto {
  id: number;
  userId: number;
  userNom: string;
  userEmail: string;
  categorieId: number;
  categorieNom: string;
  score: number;
  nbInteractions: number;
  derniereInteraction: string;
}
