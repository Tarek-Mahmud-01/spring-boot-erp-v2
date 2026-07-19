import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type PoStatus =
  | "Draft"
  | "Submitted"
  | "Approved"
  | "Sent"
  | "Partially Received"
  | "Received"
  | "Closed"
  | "Cancelled";

export interface PoLine {
  id: string;
  lineNo: number;
  productId: string;
  variantId: string | null;
  variantName: string | null;
  qtyOrdered: string;
  qtyReceivedTotal: string;
  uomId: string | null;
  unitPriceAmount: number;
  unitPriceCurrency: string | null;
  discountPercent: string;
  taxCodeId: string | null;
  taxRatePercent: string | null;
  lineTotalAmount: number;
  lineTotalCurrency: string | null;
  lineStatus: string;
}

export interface PoLineInput {
  productId: string;
  variantId?: string;
  qtyOrdered: number;
  uomId?: string;
  unitPriceAmount: number;
  unitPriceCurrency: string;
  discountPercent: number;
  taxCodeId?: string;
}

/** Purchase order wire type — mirrors backend PoDtos.PoResponse. */
export interface PurchaseOrder {
  id: string;
  number: string;
  supplierId: string;
  locationId: string;
  poDate: string;
  expectedDeliveryDate: string | null;
  currency: string;
  exchangeRate: string | null;
  paymentTerms: string | null;
  sourcePrId: string | null;
  status: PoStatus;
  closeReason: string | null;
  poVersion: number;
  invoiceDiscountType: string | null;
  invoiceDiscountValue: string | null;
  isDirect: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lines: PoLine[];
}

export interface PoCreate {
  supplierId: string;
  locationId: string;
  poDate: string;
  expectedDeliveryDate?: string;
  currency: string;
  notes?: string;
  isDirect: boolean;
  lines: PoLineInput[];
}

const BASE = "/procurement/purchase-orders";

export const purchaseOrderApi = {
  list: (q: PageQuery) => http.get<Paginated<PurchaseOrder>>(BASE, { params: q }).then((r) => r.data),
  get: (id: string) => http.get<PurchaseOrder>(`${BASE}/${id}`).then((r) => r.data),
  create: (b: PoCreate) => http.post<PurchaseOrder>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  transition: (id: string, toStatus: PoStatus, reason?: string) =>
    http.post<PurchaseOrder>(`${BASE}/${id}/transition`, { toStatus, reason }).then((r) => r.data),
};
