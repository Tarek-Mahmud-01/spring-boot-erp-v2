import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** Numbering-rule wire types — mirror backend NumberingRuleDtos (ENT-006). */
export const DOCUMENT_TYPES = [
  "INVOICE",
  "PO",
  "PR",
  "GRN",
  "BILL",
  "RECEIPT",
  "DELIVERY",
  "QUOTE",
  "SO",
  "JOURNAL",
] as const;
export type DocumentType = (typeof DOCUMENT_TYPES)[number];

export const RESET_CADENCES = ["NEVER", "YEARLY", "MONTHLY"] as const;
export type ResetCadence = (typeof RESET_CADENCES)[number];

export interface NumberingRule {
  id: string; // ULID publicId
  companyId: string;
  documentType: DocumentType;
  prefix: string;
  padding: number;
  resetCadence: ResetCadence;
  startValue: number;
  currentValue: number;
  currentWindowKey: string;
  totalIssued: number;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface NumberingRuleCreate {
  companyId: string;
  documentType: DocumentType;
  prefix?: string;
  padding: number;
  resetCadence: ResetCadence;
  startValue: number;
}

export interface NumberingRuleUpdate {
  prefix?: string;
  padding?: number;
  resetCadence?: ResetCadence;
  startValue?: number;
  version: number;
}

export interface NumberingAllocateResponse {
  number: string;
  sequenceValue: number;
  windowKey: string;
}

const BASE = "/numbering-rules";

/** Axios calls for the numbering-rule feature (ARCHITECTURE.md §3 module api/). */
export const numberingApi = {
  list: (query: PageQuery) =>
    http.get<Paginated<NumberingRule>>(BASE, { params: query }).then((r) => r.data),
  create: (body: NumberingRuleCreate) => http.post<NumberingRule>(BASE, body).then((r) => r.data),
  update: (id: string, body: NumberingRuleUpdate) =>
    http.patch<NumberingRule>(`${BASE}/${id}`, body).then((r) => r.data),
  allocate: (id: string, documentDate: string) =>
    http
      .post<NumberingAllocateResponse>(`${BASE}/${id}/allocate`, { documentDate })
      .then((r) => r.data),
};
