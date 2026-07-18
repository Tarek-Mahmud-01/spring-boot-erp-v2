import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** Compliance profile — mirrors backend ComplianceProfile enum. */
export type ComplianceProfile = "NONE" | "AU";

/** BAS lodgement cadence — mirrors backend BasPeriod enum. */
export type BasPeriod = "MONTHLY" | "QUARTERLY" | "ANNUAL";

/**
 * Company wire types — mirror backend CompanyDtos (ENT-001). The company is a
 * singleton in this single-tenant app; the list endpoint returns one record.
 */
export interface Company {
  id: string; // ULID publicId
  code: string;
  legalName: string;
  tradingName: string | null;
  country: string;
  baseCurrency: string;
  taxRegistrationNo: string | null;
  taxRegistered: boolean;
  taxRegistrationDate: string | null; // ISO date (YYYY-MM-DD)
  fiscalYearStart: string; // "MM-DD"
  primary: boolean;
  status: string; // "active" | "inactive"
  complianceProfile: ComplianceProfile;
  abn: string | null;
  acn: string | null;
  gstRegistrationDate: string | null; // ISO date
  basPeriod: BasPeriod | null;
  logoUrl: string | null;
  invoiceLayout: Record<string, unknown> | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** PATCH /api/companies/{id} body — every field optional; baseCurrency is immutable. */
export interface CompanyUpdate {
  legalName?: string;
  tradingName?: string;
  country?: string;
  taxRegistrationNo?: string;
  taxRegistered?: boolean;
  taxRegistrationDate?: string | null;
  fiscalYearStart?: string;
  complianceProfile?: ComplianceProfile;
  abn?: string;
  acn?: string;
  gstRegistrationDate?: string | null;
  basPeriod?: BasPeriod;
  logoUrl?: string;
  invoiceLayout?: Record<string, unknown>;
  version: number; // optimistic-lock token
}

const BASE = "/companies";

/**
 * Axios calls for the company feature (ARCHITECTURE.md §3 module api/).
 * `getCurrent` reads the singleton via the paginated list endpoint and returns
 * the first record (or null when no company has been created yet).
 */
export const companyApi = {
  getCurrent: (query: PageQuery = { page: 0, size: 1 }) =>
    http
      .get<Paginated<Company>>(BASE, { params: query })
      .then((r) => r.data.content[0] ?? null),
  get: (id: string) => http.get<Company>(`${BASE}/${id}`).then((r) => r.data),
  update: (id: string, body: CompanyUpdate) =>
    http.patch<Company>(`${BASE}/${id}`, body).then((r) => r.data),
};
