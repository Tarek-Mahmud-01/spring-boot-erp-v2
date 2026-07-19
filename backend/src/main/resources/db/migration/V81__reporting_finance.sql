-- =====================================================================
-- V81 — REPORTING module, "finance" sub-slice (trial balance, GL account
-- ledger, income statement, balance sheet, cash book, payments & receipts).
--
-- This slice is READ-ONLY and cross-cutting (ARCHITECTURE.md — reporting
-- reads other modules' already-ported entities directly via QueryDSL; it
-- owns no tables of its own). No new tables — this migration seeds ONLY
-- the permission row the slice's @PreAuthorize checks require, granted to
-- the admin role, matching the V70/V71/V72/V80 pattern exactly.
--
-- Reference: guru-erp-revamp/backend/app/reports/repositories/finance.py,
-- schemas/finance.py, views/finance.py (Account Ledger / Trial Balance /
-- Payments & Receipts / Cash Book / Income Statement / Balance Sheet —
-- VAT/GST and BAS reports in the same reference file are out of this
-- slice's scope; they depend on modules — tax_codes line-level detail,
-- procurement bill capital-item flags — not part of this port).
--
-- Fresh PERM193 ULID (exactly 26 chars, matching the V60/V70/V71/V72/V80
-- pattern), collision-free vs every existing seeded PERM01..PERM192 range
-- (grep-verified against every V*.sql before finalizing). Assigned range
-- PERM193..195; only PERM193 is used below (one permission code covers
-- every report endpoint in this slice) — PERM194/195 are reserved,
-- unused ULIDs in this migration's allocation, not inserted.
-- =====================================================================
insert into permissions (public_id, code, name, module, description) values
  ('01J0AA0000000000000PERM193', 'reporting.finance.read', 'View finance reports', 'reporting',
   'View finance reports: account ledger, trial balance, payments & receipts, cash book, income statement, balance sheet');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code = 'reporting.finance.read'
where r.code = 'admin';
