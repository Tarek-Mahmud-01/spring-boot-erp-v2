import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import type { ApiError } from "@/shared/types/api";
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from "./authTokens";

/**
 * Shared axios instance (ARCHITECTURE.md §3.1). Every thunk calls through this.
 * Responsibilities:
 *   - `/api` base prefix.
 *   - Attach `Authorization: Bearer <accessToken>` on each request.
 *   - On 401 with an expired token, transparently refresh once and retry.
 *   - Normalize RFC-7807 problem+json into a typed ApiError and surface it via
 *     a toast callback (wired by the app, so this module has no UI dependency).
 */

const BASE_URL = "/api";

export const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

// --- UI hooks wired at app startup (no direct import of the toast system) ---
type ErrorNotifier = (error: ApiError) => void;
type SessionExpiredHandler = () => void;

let notifyError: ErrorNotifier = () => {};
let onSessionExpired: SessionExpiredHandler = () => {};

export function configureHttp(handlers: {
  notifyError?: ErrorNotifier;
  onSessionExpired?: SessionExpiredHandler;
}): void {
  if (handlers.notifyError) notifyError = handlers.notifyError;
  if (handlers.onSessionExpired) onSessionExpired = handlers.onSessionExpired;
}

// --- Request: attach the bearer token ---
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// --- Response: 401 refresh-once + error normalization ---
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

// Single in-flight refresh shared by all concurrent 401s.
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refresh = getRefreshToken();
  if (!refresh) return null;
  try {
    // Bare axios call (not `http`) to avoid the interceptor recursion.
    const res = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken: refresh });
    const { accessToken, refreshToken } = res.data as { accessToken: string; refreshToken: string };
    setTokens(accessToken, refreshToken);
    return accessToken;
  } catch {
    return null;
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const config = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const problem = error.response?.data;

    // Attempt a single transparent refresh on 401 (expired access token).
    if (status === 401 && config && !config._retry && getRefreshToken()) {
      config._retry = true;
      refreshInFlight ??= refreshAccessToken().finally(() => {
        refreshInFlight = null;
      });
      const newToken = await refreshInFlight;
      if (newToken) {
        config.headers.set("Authorization", `Bearer ${newToken}`);
        return http(config);
      }
      clearTokens();
      onSessionExpired();
    }

    // Normalize and surface the error, then reject with the typed ApiError.
    const apiError: ApiError = problem ?? {
      type: "about:blank",
      title: "Network error",
      status: status ?? 0,
      detail: error.message,
      code: "network-error",
    };
    if (status !== 401) {
      notifyError(apiError);
    }
    return Promise.reject(apiError);
  },
);
