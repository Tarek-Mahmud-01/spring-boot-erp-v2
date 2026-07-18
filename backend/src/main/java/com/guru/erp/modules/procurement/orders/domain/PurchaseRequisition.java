package com.guru.erp.modules.procurement.orders.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-027 PurchaseRequisition (US-017 / FR-080–086) — header + lines aggregate with an approval
 * workflow (DRAFT → SUBMITTED → UNDER_REVIEW → SENT_TO_SUPPLIER → CONVERTED, or REJECTED).
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (location,
 * header supplier, requester/buyer user) are held as loose ULID {@code char(26)} columns — no hard
 * cross-slice FK, per the vertical-slice rule. Money is long minor units (see the line entity).
 * Constraints reproduced in V41: unique {@code number}, status check, and supporting indexes.
 */
@Entity
@Table(name = "purchase_requisitions")
public class PurchaseRequisition extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Location (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** ULID public id of the requesting user (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "requester_user_id", length = 26, columnDefinition = "char(26)")
    private String requesterUserId;

    /** Header-level supplier chosen by the requester; seeds convert-to-PO (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** Header-level currency; PR lines carry their own unit-price currency. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "needed_by_date")
    private Instant neededByDate;

    @Convert(converter = PrStatusConverter.class)
    @Column(name = "status", nullable = false, length = 15)
    private PrStatus status = PrStatus.DRAFT;

    /** ULID public id of the assigned buyer; null until assigned (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "assigned_buyer_id", length = 26, columnDefinition = "char(26)")
    private String assignedBuyerId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "request_date")
    private Instant requestDate;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "invoice_discount_type", length = 10)
    private String invoiceDiscountType;

    @Column(name = "invoice_discount_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal invoiceDiscountValue = BigDecimal.ZERO;

    /** Header total, minor units of the header currency (frontend-precomputed). */
    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0L;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "requisition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<PurchaseRequisitionLine> lines = new ArrayList<>();

    public PurchaseRequisition() {
    }

    public void addLine(PurchaseRequisitionLine line) {
        line.setRequisition(this);
        lines.add(line);
    }

    public void clearLines() {
        lines.clear();
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(String requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getNeededByDate() {
        return neededByDate;
    }

    public void setNeededByDate(Instant neededByDate) {
        this.neededByDate = neededByDate;
    }

    public PrStatus getStatus() {
        return status;
    }

    public void setStatus(PrStatus status) {
        this.status = status;
    }

    public String getAssignedBuyerId() {
        return assignedBuyerId;
    }

    public void setAssignedBuyerId(String assignedBuyerId) {
        this.assignedBuyerId = assignedBuyerId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Instant requestDate) {
        this.requestDate = requestDate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getInvoiceDiscountType() {
        return invoiceDiscountType;
    }

    public void setInvoiceDiscountType(String invoiceDiscountType) {
        this.invoiceDiscountType = invoiceDiscountType;
    }

    public BigDecimal getInvoiceDiscountValue() {
        return invoiceDiscountValue;
    }

    public void setInvoiceDiscountValue(BigDecimal invoiceDiscountValue) {
        this.invoiceDiscountValue = invoiceDiscountValue;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PurchaseRequisitionLine> getLines() {
        return lines;
    }
}
