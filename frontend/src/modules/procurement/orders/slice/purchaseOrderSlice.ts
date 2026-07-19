import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { purchaseOrderApi, type PurchaseOrder, type PoCreate, type PoStatus } from "../api/purchaseOrderApi";

interface PurchaseOrderState {
  page: Paginated<PurchaseOrder> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: PurchaseOrderState = {
  page: null,
  query: { page: 0, size: 20, sort: "poDate,desc" },
  loading: false,
  saving: false,
};

export const fetchPurchaseOrders = createAsyncThunk<Paginated<PurchaseOrder>, PageQuery, { rejectValue: ApiError }>(
  "purchaseOrder/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await purchaseOrderApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createPurchaseOrder = createAsyncThunk<
  PurchaseOrder,
  PoCreate,
  { state: { purchaseOrder: PurchaseOrderState } }
>("purchaseOrder/create", async (body, { dispatch, getState }) => {
  const created = await purchaseOrderApi.create(body);
  dispatch(pushToast({ tone: "success", message: `PO ${created.number} created` }));
  void dispatch(fetchPurchaseOrders(getState().purchaseOrder.query));
  return created;
});

export const deletePurchaseOrder = createAsyncThunk<void, string, { state: { purchaseOrder: PurchaseOrderState } }>(
  "purchaseOrder/delete",
  async (id, { dispatch, getState }) => {
    await purchaseOrderApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Purchase order deleted" }));
    void dispatch(fetchPurchaseOrders(getState().purchaseOrder.query));
  },
);

export const transitionPurchaseOrder = createAsyncThunk<
  PurchaseOrder,
  { id: string; toStatus: PoStatus; reason?: string },
  { state: { purchaseOrder: PurchaseOrderState } }
>("purchaseOrder/transition", async ({ id, toStatus, reason }, { dispatch, getState }) => {
  const updated = await purchaseOrderApi.transition(id, toStatus, reason);
  dispatch(pushToast({ tone: "success", message: `${updated.number} → ${toStatus}` }));
  void dispatch(fetchPurchaseOrders(getState().purchaseOrder.query));
  return updated;
});

const purchaseOrderSlice = createSlice({
  name: "purchaseOrder",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPurchaseOrders.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPurchaseOrders.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchPurchaseOrders.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) =>
          a.type.startsWith("purchaseOrder/") &&
          a.type.endsWith("/pending") &&
          a.type !== fetchPurchaseOrders.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("purchaseOrder/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("purchaseOrder/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = purchaseOrderSlice.actions;
export default purchaseOrderSlice.reducer;
export type { PurchaseOrderState };
