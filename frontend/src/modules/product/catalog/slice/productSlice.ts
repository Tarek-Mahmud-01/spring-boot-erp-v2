import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  productApi,
  type Product,
  type ProductCreate,
  type ProductUpdate,
  type LifecycleState,
} from "../api/productApi";

/** Product catalog slice (ARCHITECTURE.md §3.1 — thunk + slice for list/txn data). */
interface ProductState {
  page: Paginated<Product> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: ProductState = {
  page: null,
  query: { page: 0, size: 20, sort: "sku,asc" },
  loading: false,
  saving: false,
};

export const fetchProducts = createAsyncThunk<Paginated<Product>, PageQuery, { rejectValue: ApiError }>(
  "product/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await productApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createProduct = createAsyncThunk<Product, ProductCreate, { state: { product: ProductState } }>(
  "product/create",
  async (body, { dispatch, getState }) => {
    const created = await productApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Product ${created.sku} created` }));
    void dispatch(fetchProducts(getState().product.query));
    return created;
  },
);

export const updateProduct = createAsyncThunk<
  Product,
  { id: string; body: ProductUpdate },
  { state: { product: ProductState } }
>("product/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await productApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Product ${updated.sku} updated` }));
  void dispatch(fetchProducts(getState().product.query));
  return updated;
});

export const deleteProduct = createAsyncThunk<void, string, { state: { product: ProductState } }>(
  "product/delete",
  async (id, { dispatch, getState }) => {
    await productApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Product deleted" }));
    void dispatch(fetchProducts(getState().product.query));
  },
);

export const transitionProduct = createAsyncThunk<
  Product,
  { id: string; toState: LifecycleState; reason?: string },
  { state: { product: ProductState } }
>("product/transition", async ({ id, toState, reason }, { dispatch, getState }) => {
  const updated = await productApi.transition(id, toState, reason);
  dispatch(pushToast({ tone: "success", message: `${updated.sku} → ${toState}` }));
  void dispatch(fetchProducts(getState().product.query));
  return updated;
});

const productSlice = createSlice({
  name: "product",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchProducts.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("product/") && a.type.endsWith("/pending") && a.type !== fetchProducts.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("product/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("product/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = productSlice.actions;
export default productSlice.reducer;
export type { ProductState };
