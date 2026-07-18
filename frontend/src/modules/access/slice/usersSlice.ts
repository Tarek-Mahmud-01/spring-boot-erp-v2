import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { usersApi, type UserRow } from "../api/usersApi";

/**
 * Users feature slice (ARCHITECTURE.md §3.1 — transaction/list data via
 * createAsyncThunk + createSlice, NOT RTK Query). The module owns this slice
 * and registers it lazily; here it is exported for the store to include.
 */
interface UsersState {
  page: Paginated<UserRow> | null;
  query: PageQuery;
  loading: boolean;
  error: string | null;
}

const initialState: UsersState = {
  page: null,
  query: { page: 0, size: 20, sort: "createdAt,desc" },
  loading: false,
  error: null,
};

export const fetchUsers = createAsyncThunk<Paginated<UserRow>, PageQuery, { rejectValue: ApiError }>(
  "access/fetchUsers",
  async (query, { rejectWithValue }) => {
    try {
      return await usersApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

const usersSlice = createSlice({
  name: "accessUsers",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUsers.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUsers.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchUsers.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload?.detail ?? "Failed to load users";
      });
  },
});

export const { setQuery } = usersSlice.actions;
export default usersSlice.reducer;
export type { UsersState };
