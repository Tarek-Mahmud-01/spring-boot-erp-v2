package com.guru.erp.modules.finance.periods.domain;

/**
 * ENT-015 FiscalPeriod lifecycle (reference {@code app.business_settings.constants.FiscalPeriodStatus}
 * + the full E-009 period-end flow in {@code app.finance.views.fiscal_periods}).
 *
 * <p>Linear forward path plus the sanctioned side-moves: a validation failure or approval
 * rejection loops back to RECONCILING; CLOSED can enter ADJUSTMENT (a controlled correction
 * window) or ARCHIVED (terminal); ADJUSTMENT returns to CLOSED; a CLOSED period may also be
 * reopened straight back to OPEN (audited exception, reason mandatory — enforced in the service,
 * not the state machine, since {@link com.guru.erp.platform.status.StateMachine} has no concept of
 * a conditional reason requirement).
 */
public enum FiscalPeriodStatus {
    DRAFT,
    OPEN,
    PREPARING,
    RECONCILING,
    VALIDATING,
    PENDING_APPROVAL,
    APPROVED,
    CLOSING,
    CLOSED,
    ADJUSTMENT,
    ARCHIVED
}
