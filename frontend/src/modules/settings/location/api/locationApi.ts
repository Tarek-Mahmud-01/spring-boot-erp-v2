import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** Location wire types — mirror backend LocationDtos. */
export interface Address {
  street: string;
  city: string;
  region: string;
  postcode: string;
  country: string;
}

export interface Location {
  id: string; // ULID publicId
  companyId: string;
  code: string;
  name: string;
  type: string;
  timezone: string;
  address: Address;
  phone: string;
  publicEmail: string;
  defaultPriceListId: string;
  defaultTaxCodeId: string;
  status: string;
  priceDisplayMode: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface LocationCreate {
  companyId: string;
  code: string;
  name: string;
  type: string;
  timezone: string;
  address: Address;
  phone?: string;
  publicEmail?: string;
  defaultPriceListId?: string;
  defaultTaxCodeId?: string;
  priceDisplayMode?: string;
}

export interface LocationUpdate {
  code?: string;
  name?: string;
  type?: string;
  timezone?: string;
  address?: Address;
  phone?: string;
  publicEmail?: string;
  defaultPriceListId?: string;
  defaultTaxCodeId?: string;
  priceDisplayMode?: string;
  version: number;
}

const BASE = "/locations";

/** Axios calls for the location feature (ARCHITECTURE.md §3 module api/). */
export const locationApi = {
  list: (query: PageQuery) => http.get<Paginated<Location>>(BASE, { params: query }).then((r) => r.data),
  get: (id: string) => http.get<Location>(`${BASE}/${id}`).then((r) => r.data),
  create: (body: LocationCreate) => http.post<Location>(BASE, body).then((r) => r.data),
  update: (id: string, body: LocationUpdate) => http.patch<Location>(`${BASE}/${id}`, body).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  activate: (id: string) => http.post<Location>(`${BASE}/${id}/activate`).then((r) => r.data),
  deactivate: (id: string) => http.post<Location>(`${BASE}/${id}/deactivate`).then((r) => r.data),
};
