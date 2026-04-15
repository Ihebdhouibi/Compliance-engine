import { MediaModel } from './media.model';
import { CompanyInfo } from './company-info.model';

export type RoleEnum = 'ROLE_ADMIN' | 'ROLE_AUDITOR' | 'ROLE_USER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  name: string;
  phoneNumber: string;
  gender?: string;
  role: RoleEnum;
  profileimage?: MediaModel;
  active: boolean;
  deleted: boolean;
  timestamp: Date;
  companyInfo?: CompanyInfo;
}

export interface PageResponse<T> {
  result:      T[];
  currentPage: number;
  totalItems:  number;
  totalPages:  number;
}