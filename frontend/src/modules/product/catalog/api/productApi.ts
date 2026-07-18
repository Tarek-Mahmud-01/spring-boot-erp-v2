import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type LifecycleState = "DRAFT" | "ACTIVE" | "ON_HOLD" | "RUN_OUT" | "DISCONTINUED";

/** Product wire types — mirror backend ProductDtos (money as minor units). */
export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  categoryId: string;
  uomId: string;
  taxCodeId: string | null;
  brand: string | null;
  supplierId: string | null;
  costAmount: number;
  costCurrency: string;
  sellAmount: number;
  sellCurrency: string;
  weightGrams: number | null;
  hasVariants: boolean;
  isActive: boolean;
  lifecycleState: LifecycleState;
  restrictionAge18: boolean;
  restrictionAge21: boolean;
  soldByWeight: boolean;
  plu: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductCreate {
  sku: string;
  name: string;
  categoryId: string;
  uomId: string;
  sellAmount: number;
  costAmount: number;
  costCurrency: string;
  sellCurrency: string;
  description?: string;
  brand?: string;
  taxCodeId?: string;
  hasVariants?: boolean;
  soldByWeight?: boolean;
}

export interface ProductUpdate {
  name?: string;
  description?: string;
  brand?: string;
  categoryId?: string;
  uomId?: string;
  taxCodeId?: string;
  costAmount?: number;
  sellAmount?: number;
  costCurrency?: string;
  sellCurrency?: string;
  soldByWeight?: boolean;
  version: number;
}

const BASE = "/products";

/** Axios calls for the product catalog feature. */
export const productApi = {
  list: (q: PageQuery) => http.get<Paginated<Product>>(BASE, { params: q }).then((r) => r.data),
  create: (b: ProductCreate) => http.post<Product>(BASE, b).then((r) => r.data),
  update: (id: string, b: ProductUpdate) => http.patch<Product>(`${BASE}/${id}`, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  transition: (id: string, toState: LifecycleState, reason?: string) =>
    http.post<Product>(`${BASE}/${id}/lifecycle`, { toState, reason }).then((r) => r.data),
};
