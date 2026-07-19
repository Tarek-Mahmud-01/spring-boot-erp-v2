import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { requisitionApi, type PurchaseRequisition, type PrCreate, type PrStatus } from "../api/requisitionApi";

interface RequisitionState {
  page: Paginated<PurchaseRequisition> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: RequisitionState = {
  page: null,
  query: { page: 0, size: 20, sort: "requestDate,desc" },
  loading: false,
  saving: false,
};

export const fetchRequisitions = createAsyncThunk<Paginated<PurchaseRequisition>, PageQuery, { rejectValue: ApiError }>(
  "requisition/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await requisitionApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createRequisition = createAsyncThunk<
  PurchaseRequisition,
  PrCreate,
  { state: { requisition: RequisitionState } }
>("requisition/create", async (body, { dispatch, getState }) => {
  const created = await requisitionApi.create(body);
  dispatch(pushToast({ tone: "success", message: `Requisition ${created.number} created` }));
  void dispatch(fetchRequisitions(getState().requisition.query));
  return created;
});

export const deleteRequisition = createAsyncThunk<void, string, { state: { requisition: RequisitionState } }>(
  "requisition/delete",
  async (id, { dispatch, getState }) => {
    await requisitionApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Requisition deleted" }));
    void dispatch(fetchRequisitions(getState().requisition.query));
  },
);

export const submitRequisition = createAsyncThunk<PurchaseRequisition, string, { state: { requisition: RequisitionState } }>(
  "requisition/submit",
  async (id, { dispatch, getState }) => {
    const updated = await requisitionApi.submit(id);
    dispatch(pushToast({ tone: "success", message: `${updated.number} submitted` }));
    void dispatch(fetchRequisitions(getState().requisition.query));
    return updated;
  },
);

export const transitionRequisition = createAsyncThunk<
  PurchaseRequisition,
  { id: string; toStatus: PrStatus; reason?: string },
  { state: { requisition: RequisitionState } }
>("requisition/transition", async ({ id, toStatus, reason }, { dispatch, getState }) => {
  const updated = await requisitionApi.transition(id, toStatus, reason);
  dispatch(pushToast({ tone: "success", message: `${updated.number} → ${toStatus}` }));
  void dispatch(fetchRequisitions(getState().requisition.query));
  return updated;
});

export const convertRequisitionToPo = createAsyncThunk<
  void,
  string,
  { state: { requisition: RequisitionState } }
>("requisition/convertToPo", async (id, { dispatch, getState }) => {
  const po = await requisitionApi.convertToPo(id);
  dispatch(pushToast({ tone: "success", message: `Converted to PO ${po.number}` }));
  void dispatch(fetchRequisitions(getState().requisition.query));
});

const requisitionSlice = createSlice({
  name: "requisition",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRequisitions.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRequisitions.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchRequisitions.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) =>
          a.type.startsWith("requisition/") &&
          a.type.endsWith("/pending") &&
          a.type !== fetchRequisitions.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("requisition/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("requisition/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = requisitionSlice.actions;
export default requisitionSlice.reducer;
export type { RequisitionState };
