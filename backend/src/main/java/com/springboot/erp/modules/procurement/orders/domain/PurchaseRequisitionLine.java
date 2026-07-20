package com.springboot.erp.modules.procurement.orders.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-027a PurchaseRequisitionLine — one product line of a {@link PurchaseRequisition}. Quantity is
 * {@code numeric(18,6)} {@link BigDecimal} (reference precision). {@code unitPrice} and
 * {@code lineTotal} are {@link Money} (long minor units + currency).
 *
 * <p>{@code requisition} is a real same-slice FK; product / uom / variant / preferred supplier /
 * tax code are loose cross-slice ULID refs. Constraints reproduced in V41:
 * {@code uq_pr_lines_pr_line} plus {@code ck_pr_lines_qty_positive}.
 */
@Entity
@Table(name = "purchase_requisition_lines")
public class PurchaseRequisitionLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "pr_id", nullable = false)
    private PurchaseRequisition requisition;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** ULID public id of the Product; null for free-text lines (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the ProductVariant; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal qty = BigDecimal.ONE;

    /** ULID public id of the UnitOfMeasure; null = product default (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uom_id", length = 26, columnDefinition = "char(26)")
    private String uomId;

    /** ULID public id of the preferred Supplier for this line; null = none (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "preferred_supplier_id", length = 26, columnDefinition = "char(26)")
    private String preferredSupplierId;

    @Column(name = "description", length = 500)
    private String description;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_price_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money unitPrice;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /** ULID public id of the tax code applied to this line; null = none (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    /** Line total (minor units), net of the per-line discount — currency follows the unit price. */
    @Column(name = "line_total_amount", nullable = false)
    private long lineTotalAmount = 0L;

    @Convert(converter = PrLineStatusConverter.class)
    @Column(name = "status", nullable = false, length = 12)
    private PrLineStatus status = PrLineStatus.OPEN;

    public PurchaseRequisitionLine() {
    }

    public PurchaseRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(PurchaseRequisition requisition) {
        this.requisition = requisition;
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

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public String getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(String preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public long getLineTotalAmount() {
        return lineTotalAmount;
    }

    public void setLineTotalAmount(long lineTotalAmount) {
        this.lineTotalAmount = lineTotalAmount;
    }

    public PrLineStatus getStatus() {
        return status;
    }

    public void setStatus(PrLineStatus status) {
        this.status = status;
    }
}
