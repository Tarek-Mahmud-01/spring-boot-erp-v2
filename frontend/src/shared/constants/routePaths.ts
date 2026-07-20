/** Central route path registry (ARCHITECTURE.md §3 shared/constants). */
export const ROUTES = {
  LOGIN: "/login",
  ROOT: "/",
  DASHBOARD: "/dashboard",

  // access
  USERS: "/access/users",
  ROLES: "/access/roles",

  // settings
  COMPANY: "/settings/company",
  CURRENCIES: "/settings/currencies",
  TAX_CODES: "/settings/tax-codes",
  LOCATIONS: "/settings/locations",
  NUMBERING: "/settings/numbering",

  // product
  PRODUCTS: "/catalog/products",

  // procurement
  SUPPLIERS: "/procurement/suppliers",
  PURCHASE_ORDERS: "/procurement/purchase-orders",
  REQUISITIONS: "/procurement/requisitions",
  RECEIPTS: "/procurement/receipts",
  BILLS: "/procurement/bills",

  // inventory
  STOCK: "/inventory/stock",
  ADJUSTMENTS: "/inventory/adjustments",
  TRANSFERS: "/inventory/transfers",

  // finance
  CHART_OF_ACCOUNTS: "/finance/accounts",
  JOURNAL_ENTRIES: "/finance/journal-entries",
} as const;

export type RoutePath = (typeof ROUTES)[keyof typeof ROUTES];
