package com.guru.erp.modules.pos.auxiliary.domain;

import com.guru.erp.platform.entity.BaseEntity;
import com.guru.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PosRefund — refund metadata attached to a REFUND-type POS transaction, full or partial
 * (US-034 FR-177..180).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. {@code transactionId} (the REFUND
 * transaction this metadata belongs to) and {@code originalTransactionId} (the SALE being refunded
 * against, null for a no-receipt refund) are cross-slice references to the (not-yet-ported)
 * PosTransaction aggregate, held as loose ULID {@code char(26)} columns — no hard FK, per the
 * vertical-slice rule. {@code managerApprovalBy} is a loose reference to the VERIFIED approving
 * manager's user public id.
 *
 * <p>Constraints reproduced in V52: {@code mode} check (enforced by the Java enum + DB check for
 * defence in depth).
 */
@Entity
@Table(name = "pos_refunds")
public class PosRefund extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String transactionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "original_transaction_id", length = 26, columnDefinition = "char(26)")
    private String originalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private RefundMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "priced_from", length = 16)
    private RefundPricedFrom pricedFrom;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "manager_approval_by", length = 26, columnDefinition = "char(26)")
    private String managerApprovalBy;

    @Column(name = "manager_approval_at")
    private Instant managerApprovalAt;

    /** US-034 FR-178 — how the approving manager was authenticated. Null on a receipt-linked
     * refund (no manager approval required) or on a legacy row. */
    @Enumerated(EnumType.STRING)
    @Column(name = "manager_approval_method", length = 24)
    private ManagerApprovalMethod managerApprovalMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "total_refund_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "total_refund_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money totalRefund;

    @Column(name = "reason", length = 255)
    private String reason;

    public PosRefund() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    public RefundMode getMode() {
        return mode;
    }

    public void setMode(RefundMode mode) {
        this.mode = mode;
    }

    public RefundPricedFrom getPricedFrom() {
        return pricedFrom;
    }

    public void setPricedFrom(RefundPricedFrom pricedFrom) {
        this.pricedFrom = pricedFrom;
    }

    public String getManagerApprovalBy() {
        return managerApprovalBy;
    }

    public void setManagerApprovalBy(String managerApprovalBy) {
        this.managerApprovalBy = managerApprovalBy;
    }

    public Instant getManagerApprovalAt() {
        return managerApprovalAt;
    }

    public void setManagerApprovalAt(Instant managerApprovalAt) {
        this.managerApprovalAt = managerApprovalAt;
    }

    public ManagerApprovalMethod getManagerApprovalMethod() {
        return managerApprovalMethod;
    }

    public void setManagerApprovalMethod(ManagerApprovalMethod managerApprovalMethod) {
        this.managerApprovalMethod = managerApprovalMethod;
    }

    public Money getTotalRefund() {
        return totalRefund;
    }

    public void setTotalRefund(Money totalRefund) {
        this.totalRefund = totalRefund;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isManagerApproved() {
        return managerApprovalBy != null;
    }
}
