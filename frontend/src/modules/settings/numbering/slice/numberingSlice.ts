import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  numberingApi,
  type NumberingRule,
  type NumberingRuleCreate,
  type NumberingRuleUpdate,
  type NumberingAllocateResponse,
} from "../api/numberingApi";

/**
 * Numbering-rule feature slice (ARCHITECTURE.md §3.1 — list data via
 * createAsyncThunk + createSlice). Each mutating thunk refetches the list and
 * pushes a success toast on fulfilment.
 */
interface NumberingState {
  page: Paginated<NumberingRule> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: NumberingState = {
  page: null,
  query: { page: 0, size: 20, sort: "documentType,asc" },
  loading: false,
  saving: false,
};

export const fetchNumberingRules = createAsyncThunk<
  Paginated<NumberingRule>,
  PageQuery,
  { rejectValue: ApiError }
>("numbering/fetch", async (query, { rejectWithValue }) => {
  try {
    return await numberingApi.list(query);
  } catch (err) {
    return rejectWithValue(err as ApiError);
  }
});

export const createNumberingRule = createAsyncThunk<
  NumberingRule,
  NumberingRuleCreate,
  { state: { numbering: NumberingState } }
>("numbering/create", async (body, { dispatch, getState }) => {
  const created = await numberingApi.create(body);
  dispatch(pushToast({ tone: "success", message: `Rule for ${created.documentType} created` }));
  void dispatch(fetchNumberingRules(getState().numbering.query));
  return created;
});

export const updateNumberingRule = createAsyncThunk<
  NumberingRule,
  { id: string; body: NumberingRuleUpdate },
  { state: { numbering: NumberingState } }
>("numbering/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await numberingApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Rule for ${updated.documentType} updated` }));
  void dispatch(fetchNumberingRules(getState().numbering.query));
  return updated;
});

export const allocateNumber = createAsyncThunk<
  NumberingAllocateResponse,
  { id: string; documentDate: string },
  { state: { numbering: NumberingState } }
>("numbering/allocate", async ({ id, documentDate }, { dispatch, getState }) => {
  const result = await numberingApi.allocate(id, documentDate);
  dispatch(pushToast({ tone: "success", message: `Allocated ${result.number}` }));
  void dispatch(fetchNumberingRules(getState().numbering.query));
  return result;
});

const numberingSlice = createSlice({
  name: "numbering",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchNumberingRules.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchNumberingRules.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchNumberingRules.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) =>
          a.type.startsWith("numbering/") &&
          a.type.endsWith("/pending") &&
          a.type !== fetchNumberingRules.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("numbering/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("numbering/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = numberingSlice.actions;
export default numberingSlice.reducer;
export type { NumberingState };
