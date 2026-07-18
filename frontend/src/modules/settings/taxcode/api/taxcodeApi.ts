import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** GST treatment enum — mirrors backend GstTreatment. */
export const GST_TREATMENTS = ["STANDARD", "GST_FREE", "EXPORT", "INPUT_TAXED"] as const;
export type GstTreatment = (typeof GST_TREATMENTS)[number];

/** Tax code wire types — mirror backend TaxCodeDtos. */
export interface TaxCode {
  id: string; // ULID publicId
  companyId: string;
  code: string;
  description: string;
  ratePercent: number;
  inclusive: boolean;
  exempt: boolean;
  gstTreatment: string;
  status: string;
  effectiveFrom: string; // YYYY-MM-DD
  effectiveTo: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaxCodeCreate {
  companyId: string;
  code: string;
  description: string;
  ratePercent: number;
  inclusive: boolean;
  exempt: boolean;
  gstTreatment: string;
  effectiveFrom: string; // YYYY-MM-DD
  effectiveTo?: string;
}

export interface TaxCodeUpdate {
  description?: string;
  ratePercent?: number;
  inclusive?: boolean;
  exempt?: boolean;
  gstTreatment?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  version: number;
}

const BASE = "/tax-codes";

/** Axios calls for the tax code feature (ARCHITECTURE.md §3 module api/). */
export const taxcodeApi = {
  list: (query: PageQuery) => http.get<Paginated<TaxCode>>(BASE, { params: query }).then((r) => r.data),
  get: (id: string) => http.get<TaxCode>(`${BASE}/${id}`).then((r) => r.data),
  create: (body: TaxCodeCreate) => http.post<TaxCode>(BASE, body).then((r) => r.data),
  update: (id: string, body: TaxCodeUpdate) => http.patch<TaxCode>(`${BASE}/${id}`, body).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
};
