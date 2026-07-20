import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import type { ApiError } from "@/shared/types/api";
import { getCsrfToken } from "./authTokens";

/**
 * Shared axios instance (ARCHITECTURE.md §3.1). Every thunk calls through this.
 * Responsibilities:
 *   - `/api` base prefix, and `withCredentials` so the httpOnly auth cookies
 *     ride along automatically (tokens are never in JS).
 *   - Echo the CSRF cookie in the `X-CSRF-Token` header on mutating requests
 *     (double-submit-cookie) so the server accepts them.
 *   - On 401, transparently refresh once (cookie-based) and retry.
 *   - Normalize RFC-7807 problem+json into a typed ApiError and surface it via
 *     a toast callback (wired by the app, so this module has no UI dependency).
 */

const BASE_URL = "/api";
const MUTATING = new Set(["post", "put", "patch", "delete"]);

export const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
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

// --- Request: attach the CSRF token on mutating requests ---
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const method = (config.method ?? "get").toLowerCase();
  if (MUTATING.has(method)) {
    const csrf = getCsrfToken();
    if (csrf) {
      config.headers.set("X-CSRF-Token", csrf);
    }
  }
  return config;
});

// --- Response: 401 refresh-once + error normalization ---
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

// Single in-flight refresh shared by all concurrent 401s.
let refreshInFlight: Promise<boolean> | null = null;

async function refreshSession(): Promise<boolean> {
  try {
    // The refresh token rides in its httpOnly cookie; the server rotates it and
    // sets fresh auth cookies. Bare axios (not `http`) to avoid interceptor
    // recursion; withCredentials so the cookie is sent.
    await axios.post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true });
    return true;
  } catch {
    return false;
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const config = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const problem = error.response?.data;

    // Attempt a single transparent refresh on 401 (expired access cookie).
    if (status === 401 && config && !config._retry) {
      config._retry = true;
      refreshInFlight ??= refreshSession().finally(() => {
        refreshInFlight = null;
      });
      const refreshed = await refreshInFlight;
      if (refreshed) {
        return http(config);
      }
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
