package com.springboot.erp.modules.procurement.orders.domain;

import com.springboot.erp.platform.entity.BaseEntity;
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
 * ENT-028 PurchaseOrder (US-018 / FR-087–093) — header + lines aggregate with a status workflow
 * (DRAFT → SUBMITTED → APPROVED → SENT → RECEIVED → CLOSED, plus the frontend shortcut edges and
 * CANCELLED) and amendment history via {@link PurchaseOrderVersion}.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (supplier,
 * location, source PR) are held as loose ULID {@code char(26)} columns — no hard cross-slice FK.
 * {@code poVersion} is the amendment sequence (distinct from the optimistic-lock {@code version}
 * on {@link BaseEntity}). Constraints reproduced in V41: unique {@code number}, status check,
 * {@code created_at} index.
 */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Supplier (cross-slice, resolved app-side). Required. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the Location (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "po_date", nullable = false)
    private Instant poDate;

    @Column(name = "expected_delivery_date")
    private Instant expectedDeliveryDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    /** ULID public id of the originating PurchaseRequisition; null for a direct PO (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_pr_id", length = 26, columnDefinition = "char(26)")
    private String sourcePrId;

    @Convert(converter = PoStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private PoStatus status = PoStatus.DRAFT;

    @Column(name = "close_reason", length = 500)
    private String closeReason;

    /** Amendment sequence number (FR-092); starts at 1, bumped on each amendment. */
    @Column(name = "po_version", nullable = false)
    private int poVersion = 1;

    @Column(name = "invoice_discount_type", length = 10)
    private String invoiceDiscountType;

    @Column(name = "invoice_discount_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal invoiceDiscountValue = BigDecimal.ZERO;

    @Column(name = "is_direct", nullable = false)
    private boolean direct = false;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo asc")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    /**
     * FR-092 amendment history. Append-only: a new snapshot is added on create and on each
     * amendment; never cleared. Cascade persists new versions when the PO is saved.
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNo asc")
    private List<PurchaseOrderVersion> versions = new ArrayList<>();

    public PurchaseOrder() {
    }

    public void addLine(PurchaseOrderLine line) {
        line.setPurchaseOrder(this);
        lines.add(line);
    }

    public void clearLines() {
        lines.clear();
    }

    /** Append a version snapshot (FR-092); wires the back-reference. */
    public void addVersion(PurchaseOrderVersion version) {
        version.setPurchaseOrder(this);
        versions.add(version);
    }

    public List<PurchaseOrderVersion> getVersions() {
        return versions;
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Instant getPoDate() {
        return poDate;
    }

    public void setPoDate(Instant poDate) {
        this.poDate = poDate;
    }

    public Instant getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(Instant expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getSourcePrId() {
        return sourcePrId;
    }

    public void setSourcePrId(String sourcePrId) {
        this.sourcePrId = sourcePrId;
    }

    public PoStatus getStatus() {
        return status;
    }

    public void setStatus(PoStatus status) {
        this.status = status;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public int getPoVersion() {
        return poVersion;
    }

    public void setPoVersion(int poVersion) {
        this.poVersion = poVersion;
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

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PurchaseOrderLine> getLines() {
        return lines;
    }
}
