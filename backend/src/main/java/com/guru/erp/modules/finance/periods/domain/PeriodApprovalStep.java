package com.guru.erp.modules.finance.periods.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * E-009 Phase 4 — one step in a {@link FiscalPeriod}'s close-approval chain (reference
 * {@code app.finance.models_period_close.PeriodApprovalStep}). Steps are approved strictly in
 * {@code sequence} order; the period cannot advance PENDING_APPROVAL -&gt; APPROVED until every step
 * is {@link ApprovalStepStatus#APPROVED}. No segregation of duties — any {@code finance.period.close}
 * holder may approve any step (the platform {@link com.guru.erp.platform.status.StateMachine} drives
 * the per-step PENDING -&gt; APPROVED/REJECTED transition; the sequence-ordering rule is an extra
 * service-layer gate on top since the state machine has no notion of sibling ordering).
 *
 * <p>Same-slice child of {@link FiscalPeriod} — real FK.
 */
@Entity
@Table(name = "period_approval_steps")
public class PeriodApprovalStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Column(name = "sequence_no", nullable = false)
    private int sequence;

    /** Display label only (e.g. "Financial Controller") — NOT a distinct permission. */
    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "approver_user_id", length = 26, columnDefinition = "char(26)")
    private String approverUserId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "comment", length = 500)
    private String comment;

    public PeriodApprovalStep() {
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ApprovalStepStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStepStatus status) {
        this.status = status;
    }

    public String getApproverUserId() {
        return approverUserId;
    }

    public void setApproverUserId(String approverUserId) {
        this.approverUserId = approverUserId;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
