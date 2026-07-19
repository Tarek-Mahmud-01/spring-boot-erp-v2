-- =====================================================================
-- V83 — REPORTING module, "pos-loyalty" sub-slice (POS operational reports:
-- till reconciliation, register performance, refund/discount analysis; CRM
-- loyalty reports + analytics: point movements, customer segment
-- performance).
--
-- This slice is READ-ONLY and cross-cutting (ARCHITECTURE.md — reporting
-- reads other modules' already-ported entities directly via QueryDSL; it
-- owns no tables of its own). No new tables — this migration seeds ONLY the
-- permission rows the slice's @PreAuthorize checks require, granted to the
-- admin role, matching the V70/V71/V72/V80/V81/V82 pattern exactly.
--
-- Reference: guru-erp-revamp/backend/app/reports/repositories/pos_ops.py,
-- schemas/pos_ops.py, views/pos_ops.py (RPT-017 Discount Usage, RPT-018 POS
-- Daily Sales Summary, RPT-019 Till Variance, RPT-AU-004 Age Verification
-- Refusals, RPT-032 New vs Returning Customers, RPT-033 Suspended/Abandoned
-- Carts — RPT-023 Promotion Performance is out of this slice's scope, it
-- belongs to the product/promotions reporting surface) and
-- app/reports/repositories/loyalty.py, schemas/loyalty.py, views/loyalty.py
-- (RPT-022 Loyalty Ledger, RPT-021 Customer List with Consent) and
-- app/reports/repositories/loyalty_analytics.py, schemas/loyalty_analytics.py,
-- views/loyalty_analytics.py (RPT-028 Loyalty Liability & Breakage, RPT-029
-- Points Expiry Forecast, RPT-030 Tier Distribution & Migration, RPT-031
-- Customer RFM/LTV).
--
-- Fresh PERM199..200 ULIDs (exactly 26 chars, matching the V60/V70/V71/V72/
-- V80/V81/V82 pattern), collision-free vs every existing seeded PERM01..PERM197
-- range (grep-verified against every V*.sql before finalizing). Assigned range
-- PERM199..201 (this slice's full ULID allocation); PERM199/200 are used below
-- (one permission code per domain — pos-ops reports and loyalty/loyalty-
-- analytics reports share the same audience and permission per domain,
-- mirroring V82's one-code-per-domain choice), PERM201 is reserved, unused,
-- and not inserted.
-- =====================================================================
insert into permissions (public_id, code, name, module, description) values
  ('01J0AA0000000000000PERM199', 'reporting.pos.read',     'View POS operational reports', 'reporting', 'View Discount Usage, POS Daily Sales Summary, Till Variance, Age Verification Refusals, New vs Returning Customers, and Abandoned Carts reports'),
  ('01J0AA0000000000000PERM200', 'reporting.loyalty.read', 'View loyalty reports',         'reporting', 'View the Loyalty Ledger, Customer List with Consent, Loyalty Liability & Breakage, Points Expiry Forecast, Tier Distribution & Migration, and Customer RFM/LTV reports');

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
join permissions p on p.code in ('reporting.pos.read', 'reporting.loyalty.read')
where r.code = 'admin';
