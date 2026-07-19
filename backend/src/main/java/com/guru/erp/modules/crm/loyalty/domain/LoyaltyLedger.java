package com.guru.erp.modules.crm.loyalty.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-071 LoyaltyLedger — append-only point movements (reference
 * {@code LoyaltyLedger}, FR-209), the closest analog to {@code StockLedger}:
 * every point change is one immutable row; corrections are new REVERSE /
 * EXPIRE rows, never mutation of an existing EARN row (except its own
 * {@code remaining} FIFO bookkeeping field, updated in place by the service
 * that consumes it — the row's signed amount / type / occurred-at never
 * change once written).
 *
 * <p>{@code pointsSigned} is positive on EARN, negative on REDEEM / EXPIRE /
 * REVERSE. {@code remaining} is FIFO bookkeeping: for EARN rows, how many of
 * those points are still unspent (consumed by REDEEM / EXPIRE / REVERSE);
 * always {@code 0} for non-EARN rows.
 *
 * <p>{@code customerId} is a loose ULID cross-slice ref. {@code sourceTransactionId}
 * loosely references the {@link CustomerTransaction} (or POS sale) that produced
 * the movement; nullable for manual adjustments.
 *
 * <p>Constraints reproduced in V61__crm_loyalty.sql:
 * <ul>
 *   <li>{@code ck_loyalty_ledger_type} — type in the four movement buckets.</li>
 *   <li>Indexes on {@code customer_id}, {@code occurred_at}, {@code source_transaction_id}.</li>
 * </ul>
 */
@Entity
@Table(name = "loyalty_ledger")
public class LoyaltyLedger extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "customer_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 8)
    private LoyaltyMovementType type;

    /** Signed: positive on EARN, negative on REDEEM / EXPIRE / REVERSE. */
    @Column(name = "points_signed", nullable = false)
    private long pointsSigned;

    /** FIFO bookkeeping: unspent points from this EARN lot. Always 0 for non-EARN rows. */
    @Column(name = "remaining", nullable = false)
    private long remaining = 0;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_transaction_id", length = 26, columnDefinition = "char(26)")
    private String sourceTransactionId;

    @Column(name = "description", length = 255)
    private String description;

    /** ULID public id of the acting user; null when system-generated. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actor_user_id", length = 26, columnDefinition = "char(26)")
    private String actorUserId;

    public LoyaltyLedger() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public LoyaltyMovementType getType() {
        return type;
    }

    public void setType(LoyaltyMovementType type) {
        this.type = type;
    }

    public long getPointsSigned() {
        return pointsSigned;
    }

    public void setPointsSigned(long pointsSigned) {
        this.pointsSigned = pointsSigned;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getSourceTransactionId() {
        return sourceTransactionId;
    }

    public void setSourceTransactionId(String sourceTransactionId) {
        this.sourceTransactionId = sourceTransactionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }
}
