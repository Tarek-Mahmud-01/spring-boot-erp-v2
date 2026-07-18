package com.guru.erp.modules.procurement.bills.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-034 AmountReceived — money received back FROM a supplier (US-021c): a refund or credit-note
 * settlement after a confirmed Purchase Return. Mirrors {@link SupplierPayment} on the inflow side.
 * Confirming posts a V-004 receipt voucher — a cross-module effect emitted via the outbox (see
 * AmountReceivedPostingService); finance is never called directly.
 *
 * <p>Cross-slice refs (supplier, purchase return, PO, payment method) are loose ULID
 * {@code char(26)} columns. {@code statusHistory} is a jsonb transition timeline.
 */
@Entity
@Table(name = "amount_received")
public class AmountReceived extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Supplier (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the source SupplierReturn; nullable (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "purchase_return_id", length = 26, columnDefinition = "char(26)")
    private String purchaseReturnId;

    /** ULID public id of the linked PurchaseOrder; nullable (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", length = 26, columnDefinition = "char(26)")
    private String poId;

    @Column(name = "credit_note_reference", length = 100)
    private String creditNoteReference;

    /** ULID public id of the PaymentMethod (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_method_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String paymentMethodId;

    @Column(name = "payment_method_name", nullable = false, length = 100)
    private String paymentMethodName;

    @Column(name = "received_date", nullable = false)
    private Instant receivedDate;

    @Column(name = "amount_amount", nullable = false)
    private long amountAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "amount_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String amountCurrency;

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "base_amount", nullable = false)
    private long baseAmount = 0L;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** null | "percent" | "amount" — settlement discount on this receipt. */
    @Column(name = "discount_type", length = 10)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private long discountValue = 0L;

    @Convert(converter = AmountReceivedStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private AmountReceivedStatus status = AmountReceivedStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_history", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> statusHistory = new ArrayList<>();

    public AmountReceived() {
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getPurchaseReturnId() {
        return purchaseReturnId;
    }

    public void setPurchaseReturnId(String purchaseReturnId) {
        this.purchaseReturnId = purchaseReturnId;
    }

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public String getCreditNoteReference() {
        return creditNoteReference;
    }

    public void setCreditNoteReference(String creditNoteReference) {
        this.creditNoteReference = creditNoteReference;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getPaymentMethodName() {
        return paymentMethodName;
    }

    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
    }

    public Instant getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Instant receivedDate) {
        this.receivedDate = receivedDate;
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

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(long baseAmount) {
        this.baseAmount = baseAmount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(long discountValue) {
        this.discountValue = discountValue;
    }

    public AmountReceivedStatus getStatus() {
        return status;
    }

    public void setStatus(AmountReceivedStatus status) {
        this.status = status;
    }

    public List<Map<String, Object>> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<Map<String, Object>> statusHistory) {
        this.statusHistory = statusHistory;
    }
}
