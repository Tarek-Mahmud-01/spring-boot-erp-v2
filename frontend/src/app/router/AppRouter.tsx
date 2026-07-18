import { lazy } from "react";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import { AppShell } from "@/app/layouts/AppShell";
import { RequireAuth } from "./RequireAuth";
import { ROUTES } from "@/shared/constants/routePaths";

/**
 * Lazy, permission-aware route registry (ARCHITECTURE.md §3.2). Every page is
 * React.lazy() + dynamic import() so its chunk downloads only when navigated to
 * — no eager module code at startup. Auth pages are public; everything else is
 * wrapped by RequireAuth inside the persistent AppShell.
 */

// Pages live in their feature modules; imported lazily.
const LoginPage = lazy(() => import("@/modules/access/pages/LoginPage"));
const UsersPage = lazy(() => import("@/modules/access/pages/UsersPage"));
const DashboardPage = lazy(() => import("@/modules/dashboard/pages/DashboardPage"));

const router = createBrowserRouter([
  {
    path: ROUTES.LOGIN,
    element: <LoginPage />,
  },
  {
    path: ROUTES.ROOT,
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to={ROUTES.DASHBOARD} replace /> },
      { path: ROUTES.DASHBOARD, element: <DashboardPage /> },
      { path: ROUTES.USERS, element: <UsersPage /> },
    ],
  },
  { path: "*", element: <Navigate to={ROUTES.ROOT} replace /> },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
