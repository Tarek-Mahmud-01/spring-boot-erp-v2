import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { billApi, type SupplierBill, type BillCreate } from "../api/billApi";

interface BillState {
  page: Paginated<SupplierBill> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: BillState = {
  page: null,
  query: { page: 0, size: 20, sort: "billDate,desc" },
  loading: false,
  saving: false,
};

export const fetchBills = createAsyncThunk<Paginated<SupplierBill>, PageQuery, { rejectValue: ApiError }>(
  "bill/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await billApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createBill = createAsyncThunk<SupplierBill, BillCreate, { state: { bill: BillState } }>(
  "bill/create",
  async (body, { dispatch, getState }) => {
    const created = await billApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Bill ${created.number} created` }));
    void dispatch(fetchBills(getState().bill.query));
    return created;
  },
);

export const deleteBill = createAsyncThunk<void, string, { state: { bill: BillState } }>(
  "bill/delete",
  async (id, { dispatch, getState }) => {
    await billApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Bill deleted" }));
    void dispatch(fetchBills(getState().bill.query));
  },
);

export const approveBill = createAsyncThunk<
  SupplierBill,
  { id: string; version?: number },
  { state: { bill: BillState } }
>("bill/approve", async ({ id, version }, { dispatch, getState }) => {
  const updated = await billApi.approve(id, version);
  dispatch(pushToast({ tone: "success", message: `${updated.number} approved` }));
  void dispatch(fetchBills(getState().bill.query));
  return updated;
});

const billSlice = createSlice({
  name: "bill",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchBills.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchBills.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchBills.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("bill/") && a.type.endsWith("/pending") && a.type !== fetchBills.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("bill/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("bill/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = billSlice.actions;
export default billSlice.reducer;
export type { BillState };
