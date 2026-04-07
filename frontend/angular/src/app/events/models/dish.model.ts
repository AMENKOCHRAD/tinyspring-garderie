export type MealCategory = 'ENTREE' | 'PLAT_PRINCIPAL' | 'DESSERT' | 'GOUTER';

export interface Dish {
  id: number;
  dailyMenuId: number;
  mealType: MealCategory;
  name: string;
  description?: string;
  allergens?: string;
  allergenConflictFlags?: string;
}
