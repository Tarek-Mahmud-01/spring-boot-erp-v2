package com.guru.erp.modules.settings.numbering.domain;

/**
 * ENT-006.reset_cadence (FR-022). Controls when the per-window counter resets:
 * never, at each fiscal year, or each calendar month. Persisted by {@code name()}
 * into a checked {@code varchar(10)} column.
 */
public enum ResetCadence {
    NEVER,
    YEARLY,
    MONTHLY
}
