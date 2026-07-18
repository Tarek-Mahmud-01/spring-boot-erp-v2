package com.guru.erp.modules.product.catalog.domain;

import com.guru.erp.platform.status.StateMachine;

/**
 * ENT-011.lifecycle_state — FR-070 product lifecycle. Persisted as the constant
 * name (DRAFT/ACTIVE/...) matching the reference {@code StrEnum} value and the
 * {@code ck_products_lifecycle_state} check constraint.
 *
 * <p>{@link #MACHINE} reproduces {@code LIFECYCLE_TRANSITIONS} from the
 * reference constants: DRAFT→ACTIVE; ACTIVE→{ON_HOLD,RUN_OUT,DISCONTINUED};
 * ON_HOLD→{ACTIVE,RUN_OUT,DISCONTINUED}; RUN_OUT→{ACTIVE,DISCONTINUED};
 * DISCONTINUED is terminal.
 */
public enum LifecycleState {
    DRAFT,
    ACTIVE,
    ON_HOLD,
    RUN_OUT,
    DISCONTINUED;

    public static final StateMachine<LifecycleState> MACHINE = StateMachine.builder(LifecycleState.class)
        .allow(DRAFT, ACTIVE)
        .allow(ACTIVE, ON_HOLD, RUN_OUT, DISCONTINUED)
        .allow(ON_HOLD, ACTIVE, RUN_OUT, DISCONTINUED)
        .allow(RUN_OUT, ACTIVE, DISCONTINUED)
        .allow(DISCONTINUED)
        .build();
}
