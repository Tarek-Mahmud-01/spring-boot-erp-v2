import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type GrnStatus = "Draft" | "Approved" | "Partially Received" | "Received" | "Confirmed";

export interface GrnLine {
  id: string;
  poLineId: string | null;
  variantId: string | null;
  qtyReceived: string;
  qtyDiscrepancy: string | null;
  discrepancyType: string | null;
  discrepancyNote: string | null;
  batchNo: string | null;
  serialNo: string | null;
  expiryDate: string | null;
  supplierBarcode: string | null;
}

export interface GrnLineInput {
  poLineId?: string;
  qtyReceived: number;
  qtyDiscrepancy?: number;
  discrepancyType?: string;
  batchNo?: string;
}

/** Goods receipt (GRN) wire type — mirrors backend ReceiptDtos.GrnResponse. */
export interface GoodsReceipt {
  id: string;
  number: string;
  poId: string;
  locationId: string;
  receivedAt: string;
  receivedBy: string | null;
  status: GrnStatus;
  autoReceipt: boolean;
  deliveryNoteNo: string | null;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lines: GrnLine[];
}

export interface GrnCreate {
  poId: string;
  locationId: string;
  receivedAt: string;
  autoReceipt: boolean;
  confirm: boolean;
  deliveryNoteNo?: string;
  notes?: string;
  lines: GrnLineInput[];
}

const BASE = "/procurement/receipts";

export const receiptApi = {
  list: (q: PageQuery) => http.get<Paginated<GoodsReceipt>>(BASE, { params: q }).then((r) => r.data),
  create: (b: GrnCreate) => http.post<GoodsReceipt>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  transition: (id: string, toStatus: GrnStatus) =>
    http.post<GoodsReceipt>(`${BASE}/${id}/transition`, { toStatus }).then((r) => r.data),
  confirm: (id: string) => http.post<GoodsReceipt>(`${BASE}/${id}/confirm`).then((r) => r.data),
};
