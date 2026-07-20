package com.springboot.erp.modules.procurement.landed.domain;

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
 * Bundle-component row for a {@link UnitBarcode} with {@code isBundle=true} (reference
 * {@code app.procurement.models.UnitBarcodeItem}). Each row names one product / variant the bundle
 * barcode covers, with a qty. {@code unitBarcode} is a real same-slice FK; product / variant /
 * grnLine are loose ULID {@code char(26)} cross-slice refs. Reproduces
 * {@code ck_unit_barcode_items_qty_positive}.
 */
@Entity
@Table(name = "unit_barcode_items")
public class UnitBarcodeItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "unit_barcode_id", nullable = false)
    private UnitBarcode unitBarcode;

    /** ULID public id of the source GoodsReceiptLine (cross-slice); optional. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_line_id", length = 26, columnDefinition = "char(26)")
    private String grnLineId;

    /** ULID public id of the component Product (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the component ProductVariant (cross-slice); optional. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal qty = BigDecimal.ONE;

    public UnitBarcodeItem() {
    }

    public UnitBarcode getUnitBarcode() {
        return unitBarcode;
    }

    public void setUnitBarcode(UnitBarcode unitBarcode) {
        this.unitBarcode = unitBarcode;
    }

    public String getGrnLineId() {
        return grnLineId;
    }

    public void setGrnLineId(String grnLineId) {
        this.grnLineId = grnLineId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
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
}
