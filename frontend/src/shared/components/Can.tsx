import type { ReactNode } from "react";
import { usePermission } from "@/shared/hooks/usePermission";
import type { PermissionCode } from "@/shared/constants/permissionCodes";

/**
 * Permission gate (ARCHITECTURE.md §3 shared/components). Renders children only
 * if the current user holds the permission(s). Backend still enforces — this is
 * UI convenience, not security.
 */
interface CanProps {
  /** Require this single permission. */
  permission?: PermissionCode | string;
  /** Require ANY of these. */
  anyOf?: Array<PermissionCode | string>;
  /** Require ALL of these. */
  allOf?: Array<PermissionCode | string>;
  fallback?: ReactNode;
  children: ReactNode;
}

export function Can({ permission, anyOf, allOf, fallback = null, children }: CanProps) {
  const { has, hasAny, hasAll } = usePermission();

  let allowed = true;
  if (permission) allowed = allowed && has(permission);
  if (anyOf?.length) allowed = allowed && hasAny(...anyOf);
  if (allOf?.length) allowed = allowed && hasAll(...allOf);

  return <>{allowed ? children : fallback}</>;
}
