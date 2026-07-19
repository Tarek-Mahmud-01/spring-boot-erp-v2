-- =====================================================================
-- V82 — REPORTING module, "inv-proc" sub-slice (inventory stock summary +
-- product ledger + transfer register; procurement supplier summary +
-- PO/GRN/bill aging + supplier spend).
--
-- This slice is READ-ONLY and cross-cutting (ARCHITECTURE.md — reporting
-- reads other modules' already-ported entities directly via QueryDSL; it
-- owns no tables of its own). No new tables — this migration seeds ONLY
-- the permission rows the slice's @PreAuthorize checks require, granted to
-- the admin role, matching the V70/V71/V72/V80/V81 pattern exactly.
--
-- Reference: guru-erp-revamp/backend/app/reports/repositories/inventory.py,
-- schemas/inventory.py, views/inventory.py (Stock Summary / Product Ledger /
-- Transfer Register — Low Stock Alerts is out of this slice's scope, it
-- belongs to a future reorder-point feature) and
-- app/reports/repositories/procurement.py, schemas/procurement.py,
-- views/procurement.py (Supplier Summary / a PO+GRN+Bill aging view built
-- from this project's separately-ported PurchaseOrder / GoodsReceipt /
-- SupplierBill entities / Supplier Spend — Supplier Ledger, Purchase Lines,
-- and Purchase Returns are out of this slice's scope per the FOCUS brief).
--
-- Fresh PERM196..197 ULIDs (exactly 26 chars, matching the V60/V70/V71/V72/
-- V80/V81 pattern), collision-free vs every existing seeded PERM01..PERM195
-- range (grep-verified against every V*.sql before finalizing). Assigned
-- range PERM196..198 (this slice's full ULID allocation); PERM196/197 are
-- used below (one permission code per domain), PERM198 is reserved, unused,
-- and not inserted.
-- =====================================================================
insert into permissions (public_id, code, name, module, description) values
  ('01J0AA0000000000000PERM196', 'reporting.inventory.read',   'View inventory reports',   'reporting', 'View the Stock Summary, Product Ledger, and Stock Transfer Register reports'),
  ('01J0AA0000000000000PERM197', 'reporting.procurement.read', 'View procurement reports', 'reporting', 'View the Supplier Summary, PO/GRN/Bill Aging, and Supplier Spend reports');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in ('reporting.inventory.read', 'reporting.procurement.read')
where r.code = 'admin';
