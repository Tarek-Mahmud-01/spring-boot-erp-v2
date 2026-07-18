package com.guru.erp.modules.procurement.bills.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-033 SupplierPayment — money paid TO a supplier (US-021). A payment can settle a PO's AP as a
 * whole and may be split across methods (see {@link SupplierPaymentTender}). Confirming (APPROVED)
 * posts a V-003 payment voucher — a cross-module effect emitted via the outbox (see
 * PaymentPostingService); finance is never called directly.
 *
 * <p>Cross-slice refs (supplier, PO) are loose ULID {@code char(26)} columns. Money is bigint
 * minor units + currency; {@code baseAmount} is the base-currency conversion using
 * {@code exchangeRate}. {@code statusHistory} is a jsonb timeline of transitions (actor +
 * timestamp) for the detail page, separate from the platform audit log.
 */
@Entity
@Table(name = "supplier_payments")
public class SupplierPayment extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Supplier (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the linked PurchaseOrder; nullable (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", length = 26, columnDefinition = "char(26)")
    private String poId;

    @Column(name = "invoice_reference", length = 100)
    private String invoiceReference;

    /** ULID public id of the PaymentMethod (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_method_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String paymentMethodId;

    @Column(name = "payment_method_name", nullable = false, length = 100)
    private String paymentMethodName;

    @Column(name = "payment_date", nullable = false)
    private Instant paymentDate;

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

    /** null | "percent" | "amount" — settlement discount taken on this payment. */
    @Column(name = "discount_type", length = 10)
    private String discountType;

    @Column(name = "discount_value", nullable = false)
    private long discountValue = 0L;

    @Convert(converter = SupplierPaymentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierPaymentStatus status = SupplierPaymentStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_history", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SupplierPaymentTender> tenders = new ArrayList<>();

    public SupplierPayment() {
    }

    public void addTender(SupplierPaymentTender t) {
        t.setPayment(this);
        tenders.add(t);
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

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public String getInvoiceReference() {
        return invoiceReference;
    }

    public void setInvoiceReference(String invoiceReference) {
        this.invoiceReference = invoiceReference;
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

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
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

    public SupplierPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierPaymentStatus status) {
        this.status = status;
    }

    public List<Map<String, Object>> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<Map<String, Object>> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public List<SupplierPaymentTender> getTenders() {
        return tenders;
    }
}
