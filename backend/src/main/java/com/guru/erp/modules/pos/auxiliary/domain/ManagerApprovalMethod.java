package com.guru.erp.modules.pos.auxiliary.domain;

/**
 * PosRefund.managerApprovalMethod (reference {@code MANAGER_METHOD_PASSWORD} /
 * {@code MANAGER_METHOD_OFFLINE_OPERATOR} constants in {@code app.pos.views}) — how the approving
 * manager was authenticated for a no-receipt refund (US-034 FR-178). {@code null} on a
 * receipt-linked refund (no manager approval required).
 */
public enum ManagerApprovalMethod {
    /** The manager re-entered their credentials at the till for a live online step-up. */
    PASSWORD,
    /** Refund captured offline and replayed by an operator holding the permission themselves. */
    OFFLINE_OPERATOR
}
