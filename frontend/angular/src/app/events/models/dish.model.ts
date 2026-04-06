export type MealCategory = 'STARTER' | 'MAIN' | 'SIDE' | 'DESSERT' | 'SNACK';

export interface Dish {
  id: number;
  dailyMenuId: number;
  mealType: MealCategory;
  name: string;
  description?: string;
  photoUrl?: string;
  allergens?: string;
  allergenConflictFlags?: string;
}
