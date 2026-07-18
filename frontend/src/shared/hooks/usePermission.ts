import { useAppSelector } from "./redux";
import type { PermissionCode } from "@/shared/constants/permissionCodes";

/**
 * Permission checks against the current user's granted codes (ARCHITECTURE.md
 * §3.2 — permission-driven UI). Drives the <Can> gate, menu, and route
 * generation. Authorization is still enforced on the backend; this only hides
 * UI the user can't use.
 */
export function usePermission() {
  const permissions = useAppSelector((s) => s.auth.permissions);
  const set = new Set(permissions);

  const has = (code: PermissionCode | string): boolean => set.has(code);
  const hasAny = (...codes: Array<PermissionCode | string>): boolean => codes.some(has);
  const hasAll = (...codes: Array<PermissionCode | string>): boolean => codes.every(has);

  return { has, hasAny, hasAll, permissions };
}
