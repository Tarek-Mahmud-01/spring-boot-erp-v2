-- =====================================================================
-- V80 — REPORTING module, "dashboard" sub-slice (executive dashboard +
-- sales-by-product + POS transaction register).
--
-- This slice is READ-ONLY and cross-cutting (ARCHITECTURE.md — reporting
-- reads other modules' already-ported entities directly via QueryDSL; it
-- owns no tables of its own). No new tables — this migration seeds ONLY
-- the permission rows the slice's @PreAuthorize checks require, granted to
-- the admin role, matching the V70/V71/V72 pattern exactly.
--
-- Reference: guru-erp-revamp/backend/app/reports/repositories/dashboard.py,
-- schemas/dashboard.py, views/dashboard.py, repositories/sales.py,
-- schemas/sales.py, views/sales.py, repositories/pos_txn.py,
-- schemas/pos_txn.py, views/pos_txn.py.
--
-- Fresh PERM190..192 ULIDs (exactly 26 chars, matching the V60/V70/V71/V72
-- pattern), collision-free vs every existing seeded PERM01..PERM181 range
-- (grep-verified against every V*.sql before finalizing). Assigned range
-- PERM190..192 (3 codes, this slice's full ULID allocation); all 3 are used
-- below.
-- =====================================================================
insert into permissions (public_id, code, name, module, description) values
  ('01J0AA0000000000000PERM190', 'reporting.dashboard.read', 'View executive dashboard',    'reporting', 'View the KPI-tile executive dashboard summary (revenue, transaction count, top products, unposted journals)'),
  ('01J0AA0000000000000PERM191', 'reporting.sales.read',     'View sales reports',          'reporting', 'View the Sales by Product report and its roll-up summary'),
  ('01J0AA0000000000000PERM192', 'reporting.pos_txn.read',   'View POS transaction register','reporting', 'View the POS transaction register report (header + lines, sales and refunds)');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in (
    'reporting.dashboard.read', 'reporting.sales.read', 'reporting.pos_txn.read')
where r.code = 'admin';
