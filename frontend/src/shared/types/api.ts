/** Shared API contract types (ARCHITECTURE.md §3 shared/types). */

/** Server-driven pagination envelope — mirrors backend PageResponse<T>. */
export interface Paginated<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Query params every server-driven list accepts. */
export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string; // e.g. "createdAt,desc"
  filter?: string;
  [key: string]: string | number | undefined;
}

/** RFC-7807 problem+json body returned by the backend on error. */
export interface ApiError {
  type: string;
  title: string;
  status: number;
  detail: string;
  code: string;
  requestId?: string;
  errors?: Array<{ field: string; message: string }>;
  [key: string]: unknown;
}

/** Money as carried over the wire — minor units + ISO currency. */
export interface Money {
  amountMinor: number;
  currency: string;
}
