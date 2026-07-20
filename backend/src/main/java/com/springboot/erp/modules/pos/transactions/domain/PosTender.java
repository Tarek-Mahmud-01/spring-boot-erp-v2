package com.springboot.erp.modules.pos.transactions.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-PosTender — an append-only payment leg on a {@link PosTransaction} (reference
 * {@code app.pos.models.PosTender}); multi-tender/split-payment support. Undo sets
 * {@code isReversed} rather than deleting the row so the audit trail stays intact.
 *
 * <p>{@code transaction} is a real same-slice FK; the payment method is a loose cross-slice ULID
 * ref (this slice never hard-calls the payment-methods module — it only snapshots
 * {@code methodType} at tender time). Constraint reproduced in V51: {@code uq_pos_tender_sequence}.
 */
@Entity
@Table(name = "pos_tenders")
public class PosTender extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private PosTransaction transaction;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    /** ULID public id of the PaymentMethod used (cross-slice, required). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_method_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String paymentMethodId;

    /** Snapshot of the payment method's type at tender time (e.g. CASH, CARD). */
    @Column(name = "method_type", nullable = false, length = 20)
    private String methodType;

    @Column(name = "amount_amount", nullable = false)
    private long amountAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "amount_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String amountCurrency;

    /** Cash actually handed over; null for a non-cash tender (amount == tendered always). */
    @Column(name = "tendered_amount")
    private Long tenderedAmount;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount = 0L;

    @Column(name = "reference", length = 120)
    private String reference;

    @Column(name = "masked_pan", length = 8)
    private String maskedPan;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed = false;

    public PosTender() {
    }

    public PosTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(PosTransaction transaction) {
        this.transaction = transaction;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public long getAmountAmount() {
        return amountAmount;
    }

    public void setAmountAmount(long amountAmount) {
        this.amountAmount = amountAmount;
    }

    public String getAmountCurrency() {
        return amountCurrency;
    }

    public void setAmountCurrency(String amountCurrency) {
        this.amountCurrency = amountCurrency;
    }

    public Long getTenderedAmount() {
        return tenderedAmount;
    }

    public void setTenderedAmount(Long tenderedAmount) {
        this.tenderedAmount = tenderedAmount;
    }

    public long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getMaskedPan() {
        return maskedPan;
    }

    public void setMaskedPan(String maskedPan) {
        this.maskedPan = maskedPan;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }
}
