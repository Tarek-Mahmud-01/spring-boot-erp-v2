/**
 * Permission code catalogue (mirrors backend permission codes). Used by the
 * <Can> gate, usePermission hook, and permission-driven route/menu generation
 * (ARCHITECTURE.md §3.2). Keep in sync with the backend seed (V2__seed_access).
 */
export const PERMISSIONS = {
  USER_READ: "access.user.read",
  USER_WRITE: "access.user.write",
  ROLE_READ: "access.role.read",
  ROLE_WRITE: "access.role.write",
  COMPANY_READ: "settings.company.read",
  COMPANY_WRITE: "settings.company.write",
} as const;

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];
