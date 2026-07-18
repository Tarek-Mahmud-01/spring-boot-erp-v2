package com.guru.erp.modules.product.catalog.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * ENT-011b ProductBarcode — FR-056–059. A product (optionally a variant) can
 * carry several barcodes; at most one primary per product (partial unique index
 * {@code uq_product_barcodes_one_primary}), the {@code barcode} value is globally
 * unique ({@code uq_product_barcodes_barcode}), and {@code plu_code} is partially
 * unique when set.
 *
 * <p>{@code productId}/{@code variantId} are internal bigint FKs (same slice).
 * {@code qty_per_scan} is a numeric multiplier (never money/double for amounts).
 */
@Entity
@Table(
    name = "product_barcodes",
    uniqueConstraints = @UniqueConstraint(name = "uq_product_barcodes_barcode", columnNames = "barcode")
)
public class ProductBarcode extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    // varchar (not char): barcode is a variable-length value; char() padding
    // would break exact-match lookups (findByBarcode / existsByBarcode).
    @Column(name = "barcode", nullable = false, length = 64)
    private String barcode;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private BarcodeFormat format;

    /** Quantity multiplier: 1 for a single unit, 24 for a case, etc. */
    @Column(name = "qty_per_scan", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyPerScan = BigDecimal.ONE;

    /** "manufacturer" = GTIN on the pack; "generated" = internal barcode. */
    @Column(name = "source", nullable = false, length = 16)
    private String source = "manufacturer";

    /** Short PLU code (e.g. "4011") — alternative cashier-entry key. */
    @Column(name = "plu_code", length = 10)
    private String pluCode;

    public ProductBarcode() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public void setFormat(BarcodeFormat format) {
        this.format = format;
    }

    public BigDecimal getQtyPerScan() {
        return qtyPerScan;
    }

    public void setQtyPerScan(BigDecimal qtyPerScan) {
        this.qtyPerScan = qtyPerScan;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPluCode() {
        return pluCode;
    }

    public void setPluCode(String pluCode) {
        this.pluCode = pluCode;
    }
}
