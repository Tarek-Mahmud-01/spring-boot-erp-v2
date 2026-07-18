package com.guru.erp.modules.product.promotions.domain;

import com.guru.erp.platform.status.StateMachine;

/**
 * ENT-011.lifecycle_state — FR-070. The product lifecycle state machine. This
 * slice owns the {@link LifecycleTransition} ledger (US-015 / FR-074); the
 * product's current state itself lives on the catalog slice's Product row. The
 * ledger records every state move with the actor + timestamp.
 *
 * <p>{@link #MACHINE} reproduces the reference {@code LIFECYCLE_TRANSITIONS}
 * table (constants.py): Draft is the initial state; Discontinued is terminal.
 */
public enum LifecycleState {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    RUN_OUT,
    DISCONTINUED;

    /** FR-070 — allowed transitions in the product lifecycle state machine. */
    public static final StateMachine<LifecycleState> MACHINE = StateMachine.builder(LifecycleState.class)
        .allow(DRAFT, ACTIVE)
        .allow(ACTIVE, ON_HOLD, RUN_OUT, DISCONTINUED)
        .allow(ON_HOLD, ACTIVE, RUN_OUT, DISCONTINUED)
        .allow(RUN_OUT, ACTIVE, DISCONTINUED)
        .allow(DISCONTINUED)
        .build();
}
