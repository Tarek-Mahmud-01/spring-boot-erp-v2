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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-030 SupplierBill — the AP invoice header (US-018 / FR-098–101). Header + lines aggregate;
 * lines and the GRN / PO link rows cascade. Posting the payable is a cross-module effect emitted
 * via the outbox (see BillPostingService) — finance is never called directly.
 *
 * <p>Domain columns only; id/publicId/audit/version/soft-delete come from {@link BaseEntity}.
 * Cross-slice references (supplier, PO) are loose ULID {@code char(26)} columns — no hard FK.
 * Money is stored as bigint minor units + a header currency column, matching the reference.
 * Constraints reproduced in V43.
 */
@Entity
@Table(name = "supplier_bills")
public class SupplierBill extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Supplier (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the "primary" PurchaseOrder; null for a standalone bill (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", length = 26, columnDefinition = "char(26)")
    private String poId;

    @Column(name = "supplier_bill_no", length = 100)
    private String supplierBillNo;

    @Column(name = "bill_date", nullable = false)
    private Instant billDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "subtotal_amount", nullable = false)
    private long subtotalAmount = 0L;

    @Column(name = "tax_amount", nullable = false)
    private long taxAmount = 0L;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount = 0L;

    @Convert(converter = BillStatusConverter.class)
    @Column(name = "status", nullable = false, length = 25)
    private BillStatus status = BillStatus.DRAFT;

    /** Overall 3-way match outcome (wire label of {@link MatchStatus}); nullable. */
    @Column(name = "match_status", length = 20)
    private String matchStatus;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SupplierBillLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SupplierBillGrnLink> grnLinks = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SupplierBillPoLink> poLinks = new ArrayList<>();

    public SupplierBill() {
    }

    public void addLine(SupplierBillLine line) {
        line.setBill(this);
        lines.add(line);
    }

    public void clearLines() {
        lines.clear();
    }

    public void addGrnLink(SupplierBillGrnLink link) {
        link.setBill(this);
        grnLinks.add(link);
    }

    public void addPoLink(SupplierBillPoLink link) {
        link.setBill(this);
        poLinks.add(link);
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

    public String getSupplierBillNo() {
        return supplierBillNo;
    }

    public void setSupplierBillNo(String supplierBillNo) {
        this.supplierBillNo = supplierBillNo;
    }

    public Instant getBillDate() {
        return billDate;
    }

    public void setBillDate(Instant billDate) {
        this.billDate = billDate;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
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

    public long getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(long subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SupplierBillLine> getLines() {
        return lines;
    }

    public List<SupplierBillGrnLink> getGrnLinks() {
        return grnLinks;
    }

    public List<SupplierBillPoLink> getPoLinks() {
        return poLinks;
    }
}
