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
const CurrenciesPage = lazy(() => import("@/modules/settings/currency/pages/CurrenciesPage"));
const TaxCodesPage = lazy(() => import("@/modules/settings/taxcode/pages/TaxCodesPage"));
const LocationsPage = lazy(() => import("@/modules/settings/location/pages/LocationsPage"));
const NumberingPage = lazy(() => import("@/modules/settings/numbering/pages/NumberingPage"));
const CompanyPage = lazy(() => import("@/modules/settings/company/pages/CompanyPage"));
const ProductsPage = lazy(() => import("@/modules/product/catalog/pages/ProductsPage"));
const SuppliersPage = lazy(() => import("@/modules/procurement/suppliers/pages/SuppliersPage"));
const PurchaseOrdersPage = lazy(() => import("@/modules/procurement/orders/pages/PurchaseOrdersPage"));
const RequisitionsPage = lazy(() => import("@/modules/procurement/requisitions/pages/RequisitionsPage"));
const ReceiptsPage = lazy(() => import("@/modules/procurement/receipts/pages/ReceiptsPage"));
const BillsPage = lazy(() => import("@/modules/procurement/bills/pages/BillsPage"));
const StockPage = lazy(() => import("@/modules/inventory/stock/pages/StockPage"));
const AdjustmentsPage = lazy(() => import("@/modules/inventory/adjustments/pages/AdjustmentsPage"));
const TransfersPage = lazy(() => import("@/modules/inventory/transfers/pages/TransfersPage"));

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
      { path: ROUTES.CURRENCIES, element: <CurrenciesPage /> },
      { path: ROUTES.TAX_CODES, element: <TaxCodesPage /> },
      { path: ROUTES.LOCATIONS, element: <LocationsPage /> },
      { path: ROUTES.NUMBERING, element: <NumberingPage /> },
      { path: ROUTES.COMPANY, element: <CompanyPage /> },
      { path: ROUTES.PRODUCTS, element: <ProductsPage /> },
      { path: ROUTES.SUPPLIERS, element: <SuppliersPage /> },
      { path: ROUTES.PURCHASE_ORDERS, element: <PurchaseOrdersPage /> },
      { path: ROUTES.REQUISITIONS, element: <RequisitionsPage /> },
      { path: ROUTES.RECEIPTS, element: <ReceiptsPage /> },
      { path: ROUTES.BILLS, element: <BillsPage /> },
    ],
  },
  { path: "*", element: <Navigate to={ROUTES.ROOT} replace /> },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
