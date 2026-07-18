package com.guru.erp.modules.inventory.movements.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-042a StockTransferLine — one product line of a {@link StockTransfer}. Quantities are
 * {@code numeric(18,6)} (fractional units — kg, m³) mapped to {@link BigDecimal}, matching the
 * reference precision (never {@code double}).
 *
 * <p>{@code transfer} is a real same-slice FK; product / uom / variant are loose cross-slice ULID
 * refs. Constraints reproduced in the migration: {@code uq_transfer_lines_transfer_line} plus the
 * non-negative / positive quantity checks.
 */
@Entity
@Table(name = "stock_transfer_lines")
public class StockTransferLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

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

    @Column(name = "qty_sent", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtySent = BigDecimal.ZERO;

    @Column(name = "qty_received", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyReceived = BigDecimal.ZERO;

    @Column(name = "qty_short", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyShort = BigDecimal.ZERO;

    @Column(name = "qty_damaged", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyDamaged = BigDecimal.ZERO;

    @Column(name = "discrepancy_reason", length = 500)
    private String discrepancyReason;

    public StockTransferLine() {
    }

    public StockTransfer getTransfer() {
        return transfer;
    }

    public void setTransfer(StockTransfer transfer) {
        this.transfer = transfer;
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

    public BigDecimal getQtySent() {
        return qtySent;
    }

    public void setQtySent(BigDecimal qtySent) {
        this.qtySent = qtySent;
    }

    public BigDecimal getQtyReceived() {
        return qtyReceived;
    }

    public void setQtyReceived(BigDecimal qtyReceived) {
        this.qtyReceived = qtyReceived;
    }

    public BigDecimal getQtyShort() {
        return qtyShort;
    }

    public void setQtyShort(BigDecimal qtyShort) {
        this.qtyShort = qtyShort;
    }

    public BigDecimal getQtyDamaged() {
        return qtyDamaged;
    }

    public void setQtyDamaged(BigDecimal qtyDamaged) {
        this.qtyDamaged = qtyDamaged;
    }

    public String getDiscrepancyReason() {
        return discrepancyReason;
    }

    public void setDiscrepancyReason(String discrepancyReason) {
        this.discrepancyReason = discrepancyReason;
    }
}
