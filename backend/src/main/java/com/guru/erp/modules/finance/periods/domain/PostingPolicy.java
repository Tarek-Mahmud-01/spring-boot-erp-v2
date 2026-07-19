package com.guru.erp.modules.finance.periods.domain;

/**
 * What {@code PeriodGuardService#assertCanPost} allows for a period in a given
 * {@link FiscalPeriodStatus} (reference {@code app.finance.views.fiscal_periods._PostPolicy}).
 */
public enum PostingPolicy {
    /** Normal posting permitted (period still being worked). */
    ALLOWED,
    /** Only {@code finance.period.adjust} holders may post. */
    ADJUST_ONLY,
    /** No posting at all (409). */
    BLOCKED
}
