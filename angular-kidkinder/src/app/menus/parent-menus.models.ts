export type MenuStatus = 'DRAFT' | 'PUBLISHED' | 'TEMPLATE' | string;
export type MealType = 'ENTREE' | 'PLAT_PRINCIPAL' | 'DESSERT' | 'GOUTER' | string;

export interface MenuDish {
  id: number;
  dailyMenuId: number | null;
  mealType: MealType;
  name: string;
  description: string;
  allergens: string;
  allergenConflictFlags: string;
}

export interface DailyMenu {
  id: number;
  weeklyMenuId: number | null;
  menuDate: string | null;
  dayOfWeek: string | null;
  isVisibleToParents: boolean;
  publishedAt: string | null;
  dishes: MenuDish[];
}

export interface WeeklyMenu {
  id: number;
  title: string;
  weekStartDate: string | null;
  weekEndDate: string | null;
  status: MenuStatus;
  isTemplate: boolean;
  templateName: string | null;
  createdAt: string | null;
  dailyMenus: DailyMenu[];
}

export interface DecoratedDailyMenu extends DailyMenu {
  displayDay: string;
  displayDate: string;
  dishesByMealType: Record<string, MenuDish[]>;
}

export interface ParentMenusData {
  weeklyMenus: WeeklyMenu[];
  visibleDailyMenus: DecoratedDailyMenu[];
  todayMenu: DecoratedDailyMenu | null;
  currentWeekMenu: WeeklyMenu | null;
}
