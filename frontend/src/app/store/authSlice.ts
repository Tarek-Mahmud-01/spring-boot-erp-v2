import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import type { ApiError } from "@/shared/types/api";
import { http } from "@/shared/services/http";
import { clearTokens, getAccessToken, setTokens } from "@/shared/services/authTokens";

/**
 * Auth slice (ARCHITECTURE.md §3 global slices). Holds the authenticated user
 * and their flattened permission codes — the source of truth for the <Can>
 * gate and permission-driven route/menu generation. Login/refresh/logout are
 * createAsyncThunks calling through the shared axios instance (§3.1).
 */

export interface CurrentUser {
  publicId: string;
  username: string;
  fullName: string;
  email: string;
  permissions: string[];
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: CurrentUser;
}

interface AuthState {
  user: CurrentUser | null;
  permissions: string[];
  status: "idle" | "loading" | "authenticated" | "error";
  error: string | null;
  /** True until the initial "who am I" resolves, so the app can gate rendering. */
  bootstrapped: boolean;
}

const initialState: AuthState = {
  user: null,
  permissions: [],
  status: "idle",
  error: null,
  bootstrapped: false,
};

export const login = createAsyncThunk<
  LoginResponse,
  { username: string; password: string },
  { rejectValue: ApiError }
>("auth/login", async (credentials, { rejectWithValue }) => {
  try {
    const res = await http.post<LoginResponse>("/auth/login", credentials);
    setTokens(res.data.accessToken, res.data.refreshToken);
    return res.data;
  } catch (err) {
    return rejectWithValue(err as ApiError);
  }
});

/** Load the current user + permissions from an existing token (page reload). */
export const fetchCurrentUser = createAsyncThunk<CurrentUser | null, void, { rejectValue: ApiError }>(
  "auth/fetchCurrentUser",
  async (_, { rejectWithValue }) => {
    if (!getAccessToken()) return null;
    try {
      const res = await http.get<CurrentUser>("/auth/me");
      return res.data;
    } catch (err) {
      return rejectWithValue(err as ApiError);
    }
  },
);

export const logout = createAsyncThunk("auth/logout", async () => {
  clearTokens();
});

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.user = action.payload.user;
        state.permissions = action.payload.user.permissions;
        state.status = "authenticated";
        state.bootstrapped = true;
      })
      .addCase(login.rejected, (state, action) => {
        state.status = "error";
        state.error = action.payload?.detail ?? "Login failed";
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        if (action.payload) {
          state.user = action.payload;
          state.permissions = action.payload.permissions;
          state.status = "authenticated";
        }
        state.bootstrapped = true;
      })
      .addCase(fetchCurrentUser.rejected, (state) => {
        state.user = null;
        state.permissions = [];
        state.bootstrapped = true;
      })
      .addCase(logout.fulfilled, (state) => {
        state.user = null;
        state.permissions = [];
        state.status = "idle";
      });
  },
});

export default authSlice.reducer;
export type { AuthState };
