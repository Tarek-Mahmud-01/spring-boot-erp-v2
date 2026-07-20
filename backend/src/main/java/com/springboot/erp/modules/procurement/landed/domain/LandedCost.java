package com.springboot.erp.modules.procurement.landed.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * ENT-032 LandedCost — one freight / duty / clearance charge on an import, allocated across the
 * target PO or GRN lines (reference {@code app.procurement.models.LandedCost}). One drawer submit
 * with N charge lines creates N rows sharing an {@code invoiceNumber}; each row spreads its own
 * {@code amount} across the lines via {@link LandedCostAllocation} rows using an
 * {@link AllocationBasis}.
 *
 * <p>{@code amount} / {@code baseAmount} are {@link Money}; {@code exchangeRate} converts charge
 * currency → company base currency. The primary GRN / PO / supplier are held as loose ULID
 * {@code char(26)} cross-slice refs; the full linked set is held in {@link LandedCostGrnLink} /
 * {@link LandedCostPoLink}. Applying the cost capitalises it into stock cost — an outbox
 * revaluation event, never a hard call into inventory (see {@code LandedCostPostingService}).
 *
 * <p>Constraints reproduced in V45: {@code ck_landed_costs_amount_positive} plus the
 * grn/po/supplier/invoice indexes.
 */
@Entity
@Table(name = "landed_costs")
public class LandedCost extends BaseEntity {

    /** ULID public id of the primary GoodsReceipt (cross-slice); null for a PO-based cost. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_id", length = 26, columnDefinition = "char(26)")
    private String grnId;

    /** ULID public id of the primary PurchaseOrder (cross-slice); null for a GRN-based cost. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_id", length = 26, columnDefinition = "char(26)")
    private String poId;

    /** Logical invoice grouping — sibling charge rows share this number (LC-YYYY-NNNN). */
    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 30)
    private LandedCostChargeType chargeType;

    /** ULID public id of the charge's Supplier (cross-slice); optional. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** Charge amount in its own currency (must be positive). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money amount;

    /** Rate to convert charge currency → company base currency (1 when same currency). */
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** Charge amount converted to base currency (drives the GL / revaluation). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "base_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "base_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money baseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_basis", nullable = false, length = 10)
    private AllocationBasis allocationBasis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private LandedCostStatus status = LandedCostStatus.DRAFT;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @OneToMany(mappedBy = "landedCost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<LandedCostAllocation> allocations = new ArrayList<>();

    public LandedCost() {
    }

    public void addAllocation(LandedCostAllocation allocation) {
        allocation.setLandedCost(this);
        allocations.add(allocation);
    }

    public void clearAllocations() {
        allocations.clear();
    }

    public String getGrnId() {
        return grnId;
    }

    public void setGrnId(String grnId) {
        this.grnId = grnId;
    }

    public String getPoId() {
        return poId;
    }

    public void setPoId(String poId) {
        this.poId = poId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LandedCostChargeType getChargeType() {
        return chargeType;
    }

    public void setChargeType(LandedCostChargeType chargeType) {
        this.chargeType = chargeType;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Money getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(Money baseAmount) {
        this.baseAmount = baseAmount;
    }

    public AllocationBasis getAllocationBasis() {
        return allocationBasis;
    }

    public void setAllocationBasis(AllocationBasis allocationBasis) {
        this.allocationBasis = allocationBasis;
    }

    public LandedCostStatus getStatus() {
        return status;
    }

    public void setStatus(LandedCostStatus status) {
        this.status = status;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(Instant allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public List<LandedCostAllocation> getAllocations() {
        return allocations;
    }
}
