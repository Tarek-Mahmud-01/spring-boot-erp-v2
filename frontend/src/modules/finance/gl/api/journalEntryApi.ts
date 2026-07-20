import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

export type JournalEntryStatus = "DRAFT" | "POSTED" | "REVERSED";

export interface JournalLine {
  id: string;
  accountId: string;
  holderType: string | null;
  holderId: string | null;
  narration: string | null;
  debit: number;
  credit: number;
  currency: string;
  baseDebit: number;
  baseCredit: number;
  locationId: string | null;
}

export interface JournalLineInput {
  accountId: string;
  debit: number;
  credit: number;
  currency: string;
  narration?: string;
}

/** Journal entry wire type — mirrors backend JournalEntryDtos.JournalEntryResponse. */
export interface JournalEntry {
  id: string;
  companyId: string;
  locationId: string | null;
  voucherType: string;
  voucherNumber: string | null;
  entryDate: string;
  periodCode: string | null;
  reference: string | null;
  narration: string | null;
  status: JournalEntryStatus;
  lines: JournalLine[];
  totalDebit: number;
  totalCredit: number;
  balanced: boolean;
  reversedById: string | null;
  postedAt: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface JournalEntryCreate {
  companyId: string;
  locationId?: string;
  voucherType: string;
  entryDate: string;
  reference?: string;
  narration?: string;
  lines: JournalLineInput[];
}

const BASE = "/finance/gl/journal-entries";

export const journalEntryApi = {
  list: (companyId: string, q: PageQuery) =>
    http.get<Paginated<JournalEntry>>(BASE, { params: { companyId, ...q } }).then((r) => r.data),
  create: (b: JournalEntryCreate) => http.post<JournalEntry>(BASE, b).then((r) => r.data),
  remove: (id: string) => http.delete<void>(`${BASE}/${id}`).then((r) => r.data),
  post: (id: string) => http.post<JournalEntry>(`${BASE}/${id}/post`).then((r) => r.data),
  reverse: (id: string, entryDate: string, narration?: string) =>
    http.post<JournalEntry>(`${BASE}/${id}/reverse`, { entryDate, narration }).then((r) => r.data),
};
