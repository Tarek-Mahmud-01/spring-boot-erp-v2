import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  currencyApi,
  type Currency,
  type CurrencyCreate,
  type CurrencyUpdate,
} from "../api/currencyApi";

/**
 * Currency feature slice (ARCHITECTURE.md §3.1 — transaction/list data via
 * createAsyncThunk + createSlice). Each mutating thunk refetches the list and
 * pushes a success toast on fulfilment.
 */
interface CurrencyState {
  page: Paginated<Currency> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: CurrencyState = {
  page: null,
  query: { page: 0, size: 20, sort: "code,asc" },
  loading: false,
  saving: false,
};

export const fetchCurrencies = createAsyncThunk<Paginated<Currency>, PageQuery, { rejectValue: ApiError }>(
  "currency/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await currencyApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createCurrency = createAsyncThunk<Currency, CurrencyCreate, { state: { currency: CurrencyState } }>(
  "currency/create",
  async (body, { dispatch, getState }) => {
    const created = await currencyApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Currency ${created.code} created` }));
    void dispatch(fetchCurrencies(getState().currency.query));
    return created;
  },
);

export const updateCurrency = createAsyncThunk<
  Currency,
  { id: string; body: CurrencyUpdate },
  { state: { currency: CurrencyState } }
>("currency/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await currencyApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Currency ${updated.code} updated` }));
  void dispatch(fetchCurrencies(getState().currency.query));
  return updated;
});

export const deleteCurrency = createAsyncThunk<void, string, { state: { currency: CurrencyState } }>(
  "currency/delete",
  async (id, { dispatch, getState }) => {
    await currencyApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Currency deleted" }));
    void dispatch(fetchCurrencies(getState().currency.query));
  },
);

export const setDefaultCurrency = createAsyncThunk<Currency, string, { state: { currency: CurrencyState } }>(
  "currency/setDefault",
  async (id, { dispatch, getState }) => {
    const updated = await currencyApi.setDefault(id);
    dispatch(pushToast({ tone: "success", message: `${updated.code} is now the default currency` }));
    void dispatch(fetchCurrencies(getState().currency.query));
    return updated;
  },
);

const currencySlice = createSlice({
  name: "currency",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCurrencies.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchCurrencies.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchCurrencies.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("currency/") && a.type.endsWith("/pending") && a.type !== fetchCurrencies.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("currency/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("currency/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = currencySlice.actions;
export default currencySlice.reducer;
export type { CurrencyState };
