package com.guru.erp.modules.procurement.bills.domain;

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
 * ENT-030a SupplierBillLine — one line of a {@link SupplierBill}. The 3-way match anchors here:
 * {@code poLineId} + {@code grnLineId} are loose ULID refs to the ordered / received line the bill
 * line is matched against. {@code qty} is {@code numeric(18,6)} (reference precision); prices are
 * bigint minor units. {@code lineTotalAmount} carries the NET (after the linked PO line's
 * discount, before the header invoice discount + tax), matching the reference.
 *
 * <p>{@code bill} is a real same-slice FK; product / po_line / grn_line / variant are loose
 * cross-slice ULID refs (the reference keyed these to internal ids; here everything is publicId).
 */
@Entity
@Table(name = "supplier_bill_lines")
public class SupplierBillLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private SupplierBill bill;

    /** ULID public id of the matched PurchaseOrderLine; null for a standalone line (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "po_line_id", length = 26, columnDefinition = "char(26)")
    private String poLineId;

    /** ULID public id of the matched GoodsReceiptLine; null when no receipt is linked (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "grn_line_id", length = 26, columnDefinition = "char(26)")
    private String grnLineId;

    /** ULID public id of the Product (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the ProductVariant; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal qty = BigDecimal.ONE;

    @Column(name = "unit_price_amount", nullable = false)
    private long unitPriceAmount = 0L;

    /** ULID public id of the TaxCode applied to the line; nullable (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    @Column(name = "line_total_amount", nullable = false)
    private long lineTotalAmount = 0L;

    /** Per-line 3-way match outcome (wire label of {@link MatchStatus}); nullable. */
    @Column(name = "match_status", length = 20)
    private String matchStatus;

    /** US-AU-005 BAS G10 (capital) vs G11 (non-capital) purchase flag. */
    @Column(name = "is_capital_item", nullable = false)
    private boolean capitalItem = false;

    public SupplierBillLine() {
    }

    public SupplierBill getBill() {
        return bill;
    }

    public void setBill(SupplierBill bill) {
        this.bill = bill;
    }

    public String getPoLineId() {
        return poLineId;
    }

    public void setPoLineId(String poLineId) {
        this.poLineId = poLineId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public long getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public void setUnitPriceAmount(long unitPriceAmount) {
        this.unitPriceAmount = unitPriceAmount;
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

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public boolean isCapitalItem() {
        return capitalItem;
    }

    public void setCapitalItem(boolean capitalItem) {
        this.capitalItem = capitalItem;
    }
}
