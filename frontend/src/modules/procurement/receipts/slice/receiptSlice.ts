import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { receiptApi, type GoodsReceipt, type GrnCreate, type GrnStatus } from "../api/receiptApi";

interface ReceiptState {
  page: Paginated<GoodsReceipt> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: ReceiptState = {
  page: null,
  query: { page: 0, size: 20, sort: "receivedAt,desc" },
  loading: false,
  saving: false,
};

export const fetchReceipts = createAsyncThunk<Paginated<GoodsReceipt>, PageQuery, { rejectValue: ApiError }>(
  "receipt/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await receiptApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createReceipt = createAsyncThunk<GoodsReceipt, GrnCreate, { state: { receipt: ReceiptState } }>(
  "receipt/create",
  async (body, { dispatch, getState }) => {
    const created = await receiptApi.create(body);
    dispatch(pushToast({ tone: "success", message: `GRN ${created.number} created` }));
    void dispatch(fetchReceipts(getState().receipt.query));
    return created;
  },
);

export const deleteReceipt = createAsyncThunk<void, string, { state: { receipt: ReceiptState } }>(
  "receipt/delete",
  async (id, { dispatch, getState }) => {
    await receiptApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Receipt deleted" }));
    void dispatch(fetchReceipts(getState().receipt.query));
  },
);

export const transitionReceipt = createAsyncThunk<
  GoodsReceipt,
  { id: string; toStatus: GrnStatus },
  { state: { receipt: ReceiptState } }
>("receipt/transition", async ({ id, toStatus }, { dispatch, getState }) => {
  const updated = await receiptApi.transition(id, toStatus);
  dispatch(pushToast({ tone: "success", message: `${updated.number} → ${toStatus}` }));
  void dispatch(fetchReceipts(getState().receipt.query));
  return updated;
});

export const confirmReceipt = createAsyncThunk<GoodsReceipt, string, { state: { receipt: ReceiptState } }>(
  "receipt/confirm",
  async (id, { dispatch, getState }) => {
    const updated = await receiptApi.confirm(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} confirmed — stock posted` }));
    void dispatch(fetchReceipts(getState().receipt.query));
    return updated;
  },
);

const receiptSlice = createSlice({
  name: "receipt",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchReceipts.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchReceipts.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchReceipts.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("receipt/") && a.type.endsWith("/pending") && a.type !== fetchReceipts.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("receipt/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("receipt/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = receiptSlice.actions;
export default receiptSlice.reducer;
export type { ReceiptState };
