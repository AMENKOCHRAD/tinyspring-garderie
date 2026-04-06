import { DailyMenu } from './daily-menu.model';

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
}
