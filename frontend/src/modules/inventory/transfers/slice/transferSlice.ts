import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { transferApi, type StockTransfer, type TransferCreate } from "../api/transferApi";

interface TransferState {
  page: Paginated<StockTransfer> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: TransferState = {
  page: null,
  query: { page: 0, size: 20, sort: "createdAt,desc" },
  loading: false,
  saving: false,
};

export const fetchTransfers = createAsyncThunk<Paginated<StockTransfer>, PageQuery, { rejectValue: ApiError }>(
  "transfer/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await transferApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createTransfer = createAsyncThunk<StockTransfer, TransferCreate, { state: { transfer: TransferState } }>(
  "transfer/create",
  async (body, { dispatch, getState }) => {
    const created = await transferApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Transfer ${created.number} created` }));
    void dispatch(fetchTransfers(getState().transfer.query));
    return created;
  },
);

export const deleteTransfer = createAsyncThunk<void, string, { state: { transfer: TransferState } }>(
  "transfer/delete",
  async (id, { dispatch, getState }) => {
    await transferApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Transfer deleted" }));
    void dispatch(fetchTransfers(getState().transfer.query));
  },
);

export const confirmTransfer = createAsyncThunk<StockTransfer, string, { state: { transfer: TransferState } }>(
  "transfer/confirm",
  async (id, { dispatch, getState }) => {
    const updated = await transferApi.confirm(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} confirmed` }));
    void dispatch(fetchTransfers(getState().transfer.query));
    return updated;
  },
);

export const completeTransfer = createAsyncThunk<StockTransfer, string, { state: { transfer: TransferState } }>(
  "transfer/complete",
  async (id, { dispatch, getState }) => {
    const updated = await transferApi.complete(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} completed` }));
    void dispatch(fetchTransfers(getState().transfer.query));
    return updated;
  },
);

const transferSlice = createSlice({
  name: "transfer",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchTransfers.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchTransfers.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchTransfers.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("transfer/") && a.type.endsWith("/pending") && a.type !== fetchTransfers.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("transfer/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("transfer/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = transferSlice.actions;
export default transferSlice.reducer;
export type { TransferState };
