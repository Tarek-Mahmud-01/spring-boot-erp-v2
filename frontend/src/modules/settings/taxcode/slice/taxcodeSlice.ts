import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  taxcodeApi,
  type TaxCode,
  type TaxCodeCreate,
  type TaxCodeUpdate,
} from "../api/taxcodeApi";

/**
 * Tax code feature slice (ARCHITECTURE.md §3.1 — transaction/list data via
 * createAsyncThunk + createSlice). Each mutating thunk refetches the list and
 * pushes a success toast on fulfilment.
 */
interface TaxCodeState {
  page: Paginated<TaxCode> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: TaxCodeState = {
  page: null,
  query: { page: 0, size: 20, sort: "code,asc" },
  loading: false,
  saving: false,
};

export const fetchTaxCodes = createAsyncThunk<Paginated<TaxCode>, PageQuery, { rejectValue: ApiError }>(
  "taxcode/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await taxcodeApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createTaxCode = createAsyncThunk<TaxCode, TaxCodeCreate, { state: { taxcode: TaxCodeState } }>(
  "taxcode/create",
  async (body, { dispatch, getState }) => {
    const created = await taxcodeApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Tax code ${created.code} created` }));
    void dispatch(fetchTaxCodes(getState().taxcode.query));
    return created;
  },
);

export const updateTaxCode = createAsyncThunk<
  TaxCode,
  { id: string; body: TaxCodeUpdate },
  { state: { taxcode: TaxCodeState } }
>("taxcode/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await taxcodeApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Tax code ${updated.code} updated` }));
  void dispatch(fetchTaxCodes(getState().taxcode.query));
  return updated;
});

export const deleteTaxCode = createAsyncThunk<void, string, { state: { taxcode: TaxCodeState } }>(
  "taxcode/delete",
  async (id, { dispatch, getState }) => {
    await taxcodeApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Tax code deleted" }));
    void dispatch(fetchTaxCodes(getState().taxcode.query));
  },
);

const taxcodeSlice = createSlice({
  name: "taxcode",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchTaxCodes.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchTaxCodes.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchTaxCodes.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("taxcode/") && a.type.endsWith("/pending") && a.type !== fetchTaxCodes.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("taxcode/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("taxcode/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = taxcodeSlice.actions;
export default taxcodeSlice.reducer;
export type { TaxCodeState };
