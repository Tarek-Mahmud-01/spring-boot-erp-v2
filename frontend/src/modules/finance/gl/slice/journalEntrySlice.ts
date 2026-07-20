import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { journalEntryApi, type JournalEntry, type JournalEntryCreate } from "../api/journalEntryApi";

interface JournalEntryState {
  page: Paginated<JournalEntry> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: JournalEntryState = {
  page: null,
  query: { page: 0, size: 20, sort: "entryDate,desc" },
  loading: false,
  saving: false,
};

export const fetchJournalEntries = createAsyncThunk<
  Paginated<JournalEntry>,
  { companyId: string; query: PageQuery },
  { rejectValue: ApiError }
>("journalEntry/fetch", async ({ companyId, query }, { rejectWithValue }) => {
  try {
    return await journalEntryApi.list(companyId, query);
  } catch (err) {
    return rejectWithValue(err as ApiError);
  }
});

export const createJournalEntry = createAsyncThunk<
  JournalEntry,
  JournalEntryCreate,
  { state: { journalEntry: JournalEntryState } }
>("journalEntry/create", async (body, { dispatch, getState }) => {
  const created = await journalEntryApi.create(body);
  dispatch(pushToast({ tone: "success", message: `Journal entry ${created.voucherNumber ?? created.id} created` }));
  void dispatch(fetchJournalEntries({ companyId: body.companyId, query: getState().journalEntry.query }));
  return created;
});

export const deleteJournalEntry = createAsyncThunk<
  void,
  { id: string; companyId: string },
  { state: { journalEntry: JournalEntryState } }
>("journalEntry/delete", async ({ id, companyId }, { dispatch, getState }) => {
  await journalEntryApi.remove(id);
  dispatch(pushToast({ tone: "success", message: "Journal entry deleted" }));
  void dispatch(fetchJournalEntries({ companyId, query: getState().journalEntry.query }));
});

export const postJournalEntry = createAsyncThunk<
  JournalEntry,
  { id: string; companyId: string },
  { state: { journalEntry: JournalEntryState } }
>("journalEntry/post", async ({ id, companyId }, { dispatch, getState }) => {
  const updated = await journalEntryApi.post(id);
  dispatch(pushToast({ tone: "success", message: `${updated.voucherNumber ?? updated.id} posted` }));
  void dispatch(fetchJournalEntries({ companyId, query: getState().journalEntry.query }));
  return updated;
});

export const reverseJournalEntry = createAsyncThunk<
  JournalEntry,
  { id: string; companyId: string; entryDate: string; narration?: string },
  { state: { journalEntry: JournalEntryState } }
>("journalEntry/reverse", async ({ id, companyId, entryDate, narration }, { dispatch, getState }) => {
  const updated = await journalEntryApi.reverse(id, entryDate, narration);
  dispatch(pushToast({ tone: "success", message: `${updated.voucherNumber ?? updated.id} reversed` }));
  void dispatch(fetchJournalEntries({ companyId, query: getState().journalEntry.query }));
  return updated;
});

const journalEntrySlice = createSlice({
  name: "journalEntry",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchJournalEntries.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchJournalEntries.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchJournalEntries.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) =>
          a.type.startsWith("journalEntry/") &&
          a.type.endsWith("/pending") &&
          a.type !== fetchJournalEntries.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("journalEntry/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("journalEntry/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = journalEntrySlice.actions;
export default journalEntrySlice.reducer;
export type { JournalEntryState };
