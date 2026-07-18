import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** Currency wire types — mirror backend CurrencyDtos. */
export interface Currency {
  id: string; // ULID publicId
  code: string;
  name: string;
  shortName: string;
  country: string;
  symbol: string;
  decimalPlaces: number;
  isDefault: boolean;
  isActive: boolean;
  status: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CurrencyCreate {
  code: string;
  name: string;
  shortName: string;
  country: string;
  symbol: string;
  decimalPlaces: number;
  isActive: boolean;
}

export interface CurrencyUpdate {
  name?: string;
  shortName?: string;
  country?: string;
  symbol?: string;
  decimalPlaces?: number;
  isActive?: boolean;
  version: number;
}

const BASE = "/currencies";

/** Axios calls for the currency feature (ARCHITECTURE.md §3 module api/). */
export const currencyApi = {
  list: (query: PageQuery) => http.get<Paginated<Currency>>(BASE, { params: query }).then((r) => r.data),
  create: (body: CurrencyCreate) => http.post<Currency>(BASE, body).then((r) => r.data),
  update: (id: string, body: CurrencyUpdate) => http.patch<Currency>(`${BASE}/${id}`, body).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  setDefault: (id: string) => http.post<Currency>(`${BASE}/${id}/set-default`).then((r) => r.data),
};
