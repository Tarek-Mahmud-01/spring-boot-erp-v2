package com.guru.erp.modules.procurement.returns.domain;

import com.guru.erp.platform.entity.BaseEntity;
import com.guru.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-031 SupplierReturn (US-020 / FR-109–113) — a supplier return (debit note) raised against a
 * goods receipt (GRN). Header + lines aggregate; the status workflow is DRAFT → CONFIRMED and, on
 * CONFIRMED, the reference relieves on-hand stock and posts the V-007 debit-note journal. Those
 * cross-slice effects are emitted here as outbox events (see {@code ReturnPostingService}) — never a
 * hard call into the inventory / finance modules.
 *
 * <p>Domain columns only; base columns from {@link BaseEntity}. Cross-slice references (supplier,
 * GRN) are held as loose ULID {@code char(26)} columns — no hard cross-slice FK, per the
 * vertical-slice rule. {@code debitNote} / {@code baseDebitNote} are {@link Money} (long minor
 * units + currency). {@code exchangeRate} is the FX trail used to derive the base-currency debit
 * note. Constraints reproduced in V44: status check + the {@code returned_at} index.
 */
@Entity
@Table(name = "supplier_returns")
public class SupplierReturn extends BaseEntity {

    @Column(name = "number", nullable = false, unique = true, length = 30)
    private String number;

    /** ULID public id of the Supplier (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the source GoodsReceipt (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String grnId;

    @Column(name = "returned_at", nullable = false)
    private Instant returnedAt;

    @Convert(converter = ReturnStatusConverter.class)
    @Column(name = "status", nullable = false, length = 12)
    private ReturnStatus status = ReturnStatus.DRAFT;

    /** Transaction-currency debit note (net + tax), minor units + currency. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "debit_note_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "debit_note_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money debitNote;

    /** FX rate resolved at creation against the GRN's company + {@code returnedAt}. */
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private java.math.BigDecimal exchangeRate = java.math.BigDecimal.ONE;

    /** Base-currency debit note the V-007 voucher posts (debitNote * exchangeRate). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "base_debit_note_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "base_debit_note_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money baseDebitNote;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SupplierReturnLine> lines = new ArrayList<>();

    public SupplierReturn() {
    }

    public void addLine(SupplierReturnLine line) {
        line.setSupplierReturn(this);
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

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnStatus status) {
        this.status = status;
    }

    public Money getDebitNote() {
        return debitNote;
    }

    public void setDebitNote(Money debitNote) {
        this.debitNote = debitNote;
    }

    public java.math.BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(java.math.BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Money getBaseDebitNote() {
        return baseDebitNote;
    }

    public void setBaseDebitNote(Money baseDebitNote) {
        this.baseDebitNote = baseDebitNote;
    }

    public List<SupplierReturnLine> getLines() {
        return lines;
    }
}
