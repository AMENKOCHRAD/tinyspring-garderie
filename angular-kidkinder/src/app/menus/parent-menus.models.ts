export type MenuStatus = 'DRAFT' | 'PUBLISHED' | 'TEMPLATE' | string;
export type MealType = 'ENTREE' | 'PLAT_PRINCIPAL' | 'DESSERT' | 'GOUTER' | string;
export type MenuSectionKey = 'ENTREE' | 'PLAT_PRINCIPAL' | 'ACCOMPAGNEMENT' | 'DESSERT' | 'GOUTER';

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
  displayDayShort: string;
  displayDayLong: string;
  displayDate: string;
  displayDateLong: string;
  dayColor: string;
  isToday: boolean;
  sections: Record<MenuSectionKey, MenuDish[]>;
  summaryDish: string;
  allergens: string[];
  conflictFlags: string[];
}

export interface DecoratedWeeklyMenu extends WeeklyMenu {
  weekLabel: string;
  weekRangeLabel: string;
  isCurrentWeek: boolean;
  visibleDailyMenus: DecoratedDailyMenu[];
  todayMenu: DecoratedDailyMenu | null;
}

export interface ParentMenusData {
  weeklyMenus: DecoratedWeeklyMenu[];
  currentWeekIndex: number;
}
