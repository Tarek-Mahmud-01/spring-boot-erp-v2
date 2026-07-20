package com.springboot.erp.modules.procurement.orders.domain;

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
 * ENT-028a PurchaseOrderLine — one product line of a {@link PurchaseOrder}. Quantities are
 * {@code numeric(18,6)} {@link BigDecimal} (a weighed product is bought as 12.675 kg, matching the
 * reference and the stock ledger). {@code unitPrice} and {@code lineTotal} are {@link Money}
 * (long minor units + currency).
 *
 * <p>{@code purchaseOrder} is a real same-slice FK; product / uom / variant / tax code are loose
 * cross-slice ULID refs. Constraints reproduced in V41: {@code uq_po_lines_po_line},
 * {@code ck_po_lines_qty_positive}, and {@code ck_po_lines_discount}.
 */
@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** ULID public id of the Product; null for free-text lines (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", length = 26, columnDefinition = "char(26)")
    private String productId;

    @Column(name = "qty_ordered", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyOrdered = BigDecimal.ZERO;

    @Column(name = "qty_received_total", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtyReceivedTotal = BigDecimal.ZERO;

    /** ULID public id of the UnitOfMeasure; null = product default (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uom_id", length = 26, columnDefinition = "char(26)")
    private String uomId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_price_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /** ULID public id of the tax code; null = none (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    /** Tax rate snapshot at PO date (percent); null when no tax code applied. */
    @Column(name = "tax_rate_percent", precision = 10, scale = 4)
    private BigDecimal taxRatePercent;

    /** ULID public id of the ProductVariant; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    /** Snapshot label of the variant at create time (attributes joined, else SKU). */
    @Column(name = "variant_name", length = 500)
    private String variantName;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "line_total_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "line_total_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money lineTotal;

    public PurchaseOrderLine() {
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
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

    public BigDecimal getQtyOrdered() {
        return qtyOrdered;
    }

    public void setQtyOrdered(BigDecimal qtyOrdered) {
        this.qtyOrdered = qtyOrdered;
    }

    public BigDecimal getQtyReceivedTotal() {
        return qtyReceivedTotal;
    }

    public void setQtyReceivedTotal(BigDecimal qtyReceivedTotal) {
        this.qtyReceivedTotal = qtyReceivedTotal;
    }

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getTaxCodeId() {
        return taxCodeId;
    }

    public void setTaxCodeId(String taxCodeId) {
        this.taxCodeId = taxCodeId;
    }

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
    }

    public void setTaxRatePercent(BigDecimal taxRatePercent) {
        this.taxRatePercent = taxRatePercent;
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

    public Money getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(Money lineTotal) {
        this.lineTotal = lineTotal;
    }
}
