package com.guru.erp.modules.finance.periods.domain;

/**
 * Stable checklist item keys — one per evaluator in {@code PeriodChecklistService}
 * (reference {@code app.finance.models_period_close.ChecklistItemKey}).
 */
public enum ChecklistItemKey {
    TRIAL_BALANCE_BALANCED,
    NO_DRAFT_JOURNALS,
    NO_DRAFT_BILLS,
    NO_DRAFT_POS,
    NO_NEGATIVE_STOCK,
    POS_GL_RECONCILED
}
