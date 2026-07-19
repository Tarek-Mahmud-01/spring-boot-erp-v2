import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { adjustmentApi, type StockAdjustment, type AdjustmentCreate } from "../api/adjustmentApi";

interface AdjustmentState {
  page: Paginated<StockAdjustment> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: AdjustmentState = {
  page: null,
  query: { page: 0, size: 20, sort: "createdAt,desc" },
  loading: false,
  saving: false,
};

export const fetchAdjustments = createAsyncThunk<Paginated<StockAdjustment>, PageQuery, { rejectValue: ApiError }>(
  "adjustment/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await adjustmentApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createAdjustment = createAsyncThunk<
  StockAdjustment,
  AdjustmentCreate,
  { state: { adjustment: AdjustmentState } }
>("adjustment/create", async (body, { dispatch, getState }) => {
  const created = await adjustmentApi.create(body);
  dispatch(pushToast({ tone: "success", message: `Adjustment ${created.number} created` }));
  void dispatch(fetchAdjustments(getState().adjustment.query));
  return created;
});

export const deleteAdjustment = createAsyncThunk<void, string, { state: { adjustment: AdjustmentState } }>(
  "adjustment/delete",
  async (id, { dispatch, getState }) => {
    await adjustmentApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Adjustment deleted" }));
    void dispatch(fetchAdjustments(getState().adjustment.query));
  },
);

export const approveAdjustment = createAsyncThunk<StockAdjustment, string, { state: { adjustment: AdjustmentState } }>(
  "adjustment/approve",
  async (id, { dispatch, getState }) => {
    const updated = await adjustmentApi.approve(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} approved` }));
    void dispatch(fetchAdjustments(getState().adjustment.query));
    return updated;
  },
);

export const postAdjustment = createAsyncThunk<StockAdjustment, string, { state: { adjustment: AdjustmentState } }>(
  "adjustment/post",
  async (id, { dispatch, getState }) => {
    const updated = await adjustmentApi.post(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} posted` }));
    void dispatch(fetchAdjustments(getState().adjustment.query));
    return updated;
  },
);

const adjustmentSlice = createSlice({
  name: "adjustment",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAdjustments.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchAdjustments.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchAdjustments.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("adjustment/") && a.type.endsWith("/pending") && a.type !== fetchAdjustments.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("adjustment/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("adjustment/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = adjustmentSlice.actions;
export default adjustmentSlice.reducer;
export type { AdjustmentState };
