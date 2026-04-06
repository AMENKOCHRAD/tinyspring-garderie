import { Dish } from './dish.model';

export type MenuDayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface DailyMenu {
  id: number;
  weeklyMenuId: number;
  menuDate: string | null;
  dayOfWeek: MenuDayOfWeek;
  isVisibleToParents: boolean;
  publishedAt?: string | null;
  dishes: Dish[];
}
