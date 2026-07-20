package com.springboot.erp.modules.pos.registers.domain;

/**
 * PosTillMovement.type (reference {@code app.pos.constants.TillMovementType}).
 * Persisted UPPERCASE via {@code @Enumerated(STRING)} — matches the reference
 * wire values verbatim.
 *
 * <p>Cash-flow direction (reference FR-192): {@code PICKUP} and {@code SALE} add
 * cash to the drawer; {@code DROP}, {@code CHANGE}, and {@code REFUND_OUT} remove
 * it. {@code SALE} and {@code CHANGE} are system-recorded only (never posted via
 * the manual cash-movement endpoint).
 */
public enum TillMovementType {
    PICKUP,
    DROP,
    SALE,
    CHANGE,
    REFUND_OUT
}
