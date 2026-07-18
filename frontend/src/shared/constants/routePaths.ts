/** Central route path registry (ARCHITECTURE.md §3 shared/constants). */
export const ROUTES = {
  LOGIN: "/login",
  ROOT: "/",
  DASHBOARD: "/dashboard",

  // settings + access (pilot module)
  USERS: "/access/users",
  ROLES: "/access/roles",
  COMPANY: "/settings/company",
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];
