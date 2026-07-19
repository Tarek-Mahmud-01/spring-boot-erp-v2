import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type AdjustmentStatus = "Draft" | "Pending Approval" | "Approved" | "Posted" | "Reversed";

export interface AdjustmentLine {
  id: string;
  lineNo: number;
  productId: string;
  variantId: string | null;
  variantName: string | null;
  qtyCounted: string;
  qtyOnHandAtCount: string;
  qtyVariance: string;
  writeOffReason: string | null;
  unitCostAmount: number;
  unitCostCurrency: string | null;
}

export interface AdjustmentLineInput {
  productId: string;
  qtyDelta: number;
  variantId?: string;
  writeOffReason?: string;
  unitCostAmount?: number;
  unitCostCurrency?: string;
}

/** Stock adjustment wire type — mirrors backend AdjustmentDtos.AdjustmentResponse. */
export interface StockAdjustment {
  id: string;
  number: string;
  locationId: string;
  reason: string;
  notes: string | null;
  status: AdjustmentStatus;
  thresholdExceeded: boolean;
  approverId: string | null;
  approvedAt: string | null;
  postedAt: string | null;
  varianceAccountId: string | null;
  journalEntryId: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lines: AdjustmentLine[];
}

export interface AdjustmentCreate {
  locationId: string;
  reason: string;
  notes?: string;
  varianceAccountId?: string;
  lines: AdjustmentLineInput[];
  autoComplete: boolean;
}

const BASE = "/inventory/adjustments";

export const adjustmentApi = {
  list: (q: PageQuery) => http.get<Paginated<StockAdjustment>>(BASE, { params: q }).then((r) => r.data),
  create: (b: AdjustmentCreate) => http.post<StockAdjustment>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  approve: (id: string) => http.post<StockAdjustment>(`${BASE}/${id}/approve`).then((r) => r.data),
  post: (id: string) => http.post<StockAdjustment>(`${BASE}/${id}/post`).then((r) => r.data),
};
