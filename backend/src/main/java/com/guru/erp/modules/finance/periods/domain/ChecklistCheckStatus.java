package com.guru.erp.modules.finance.periods.domain;

/** Result of the last automated evaluation of a {@link PeriodChecklistItem}. */
public enum ChecklistCheckStatus {
    /** Not yet evaluated. */
    PENDING,
    PASSED,
    FAILED
}
