import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  supplierApi,
  type Supplier,
  type SupplierCreate,
  type SupplierUpdate,
  type SupplierStatus,
} from "../api/supplierApi";

interface SupplierState {
  page: Paginated<Supplier> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: SupplierState = {
  page: null,
  query: { page: 0, size: 20, sort: "name,asc" },
  loading: false,
  saving: false,
};

export const fetchSuppliers = createAsyncThunk<Paginated<Supplier>, PageQuery, { rejectValue: ApiError }>(
  "supplier/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await supplierApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createSupplier = createAsyncThunk<Supplier, SupplierCreate, { state: { supplier: SupplierState } }>(
  "supplier/create",
  async (body, { dispatch, getState }) => {
    const created = await supplierApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Supplier ${created.name} created` }));
    void dispatch(fetchSuppliers(getState().supplier.query));
    return created;
  },
);

export const updateSupplier = createAsyncThunk<
  Supplier,
  { id: string; body: SupplierUpdate },
  { state: { supplier: SupplierState } }
>("supplier/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await supplierApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Supplier ${updated.name} updated` }));
  void dispatch(fetchSuppliers(getState().supplier.query));
  return updated;
});

export const deleteSupplier = createAsyncThunk<void, string, { state: { supplier: SupplierState } }>(
  "supplier/delete",
  async (id, { dispatch, getState }) => {
    await supplierApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Supplier deleted" }));
    void dispatch(fetchSuppliers(getState().supplier.query));
  },
);

export const setSupplierStatus = createAsyncThunk<
  Supplier,
  { id: string; status: SupplierStatus; blockReason?: string },
  { state: { supplier: SupplierState } }
>("supplier/setStatus", async ({ id, status, blockReason }, { dispatch, getState }) => {
  const updated = await supplierApi.setStatus(id, status, blockReason);
  dispatch(pushToast({ tone: "success", message: `${updated.name} → ${status}` }));
  void dispatch(fetchSuppliers(getState().supplier.query));
  return updated;
});

const supplierSlice = createSlice({
  name: "supplier",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchSuppliers.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchSuppliers.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchSuppliers.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("supplier/") && a.type.endsWith("/pending") && a.type !== fetchSuppliers.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("supplier/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("supplier/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = supplierSlice.actions;
export default supplierSlice.reducer;
export type { SupplierState };
