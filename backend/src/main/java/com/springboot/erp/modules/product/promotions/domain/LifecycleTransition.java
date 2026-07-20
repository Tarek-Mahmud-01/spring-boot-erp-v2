package com.springboot.erp.modules.product.promotions.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * FR-074 — append-only log of every product lifecycle transition (US-015). One
 * row is written per state move; rows are never updated or deleted by the
 * application (there is no update/delete endpoint), so this is an audit ledger.
 *
 * <p>The product is referenced loosely by its ULID {@code productId} (char(26))
 * — the Product row itself lives on the catalog slice, so there is no hard
 * cross-slice FK (app-layer resolution).
 *
 * <p>Domain columns only; id / publicId / audit / version / soft-delete come
 * from {@link BaseEntity}. {@code changedAt} / {@code changedBy} are kept as
 * explicit domain columns to faithfully reproduce the reference ledger shape
 * (they are the semantic transition timestamp + actor, distinct from the
 * generic BaseEntity auditing columns).
 *
 * <p>Constraints reproduced in V22__product_promotions.sql:
 * {@code ck_lifecycle_transitions_from_state} / {@code ..._to_state} check the
 * enum values.
 */
@Entity
@Table(name = "product_lifecycle_transitions")
public class LifecycleTransition extends BaseEntity {

    /** ULID of the product whose state moved (cross-slice reference — no hard FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", nullable = false, length = 16)
    private LifecycleState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 16)
    private LifecycleState toState;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** ULID of the acting user (loose reference — no hard FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "changed_by", length = 26, columnDefinition = "char(26)")
    private String changedBy;

    public LifecycleTransition() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public LifecycleState getFromState() {
        return fromState;
    }

    public void setFromState(LifecycleState fromState) {
        this.fromState = fromState;
    }

    public LifecycleState getToState() {
        return toState;
    }

    public void setToState(LifecycleState toState) {
        this.toState = toState;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
