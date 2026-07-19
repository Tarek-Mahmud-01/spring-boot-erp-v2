package com.guru.erp.modules.pos.auxiliary.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PosEvent — an append-only POS-domain event/audit trail (age verification, offline sync
 * conflicts, manual discounts, peripheral failures, manager overrides) kept for
 * compliance/review and receipt/journal purposes (FR-AU-013 / FR-198 / FR-25.8).
 *
 * <p>Distinct from the platform {@code audit_logs} hash-chained mutation trail
 * ({@link com.guru.erp.platform.audit.AuditService}) — that one records generic before/after
 * entity mutations; this one is a POS-specific operational timeline the till / back-office review
 * screen reads directly, keyed by event {@code type} with a free-form JSON {@code payload}.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. {@code transactionId} /
 * {@code registerId} are optional cross-slice references (POS transaction / register), held as
 * loose ULID {@code char(26)} columns — no hard FK. {@code reviewedBy} is a loose reference to the
 * reviewing user's public id.
 */
@Entity
@Table(name = "pos_events")
public class PosEvent extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_id", length = 26, columnDefinition = "char(26)")
    private String transactionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "register_id", length = 26, columnDefinition = "char(26)")
    private String registerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private PosEventType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview = false;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reviewed_by", length = 26, columnDefinition = "char(26)")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public PosEvent() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getRegisterId() {
        return registerId;
    }

    public void setRegisterId(String registerId) {
        this.registerId = registerId;
    }

    public PosEventType getType() {
        return type;
    }

    public void setType(PosEventType type) {
        this.type = type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? Map.of() : payload;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
