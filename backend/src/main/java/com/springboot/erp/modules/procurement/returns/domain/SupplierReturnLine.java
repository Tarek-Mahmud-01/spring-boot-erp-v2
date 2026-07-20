package com.springboot.erp.modules.procurement.returns.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-031a SupplierReturnLine — one returned line of a {@link SupplierReturn}. Each line references
 * a goods-receipt line (the units being sent back) and carries the returned quantity as
 * {@code numeric(18,6)} {@link BigDecimal} (reference precision).
 *
 * <p>{@code supplierReturn} is a real same-slice FK; {@code grnLineId} / {@code variantId} are loose
 * cross-slice ULID refs. Constraint reproduced in the migration:
 * {@code ck_supplier_return_lines_qty_positive}.
 */
@Entity
@Table(name = "supplier_return_lines")
public class SupplierReturnLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "return_id", nullable = false)
    private SupplierReturn supplierReturn;

    /** ULID public id of the source GoodsReceiptLine (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_line_id", length = 26, columnDefinition = "char(26)")
    private String grnLineId;

    /** ULID public id of the ProductVariant returned; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal qty = BigDecimal.ZERO;

    @Column(name = "reason", length = 500)
    private String reason;

    public SupplierReturnLine() {
    }

    public SupplierReturn getSupplierReturn() {
        return supplierReturn;
    }

    public void setSupplierReturn(SupplierReturn supplierReturn) {
        this.supplierReturn = supplierReturn;
    }

    public String getGrnLineId() {
        return grnLineId;
    }

    public void setGrnLineId(String grnLineId) {
        this.grnLineId = grnLineId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
