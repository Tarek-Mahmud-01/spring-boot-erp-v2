/**
 * Auth-cookie helpers.
 *
 * Access/refresh tokens now live in httpOnly cookies set by the backend, so
 * page JS (and therefore an XSS payload) can NOT read them — this module no
 * longer stores tokens. The one value we read is the CSRF token, which the
 * server exposes in a non-httpOnly cookie for the SPA to echo back in the
 * `X-CSRF-Token` header on mutating requests (double-submit-cookie pattern).
 */

const CSRF_COOKIE = "ERP_CSRF";

/** Read the CSRF token the backend set as a readable cookie, or null. */
export function getCsrfToken(): string | null {
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${CSRF_COOKIE}=`));
  return match ? decodeURIComponent(match.slice(CSRF_COOKIE.length + 1)) : null;
}

/** True if a CSRF cookie is present (a heuristic that a session likely exists). */
export function hasSessionCookie(): boolean {
  return getCsrfToken() !== null;
}
