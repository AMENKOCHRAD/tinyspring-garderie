import { DailyMenu } from './daily-menu.model';
import { MenuDayOfWeek } from './daily-menu.model';
import { MealCategory } from './dish.model';

export type WeeklyMenuStatus = 'DRAFT' | 'PUBLISHED' | 'TEMPLATE';

export interface WeeklyMenu {
  id: number;
  title: string;
  weekStartDate: string | null;
  weekEndDate: string | null;
  status: WeeklyMenuStatus;
  isTemplate: boolean;
  templateName?: string | null;
  createdAt?: string | null;
  dailyMenus: DailyMenu[];
}

export interface WeeklyMenuRequest {
  title: string;
  weekStartDate: string | null;
  weekEndDate: string | null;
  status: WeeklyMenuStatus;
  isTemplate: boolean;
  templateName?: string | null;
  dailyMenus?: WeeklyMenuDailyRequest[];
}

export interface WeeklyMenuDailyRequest {
  weeklyMenuId?: number | null;
  menuDate: string | null;
  dayOfWeek: MenuDayOfWeek | null;
  isVisibleToParents: boolean;
  publishedAt?: string | null;
  dishes?: WeeklyMenuDishRequest[];
}

export interface WeeklyMenuDishRequest {
  dailyMenuId?: number | null;
  mealType: MealCategory;
  name: string;
  description?: string;
  allergens?: string;
}
