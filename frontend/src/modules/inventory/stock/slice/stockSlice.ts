import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError } from "@/shared/types/api";
import { stockApi, type StockOnHand, type StockQuery } from "../api/stockApi";

interface StockState {
  rows: StockOnHand[];
  query: StockQuery;
  loading: boolean;
}

const initialState: StockState = {
  rows: [],
  query: {},
  loading: false,
};

export const fetchStockOnHand = createAsyncThunk<StockOnHand[], StockQuery, { rejectValue: ApiError }>(
  "stock/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await stockApi.onHand(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

const stockSlice = createSlice({
  name: "stock",
  initialState,
  reducers: {
    setQuery(state, action: { payload: StockQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchStockOnHand.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchStockOnHand.fulfilled, (state, action) => {
        state.loading = false;
        state.rows = action.payload;
      })
      .addCase(fetchStockOnHand.rejected, (state) => {
        state.loading = false;
      });
  },
});

export const { setQuery } = stockSlice.actions;
export default stockSlice.reducer;
export type { StockState };
