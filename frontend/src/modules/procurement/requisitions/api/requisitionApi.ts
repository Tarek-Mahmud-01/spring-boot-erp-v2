import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";
import type { PurchaseOrder } from "@/modules/procurement/orders/api/purchaseOrderApi";

export type PrStatus = "Draft" | "Submitted" | "Under Review" | "Sent to Supplier" | "Converted" | "Rejected";

export interface PrLine {
  id: string;
  lineNo: number;
  productId: string;
  variantId: string | null;
  qty: string;
  uomId: string | null;
  preferredSupplierId: string | null;
  description: string | null;
  unitPriceAmount: number;
  unitPriceCurrency: string | null;
  discountPercent: string;
  taxCodeId: string | null;
  lineTotalAmount: number;
  status: string;
}

export interface PrLineInput {
  productId: string;
  qty: number;
  description?: string;
  unitPriceAmount: number;
  unitPriceCurrency: string;
  discountPercent: number;
  lineTotalAmount: number;
}

/** Purchase requisition wire type — mirrors backend PrDtos.PrResponse. */
export interface PurchaseRequisition {
  id: string;
  number: string;
  locationId: string;
  requesterUserId: string;
  supplierId: string | null;
  currency: string;
  neededByDate: string | null;
  requestDate: string | null;
  totalAmount: number;
  notes: string | null;
  status: PrStatus;
  assignedBuyerId: string | null;
  rejectionReason: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lines: PrLine[];
}

export interface PrCreate {
  locationId: string;
  supplierId?: string;
  currency: string;
  neededByDate?: string;
  notes?: string;
  totalAmount: number;
  lines: PrLineInput[];
}

const BASE = "/procurement/purchase-requisitions";

export const requisitionApi = {
  list: (q: PageQuery) => http.get<Paginated<PurchaseRequisition>>(BASE, { params: q }).then((r) => r.data),
  create: (b: PrCreate) => http.post<PurchaseRequisition>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  submit: (id: string) => http.post<PurchaseRequisition>(`${BASE}/${id}/submit`).then((r) => r.data),
  transition: (id: string, toStatus: PrStatus, reason?: string) =>
    http.post<PurchaseRequisition>(`${BASE}/${id}/transition`, { toStatus, reason }).then((r) => r.data),
  convertToPo: (id: string) => http.post<PurchaseOrder>(`${BASE}/${id}/convert-to-po`).then((r) => r.data),
};
