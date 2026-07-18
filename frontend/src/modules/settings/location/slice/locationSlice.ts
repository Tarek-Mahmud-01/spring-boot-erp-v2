import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError, Paginated, PageQuery } from "@/shared/types/api";
import { pushToast } from "@/app/store/toastSlice";
import {
  locationApi,
  type Location,
  type LocationCreate,
  type LocationUpdate,
} from "../api/locationApi";

/**
 * Location feature slice (ARCHITECTURE.md §3.1 — list data via createAsyncThunk
 * + createSlice). Each mutating thunk refetches the list and pushes a success
 * toast on fulfilment.
 */
interface LocationState {
  page: Paginated<Location> | null;
  query: PageQuery;
  loading: boolean;
  saving: boolean;
}

const initialState: LocationState = {
  page: null,
  query: { page: 0, size: 20, sort: "code,asc" },
  loading: false,
  saving: false,
};

export const fetchLocations = createAsyncThunk<Paginated<Location>, PageQuery, { rejectValue: ApiError }>(
  "location/fetch",
  async (query, { rejectWithValue }) => {
    try {
      return await locationApi.list(query);
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const createLocation = createAsyncThunk<Location, LocationCreate, { state: { location: LocationState } }>(
  "location/create",
  async (body, { dispatch, getState }) => {
    const created = await locationApi.create(body);
    dispatch(pushToast({ tone: "success", message: `Location ${created.code} created` }));
    void dispatch(fetchLocations(getState().location.query));
    return created;
  },
);

export const updateLocation = createAsyncThunk<
  Location,
  { id: string; body: LocationUpdate },
  { state: { location: LocationState } }
>("location/update", async ({ id, body }, { dispatch, getState }) => {
  const updated = await locationApi.update(id, body);
  dispatch(pushToast({ tone: "success", message: `Location ${updated.code} updated` }));
  void dispatch(fetchLocations(getState().location.query));
  return updated;
});

export const deleteLocation = createAsyncThunk<void, string, { state: { location: LocationState } }>(
  "location/delete",
  async (id, { dispatch, getState }) => {
    await locationApi.remove(id);
    dispatch(pushToast({ tone: "success", message: "Location deleted" }));
    void dispatch(fetchLocations(getState().location.query));
  },
);

export const activateLocation = createAsyncThunk<Location, string, { state: { location: LocationState } }>(
  "location/activate",
  async (id, { dispatch, getState }) => {
    const updated = await locationApi.activate(id);
    dispatch(pushToast({ tone: "success", message: `Location ${updated.code} activated` }));
    void dispatch(fetchLocations(getState().location.query));
    return updated;
  },
);

export const deactivateLocation = createAsyncThunk<Location, string, { state: { location: LocationState } }>(
  "location/deactivate",
  async (id, { dispatch, getState }) => {
    const updated = await locationApi.deactivate(id);
    dispatch(pushToast({ tone: "success", message: `Location ${updated.code} deactivated` }));
    void dispatch(fetchLocations(getState().location.query));
    return updated;
  },
);

const locationSlice = createSlice({
  name: "location",
  initialState,
  reducers: {
    setQuery(state, action: { payload: PageQuery }) {
      state.query = { ...state.query, ...action.payload };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchLocations.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchLocations.fulfilled, (state, action) => {
        state.loading = false;
        state.page = action.payload;
      })
      .addCase(fetchLocations.rejected, (state) => {
        state.loading = false;
      })
      .addMatcher(
        (a) => a.type.startsWith("location/") && a.type.endsWith("/pending") && a.type !== fetchLocations.pending.type,
        (state) => {
          state.saving = true;
        },
      )
      .addMatcher(
        (a) =>
          a.type.startsWith("location/") &&
          (a.type.endsWith("/fulfilled") || a.type.endsWith("/rejected")) &&
          !a.type.startsWith("location/fetch"),
        (state) => {
          state.saving = false;
        },
      );
  },
});

export const { setQuery } = locationSlice.actions;
export default locationSlice.reducer;
export type { LocationState };
