package com.springboot.erp.modules.procurement.landed.domain;

/**
 * Landed-cost lifecycle. The reference records a landed cost and immediately posts its V-011
 * voucher + stock revaluation in one call, so the useful states here are DRAFT (recorded, not yet
 * capitalised) → APPLIED (allocations posted, revaluation event emitted) → REVERSED (on delete of
 * an applied cost). This gives the apply/reverse seam a status to gate on, while a plain create
 * lands in DRAFT so the operator can review allocations before capitalising into stock cost.
 *
 * <p>Persisted UPPERCASE via {@code @Enumerated(STRING)} (no converter needed).
 */
public enum LandedCostStatus {
    DRAFT,
    APPLIED,
    REVERSED
}
