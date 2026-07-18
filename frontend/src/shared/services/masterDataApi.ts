import { createApi } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn } from "@reduxjs/toolkit/query";
import type { AxiosRequestConfig } from "axios";
import type { ApiError } from "@/shared/types/api";
import { http } from "./http";

/**
 * RTK Query — MASTER / REFERENCE DATA ONLY (ARCHITECTURE.md §3.1). Load-once,
 * cache: countries, currencies, units, tax codes, departments, config, etc.
 * NEVER for transaction data (invoices, stock, payroll…) — those go through
 * createAsyncThunk so a single thunk can update multiple slices explicitly.
 *
 * Uses the shared axios instance as its base query so tokens, refresh, and
 * error normalization stay in one place.
 */
const axiosBaseQuery =
  (): BaseQueryFn<
    { url: string; method?: AxiosRequestConfig["method"]; data?: unknown; params?: unknown },
    unknown,
    ApiError
  > =>
  async ({ url, method = "GET", data, params }) => {
    try {
      const result = await http.request({ url, method, data, params });
      return { data: result.data };
    } catch (axiosError) {
      return { error: axiosError as ApiError };
    }
  };

export const masterDataApi = createApi({
  reducerPath: "masterDataApi",
  baseQuery: axiosBaseQuery(),
  // Master data changes rarely; keep it cached for the session.
  keepUnusedDataFor: 3600,
  tagTypes: ["Currency", "TaxCode", "Unit", "Country", "Department"],
  endpoints: () => ({}),
});
