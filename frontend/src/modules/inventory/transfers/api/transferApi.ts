import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type TransferStatus = "Draft" | "Approved" | "Partially Complete" | "Complete";

export interface TransferLine {
  id: string;
  lineNo: number;
  productId: string;
  variantId: string | null;
  qtySent: string;
  qtyReceived: string | null;
  qtyShort: string | null;
  qtyDamaged: string | null;
  discrepancyReason: string | null;
}

export interface TransferLineInput {
  productId: string;
  qtySent: number;
  variantId?: string;
}

export interface TransferReceiveLineInput {
  lineId: string;
  qtyReceived: number;
  qtyShort?: number;
  qtyDamaged?: number;
  discrepancyReason?: string;
}

/** Stock transfer wire type — mirrors backend TransferDtos.TransferResponse. */
export interface StockTransfer {
  id: string;
  number: string;
  sourceLocationId: string;
  destinationLocationId: string;
  status: TransferStatus;
  notes: string | null;
  transferDate: string | null;
  confirmedAt: string | null;
  receivedAt: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  lines: TransferLine[];
}

export interface TransferCreate {
  sourceLocationId: string;
  destinationLocationId: string;
  notes?: string;
  transferDate?: string;
  lines: TransferLineInput[];
  autoComplete: boolean;
}

const BASE = "/inventory/transfers";

export const transferApi = {
  list: (q: PageQuery) => http.get<Paginated<StockTransfer>>(BASE, { params: q }).then((r) => r.data),
  create: (b: TransferCreate) => http.post<StockTransfer>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  confirm: (id: string) => http.post<StockTransfer>(`${BASE}/${id}/confirm`).then((r) => r.data),
  receive: (id: string, lines: TransferReceiveLineInput[]) =>
    http.post<StockTransfer>(`${BASE}/${id}/receive`, { lines }).then((r) => r.data),
  complete: (id: string) => http.post<StockTransfer>(`${BASE}/${id}/complete`).then((r) => r.data),
};
