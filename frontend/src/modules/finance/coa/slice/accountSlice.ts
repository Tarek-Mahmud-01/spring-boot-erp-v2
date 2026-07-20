import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import { accountApi, type Account, type AccountCreate, type AccountUpdate } from "../api/accountApi";

interface AccountState {
  page: Paginated<Account> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: AccountState = {
  page: null,
  query: { page: 0, size: 50, sort: "code,asc" },
  loading: false,
  saving: false,
};

export const fetchAccounts = createAsyncThunk<
  Paginated<Account>,
  { companyId: string; query: PageQuery },
  { rejectValue: ApiError }
>("account/fetch", async ({ companyId, query }, { rejectWithValue }) => {
  try {
    return await accountApi.list(companyId, query);
  } catch (err) {
    return rejectWithValue(err as ApiError);
  }
});

export const createAccount = createAsyncThunk<
  Account,
  AccountCreate,
  { state: { account: AccountState } }
>("account/create", async (body, { dispatch, getState }) => {
  const created = await accountApi.create(body);
  dispatch(pushToast({ tone: "success", message: `Account ${created.code} created` }));
  void dispatch(fetchAccounts({ companyId: body.companyId, query: getState().account.query }));
  return created;
});

export const updateAccount = createAsyncThunk<
  Account,
  { id: string; companyId: string; body: AccountUpdate },
  { state: { account: AccountState } }
>("account/update", async ({ id, companyId, body }, { dispatch, getState }) => {
  const updated = await accountApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Account ${updated.code} updated` }));
  void dispatch(fetchAccounts({ companyId, query: getState().account.query }));
  return updated;
});

export const deleteAccount = createAsyncThunk<
  void,
  { id: string; companyId: string },
  { state: { account: AccountState } }
>("account/delete", async ({ id, companyId }, { dispatch, getState }) => {
  await accountApi.remove(id);
  dispatch(pushToast({ tone: "success", message: "Account deleted" }));
  void dispatch(fetchAccounts({ companyId, query: getState().account.query }));
});

const accountSlice = createSlice({
  name: "account",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAccounts.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchAccounts.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchAccounts.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("account/") && a.type.endsWith("/pending") && a.type !== fetchAccounts.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("account/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("account/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = accountSlice.actions;
export default accountSlice.reducer;
export type { AccountState };
