import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type BillStatus = "Draft" | "Received" | "Approved" | "Paid" | "Partially Received" | "Cancelled" | "Invoiced Not Received";

export interface BillLine {
  id: string;
  poLineId: string | null;
  grnLineId: string | null;
  productId: string;
  variantId: string | null;
  description: string | null;
  qty: string;
  unitPriceAmount: number;
  taxCodeId: string | null;
  lineTotalAmount: number;
  matchStatus: string | null;
  isCapitalItem: boolean;
}

export interface BillLineInput {
  productId: string;
  poLineId?: string;
  grnLineId?: string;
  description?: string;
  qty: number;
  unitPriceAmount: number;
  taxCodeId?: string;
  isCapitalItem: boolean;
}

/** Supplier bill wire type — mirrors backend BillDtos.BillResponse. */
export interface SupplierBill {
  id: string;
  number: string;
  supplierId: string;
  poId: string | null;
  supplierBillNo: string | null;
  billDate: string;
  dueDate: string | null;
  currency: string;
  subtotalAmount: number;
  taxAmount: number;
  totalAmount: number;
  status: BillStatus;
  matchStatus: string | null;
  notes: string | null;
  grnIds: string[];
  poIds: string[];
  lines: BillLine[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface BillCreate {
  supplierId: string;
  poId?: string;
  grnIds?: string[];
  supplierBillNo?: string;
  billDate: string;
  dueDate?: string;
  currency: string;
  notes?: string;
  lines: BillLineInput[];
}

const BASE = "/procurement/bills";

export const billApi = {
  list: (q: PageQuery) => http.get<Paginated<SupplierBill>>(BASE, { params: q }).then((r) => r.data),
  create: (b: BillCreate) => http.post<SupplierBill>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  approve: (id: string, version?: number) =>
    http.post<SupplierBill>(`${BASE}/${id}/approve`, null, { params: { version } }).then((r) => r.data),
};
