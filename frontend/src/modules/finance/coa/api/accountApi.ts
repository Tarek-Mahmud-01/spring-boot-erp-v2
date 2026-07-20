import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type AccountType = "ASSET" | "LIABILITY" | "EQUITY" | "INCOME" | "EXPENSE";
export type AccountPostingType = "POSTING" | "HEADER";
export type AccountStatus = "active" | "inactive";

/** Chart-of-accounts node — mirrors backend AccountDtos.AccountResponse. */
export interface Account {
  id: string;
  companyId: string;
  code: string;
  name: string;
  type: AccountType;
  parentId: string | null;
  postingType: AccountPostingType;
  currency: string | null;
  status: AccountStatus;
  lft: number;
  rgt: number;
  depth: number;
  openingDebitAmount: number;
  openingCreditAmount: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface AccountCreate {
  companyId: string;
  code: string;
  name: string;
  type: AccountType;
  parentId?: string;
  postingType?: AccountPostingType;
  currency?: string;
}

export interface AccountUpdate {
  name?: string;
  postingType?: AccountPostingType;
  currency?: string;
  status?: AccountStatus;
  version: number;
}

const BASE = "/finance/accounts";

export const accountApi = {
  list: (companyId: string, q: PageQuery) =>
    http.get<Paginated<Account>>(BASE, { params: { companyId, ...q } }).then((r) => r.data),
  create: (b: AccountCreate) => http.post<Account>(BASE, b).then((r) => r.data),
  update: (id: string, b: AccountUpdate) => http.patch<Account>(`${BASE}/${id}`, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
};
