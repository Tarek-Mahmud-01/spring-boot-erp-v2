package com.springboot.erp.modules.pos.registers.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * PosTillMovement — an append-only cash movement within a {@link PosTillSession}
 * (US-037 / FR-192): manual {@code PICKUP}/{@code DROP} recorded by the cashier,
 * plus system-recorded {@code SALE}/{@code CHANGE}/{@code REFUND_OUT} legs.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity} (rows are never
 * updated after insert — the "append-only" ledger). {@code transactionId} is a
 * cross-module loose ULID reference to the POS transaction that produced a
 * {@code SALE}/{@code CHANGE}/{@code REFUND_OUT} leg; null for a manual
 * {@code PICKUP}/{@code DROP}. Constraints reproduced in V50: type check.
 */
@Entity
@Table(name = "pos_till_movements")
public class PosTillMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "till_session_id", nullable = false)
    private PosTillSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 12)
    private TillMovementType type;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "amount_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money amount;

    /** ULID public id of the POS transaction this movement was posted from (cross-module, loose). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transaction_id", length = 26, columnDefinition = "char(26)")
    private String transactionId;

    @Column(name = "note", length = 255)
    private String note;

    public PosTillMovement() {
    }

    public PosTillSession getSession() {
        return session;
    }

    public void setSession(PosTillSession session) {
        this.session = session;
    }

    public TillMovementType getType() {
        return type;
    }

    public void setType(TillMovementType type) {
        this.type = type;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
