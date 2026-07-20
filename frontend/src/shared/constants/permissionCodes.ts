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
  CURRENCY_READ: "settings.currency.read",
  CURRENCY_WRITE: "settings.currency.write",
  TAXCODE_READ: "settings.taxcode.read",
  TAXCODE_WRITE: "settings.taxcode.write",
  LOCATION_READ: "settings.location.read",
  LOCATION_WRITE: "settings.location.write",
  NUMBERING_READ: "settings.numbering.read",
  NUMBERING_WRITE: "settings.numbering.write",
  PRODUCT_READ: "product.product.read",
  PRODUCT_WRITE: "product.product.write",

  SUPPLIER_READ: "procurement.supplier.read",
  SUPPLIER_WRITE: "procurement.supplier.write",
  PROCUREMENT_ORDER_READ: "procurement.order.read",
  PROCUREMENT_ORDER_WRITE: "procurement.order.write",
  PROCUREMENT_REQUISITION_READ: "procurement.requisition.read",
  PROCUREMENT_REQUISITION_WRITE: "procurement.requisition.write",
  RECEIPT_READ: "procurement.receipt.read",
  RECEIPT_WRITE: "procurement.receipt.write",
  BILL_READ: "procurement.bill.read",
  BILL_WRITE: "procurement.bill.write",

  STOCK_READ: "inventory.stock.read",
  ADJUSTMENT_READ: "inventory.adjustment.read",
  ADJUSTMENT_WRITE: "inventory.adjustment.write",
  TRANSFER_READ: "inventory.transfer.read",
  TRANSFER_WRITE: "inventory.transfer.write",

  COA_READ: "finance.coa.read",
  COA_WRITE: "finance.coa.write",
  JOURNAL_READ: "finance.journal.read",
  JOURNAL_WRITE: "finance.journal.write",
  JOURNAL_POST: "finance.journal.post",
} as const;

export type PermissionCode = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];
