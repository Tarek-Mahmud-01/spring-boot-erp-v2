/**
 * In-memory + persisted access/refresh token holder. Kept separate from the
 * axios instance and the Redux store so both can read/write tokens without a
 * circular import. The access token is held in memory (fast, cleared on reload)
 * and mirrored to localStorage so a page refresh can rehydrate the session.
 */

const ACCESS_KEY = "guru-erp.accessToken";
const REFRESH_KEY = "guru-erp.refreshToken";

let accessToken: string | null = readInitial(ACCESS_KEY);
let refreshToken: string | null = readInitial(REFRESH_KEY);

function readInitial(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function getRefreshToken(): string | null {
  return refreshToken;
}

export function setTokens(access: string | null, refresh: string | null): void {
  accessToken = access;
  refreshToken = refresh;
  try {
    if (access) localStorage.setItem(ACCESS_KEY, access);
    else localStorage.removeItem(ACCESS_KEY);
    if (refresh) localStorage.setItem(REFRESH_KEY, refresh);
    else localStorage.removeItem(REFRESH_KEY);
  } catch {
    // ignore storage failures — tokens still live in memory for this session
  }
}

export function clearTokens(): void {
  setTokens(null, null);
}
