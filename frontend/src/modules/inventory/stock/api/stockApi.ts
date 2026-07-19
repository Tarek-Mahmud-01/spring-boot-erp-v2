import { http } from "@/shared/services/http";

export type StockStatus = "AVAILABLE" | "RESERVED" | "QUARANTINE" | "IN_TRANSIT";

/** On-hand bucket — mirrors backend StockOnHandDtos.StockOnHandResponse. */
export interface StockOnHand {
  productId: string;
  variantId: string | null;
  locationId: string;
  status: StockStatus;
  qtyOnHand: string;
  unitCostAmount: number;
  unitCostCurrency: string | null;
  totalValue: number;
  updatedAt: string;
}

export interface StockQuery {
  locationId?: string;
  productId?: string;
  variantId?: string;
}

const BASE = "/inventory";

export const stockApi = {
  onHand: (q: StockQuery) => http.get<StockOnHand[]>(`${BASE}/stock`, { params: q }).then((r) => r.data),
};
