package com.guru.erp.modules.inventory.movements.domain;

import com.guru.erp.platform.entity.BaseEntity;
import com.guru.erp.platform.money.Money;
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
 * ENT-043a StockAdjustmentLine — one product line of a {@link StockAdjustment}. Quantities are
 * {@code numeric(18,6)} {@link BigDecimal} (reference precision). {@code unitCost} is
 * {@link Money} (long minor units + currency) — the operator-supplied cost, 0 lets the posting
 * step fall back to moving-average.
 *
 * <p>{@code adjustment} is a real same-slice FK; product / uom / variant are loose cross-slice
 * ULID refs. Constraints reproduced in the migration: {@code uq_adjustment_lines_adj_line} plus
 * {@code ck_adjustment_lines_qty_counted_non_negative}.
 */
@Entity
@Table(name = "stock_adjustment_lines")
public class StockAdjustmentLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "adjustment_id", nullable = false)
    private StockAdjustment adjustment;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** ULID public id of the Product (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the UnitOfMeasure; null = product default (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uom_id", length = 26, columnDefinition = "char(26)")
    private String uomId;

    /** ULID public id of the ProductVariant; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    /** Snapshot label of the variant at create time (attributes joined, else SKU). */
    @Column(name = "variant_name", length = 500)
    private String variantName;

    @Column(name = "qty_counted", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyCounted = BigDecimal.ZERO;

    @Column(name = "qty_on_hand_at_count", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyOnHandAtCount = BigDecimal.ZERO;

    @Column(name = "qty_variance", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyVariance = BigDecimal.ZERO;

    @Column(name = "write_off_reason", length = 200)
    private String writeOffReason;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_cost_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_cost_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money unitCost;

    public StockAdjustmentLine() {
    }

    public StockAdjustment getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(StockAdjustment adjustment) {
        this.adjustment = adjustment;
    }

    public int getLineNo() {
        return lineNo;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public BigDecimal getQtyCounted() {
        return qtyCounted;
    }

    public void setQtyCounted(BigDecimal qtyCounted) {
        this.qtyCounted = qtyCounted;
    }

    public BigDecimal getQtyOnHandAtCount() {
        return qtyOnHandAtCount;
    }

    public void setQtyOnHandAtCount(BigDecimal qtyOnHandAtCount) {
        this.qtyOnHandAtCount = qtyOnHandAtCount;
    }

    public BigDecimal getQtyVariance() {
        return qtyVariance;
    }

    public void setQtyVariance(BigDecimal qtyVariance) {
        this.qtyVariance = qtyVariance;
    }

    public String getWriteOffReason() {
        return writeOffReason;
    }

    public void setWriteOffReason(String writeOffReason) {
        this.writeOffReason = writeOffReason;
    }

    public Money getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Money unitCost) {
        this.unitCost = unitCost;
    }
}
