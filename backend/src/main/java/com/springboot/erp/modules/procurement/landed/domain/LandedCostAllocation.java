package com.springboot.erp.modules.procurement.landed.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-032a LandedCostAllocation — one line of a {@link LandedCost}'s spread: how much of the charge
 * (in base currency) landed on a specific GRN receipt line or PO order line (reference
 * {@code app.procurement.models.LandedCostAllocation}). Exactly one of {@code grnLineId} /
 * {@code poLineId} is set. {@code allocatedAmount} is the base-currency slice; the last line
 * carries the rounding remainder so the slices sum back to the cost's {@code baseAmount}.
 *
 * <p>{@code landedCost} is a real same-slice FK; the target line ids are loose cross-slice ULID
 * refs. {@code allocQty} records the qty the charge was allocated against (partial-allocation
 * input) so the edit drawer can pre-fill it.
 */
@Entity
@Table(name = "landed_cost_allocations")
public class LandedCostAllocation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "landed_cost_id", nullable = false)
    private LandedCost landedCost;

    /** ULID public id of the GoodsReceiptLine (cross-slice); null for a PO-based allocation. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_line_id", length = 26, columnDefinition = "char(26)")
    private String grnLineId;

    /** ULID public id of the PurchaseOrderLine (cross-slice); null for a GRN-based allocation. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_line_id", length = 26, columnDefinition = "char(26)")
    private String poLineId;

    /** Base-currency slice of the charge landed on this line. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "allocated_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "allocated_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money allocatedAmount;

    /** Qty of the target line the charge was allocated against (partial allocation input). */
    @Column(name = "alloc_qty", precision = 18, scale = 6)
    private BigDecimal allocQty;

    public LandedCostAllocation() {
    }

    public LandedCost getLandedCost() {
        return landedCost;
    }

    public void setLandedCost(LandedCost landedCost) {
        this.landedCost = landedCost;
    }

    public String getGrnLineId() {
        return grnLineId;
    }

    public void setGrnLineId(String grnLineId) {
        this.grnLineId = grnLineId;
    }

    public String getPoLineId() {
        return poLineId;
    }

    public void setPoLineId(String poLineId) {
        this.poLineId = poLineId;
    }

    public Money getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(Money allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getAllocQty() {
        return allocQty;
    }

    public void setAllocQty(BigDecimal allocQty) {
        this.allocQty = allocQty;
    }
}
