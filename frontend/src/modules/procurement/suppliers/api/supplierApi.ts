import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type SupplierStatus = "Active" | "Inactive" | "Blocked";
export type SupplierType = "GOODS" | "SERVICES" | "BOTH";

/** Supplier wire type — mirrors backend SupplierDtos.SupplierResponse. */
export interface Supplier {
  id: string;
  code: string;
  name: string;
  type: SupplierType;
  locationId: string | null;
  contact: Record<string, unknown> | null;
  address: Record<string, unknown> | null;
  paymentTerms: string | null;
  defaultCurrency: string | null;
  taxRegistrationNo: string | null;
  abn: string | null;
  creditLimitAmount: number;
  creditLimitCurrency: string | null;
  openingBalanceAmount: number;
  openingBalanceCurrency: string | null;
  openingBalanceSide: "DEBIT" | "CREDIT" | null;
  status: SupplierStatus;
  displayStatusTone: string | null;
  blockReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierCreate {
  name: string;
  type: SupplierType;
  locationId?: string;
  paymentTerms?: string;
  defaultCurrency?: string;
  taxRegistrationNo?: string;
  abn?: string;
  creditLimitAmount?: number;
  creditLimitCurrency?: string;
}

export interface SupplierUpdate {
  name?: string;
  type?: SupplierType;
  locationId?: string;
  paymentTerms?: string;
  defaultCurrency?: string;
  taxRegistrationNo?: string;
  abn?: string;
  creditLimitAmount?: number;
  creditLimitCurrency?: string;
  version: number;
}

const BASE = "/procurement/suppliers";

export const supplierApi = {
  list: (q: PageQuery) => http.get<Paginated<Supplier>>(BASE, { params: q }).then((r) => r.data),
  create: (b: SupplierCreate) => http.post<Supplier>(BASE, b).then((r) => r.data),
  update: (id: string, b: SupplierUpdate) => http.patch<Supplier>(`${BASE}/${id}`, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  setStatus: (id: string, status: SupplierStatus, blockReason?: string) =>
    http.patch<Supplier>(`${BASE}/${id}/status`, { status, blockReason }).then((r) => r.data),
};
