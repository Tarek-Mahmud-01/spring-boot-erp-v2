import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { companyApi, type Company, type CompanyUpdate } from "../api/companyApi";

/**
 * Company settings slice (ARCHITECTURE.md §3.1). The company is a singleton in
 * this single-tenant app, so state holds a single record (or null before one
 * exists). `fetchCompany` loads the current record; `updateCompany` PATCHes it
 * (carrying the optimistic-lock version) and pushes a success toast.
 */
interface CompanyState {
  company: Company | null;
  loading: boolean;
  saving: boolean;
}

const initialState: CompanyState = {
  company: null,
  loading: false,
  saving: false,
};

export const fetchCompany = createAsyncThunk<Company | null, void, { rejectValue: ApiError }>(
  "company/fetch",
  async (_, { rejectWithValue }) => {
    try {
      return await companyApi.getCurrent();
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const updateCompany = createAsyncThunk<
  Company,
  { id: string; body: CompanyUpdate },
  { rejectValue: ApiError }
>("company/update", async ({ id, body }, { dispatch, rejectWithValue }) => {
  try {
    const updated = await companyApi.update(id, body);
    dispatch(pushToast({ tone: "success", message: "Company settings saved" }));
    return updated;
  } catch (err) {
    return rejectWithValue(err as ApiError);
  }
});

const companySlice = createSlice({
  name: "company",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchCompany.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchCompany.fulfilled, (state, action) => {
        state.loading = false;
        state.company = action.payload;
      })
      .addCase(fetchCompany.rejected, (state) => {
        state.loading = false;
      })
      .addCase(updateCompany.pending, (state) => {
        state.saving = true;
      })
      .addCase(updateCompany.fulfilled, (state, action) => {
        state.saving = false;
        state.company = action.payload;
      })
      .addCase(updateCompany.rejected, (state) => {
        state.saving = false;
      });
  },
});

export default companySlice.reducer;
export type { CompanyState };
