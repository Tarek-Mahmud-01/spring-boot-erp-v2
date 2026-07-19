package com.guru.erp.modules.pos.auxiliary.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PosParkedSale — a parked cart keyed by a short, human-typeable code so a lane can be freed for
 * the next customer and resumed later (US-035 FR-182..186).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. {@code transactionId} /
 * {@code registerId} / {@code locationId} are cross-slice references (POS transaction / register /
 * location — none of those aggregates are same-slice here) held as loose ULID {@code char(26)}
 * columns, per the vertical-slice rule — no hard FK. {@code parkedBy} is a loose reference to the
 * acting user's public id.
 *
 * <p>Constraints reproduced in V52: a partial unique index on {@code park_code} scoped to
 * {@code resumed_at is null} (the reference's {@code uq_parked_active_code}) — a code may be
 * reused once its park is resumed, but never collide with another still-active park.
 */
@Entity
@Table(name = "pos_parked_sales")
public class PosParkedSale extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String transactionId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "register_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String registerId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "park_code", nullable = false, length = 12)
    private String parkCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "parked_by", length = 26, columnDefinition = "char(26)")
    private String parkedBy;

    @Column(name = "parked_at", nullable = false)
    private Instant parkedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resumed_at")
    private Instant resumedAt;

    /** FR-182 — optional note the cashier left when parking (audit payload only, not a column on
     * the reference model beyond the audit dict; kept here for parity with the create request). */
    @Column(name = "note", length = 255)
    private String note;

    public PosParkedSale() {
    }

    public boolean isActive() {
        return resumedAt == null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getParkCode() {
        return parkCode;
    }

    public void setParkCode(String parkCode) {
        this.parkCode = parkCode;
    }

    public String getParkedBy() {
        return parkedBy;
    }

    public void setParkedBy(String parkedBy) {
        this.parkedBy = parkedBy;
    }

    public Instant getParkedAt() {
        return parkedAt;
    }

    public void setParkedAt(Instant parkedAt) {
        this.parkedAt = parkedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResumedAt() {
        return resumedAt;
    }

    public void setResumedAt(Instant resumedAt) {
        this.resumedAt = resumedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
