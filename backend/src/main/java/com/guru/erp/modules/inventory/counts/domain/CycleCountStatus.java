package com.guru.erp.modules.inventory.counts.domain;

/**
 * ENT-044 CycleCountPlan lifecycle (reference app.inventory.constants.CycleCountStatus).
 *
 * <p>Persisted via {@link CycleCountStatusConverter} as the human wire value
 * ("Draft" / "In Progress" / "Completed" / "Approved") to satisfy the
 * {@code ck_cycle_count_plans_status} check constraint. The lifecycle the
 * reference walks is Draft -&gt; In Progress -&gt; Completed -&gt; Approved
 * (a new plan is created Draft then immediately flipped to In Progress once its
 * lines are generated).
 */
public enum CycleCountStatus {
    DRAFT("Draft"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    APPROVED("Approved");

    private final String wire;

    CycleCountStatus(String wire) {
        this.wire = wire;
    }

    public String wire() {
        return wire;
    }

    public static CycleCountStatus fromWire(String value) {
        for (CycleCountStatus s : values()) {
            if (s.wire.equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown cycle count status: " + value);
    }
}
