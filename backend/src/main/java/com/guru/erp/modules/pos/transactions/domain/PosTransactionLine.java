package com.guru.erp.modules.pos.transactions.domain;

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
 * ENT-PosTransactionLine — one cart/receipt line of a {@link PosTransaction} (reference
 * {@code app.pos.models.PosTransactionLine}). Price / tax / restriction / SKU / name are
 * SNAPSHOT at add time so a later catalogue change can never rewrite a posted sale.
 *
 * <p>{@code transaction} is a real same-slice FK; product / variant / tax-code are loose
 * cross-slice ULID refs (this slice never hard-calls the product module). {@code qty} is
 * {@code numeric(12,3)} to support weighed goods (fractional kg); non-weighed products are
 * enforced whole-unit-only in the service layer, matching the reference. Constraints reproduced
 * in V51: {@code ck_pos_line_qty_positive} and {@code uq_pos_line_no}.
 */
@Entity
@Table(name = "pos_transaction_lines")
public class PosTransactionLine extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private PosTransaction transaction;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** ULID public id of the Product (cross-slice, required). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the ProductVariant; null for non-variant products (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "barcode", length = 64)
    private String barcode;

    @Column(name = "qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal qty;

    /** Un-rung same-product BOGO free units dispensed on top of {@code qty}; priced at zero. */
    @Column(name = "free_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal freeQty = BigDecimal.ZERO;

    @Column(name = "unit_price_amount", nullable = false)
    private long unitPriceAmount;

    /** Pre-promotion base price per unit; equals {@code unitPriceAmount} when no promo applied. */
    @Column(name = "base_price_amount", nullable = false)
    private long basePriceAmount = 0L;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount = 0L;

    /** Display name of the first applied item-level promotion (cart chip / receipt). */
    @Column(name = "promotion_label", length = 200)
    private String promotionLabel;

    /** ULID public id of the tax code applied; null = none (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_inclusive", nullable = false)
    private boolean taxInclusive = false;

    @Column(name = "tax_amount", nullable = false)
    private long taxAmount = 0L;

    @Column(name = "line_net_amount", nullable = false)
    private long lineNetAmount = 0L;

    @Column(name = "line_total_amount", nullable = false)
    private long lineTotalAmount = 0L;

    @Column(name = "is_restricted_18", nullable = false)
    private boolean restricted18 = false;

    @Column(name = "is_restricted_21", nullable = false)
    private boolean restricted21 = false;

    @Column(name = "is_restricted_controlled_display", nullable = false)
    private boolean restrictedControlledDisplay = false;

    /** ULID public id of the sale line this refund line reverses; null on a sale (cross-slice). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "refund_of_line_id", length = 26, columnDefinition = "char(26)")
    private String refundOfLineId;

    @Column(name = "refunded_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal refundedQty = BigDecimal.ZERO;

    @Column(name = "void_reason", length = 120)
    private String voidReason;

    /** Awarding promotion's public id when this line is an auto-added "buy X get Y" reward. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "reward_promotion_id", length = 26, columnDefinition = "char(26)")
    private String rewardPromotionId;

    /** Back-derived physical weight (kg) for a decoded scale-label barcode line; null otherwise. */
    @Column(name = "weighed_qty_kg", precision = 12, scale = 3)
    private BigDecimal weighedQtyKg;

    /** Moving-average unit cost snapshot stamped when the sale posts to GL; null until then. */
    @Column(name = "unit_cost_amount")
    private Long unitCostAmount;

    public PosTransactionLine() {
    }

    public PosTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(PosTransaction transaction) {
        this.transaction = transaction;
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

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(BigDecimal freeQty) {
        this.freeQty = freeQty;
    }

    public long getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public void setUnitPriceAmount(long unitPriceAmount) {
        this.unitPriceAmount = unitPriceAmount;
    }

    public long getBasePriceAmount() {
        return basePriceAmount;
    }

    public void setBasePriceAmount(long basePriceAmount) {
        this.basePriceAmount = basePriceAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getPromotionLabel() {
        return promotionLabel;
    }

    public void setPromotionLabel(String promotionLabel) {
        this.promotionLabel = promotionLabel;
    }

    public String getTaxCodeId() {
        return taxCodeId;
    }

    public void setTaxCodeId(String taxCodeId) {
        this.taxCodeId = taxCodeId;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public boolean isTaxInclusive() {
        return taxInclusive;
    }

    public void setTaxInclusive(boolean taxInclusive) {
        this.taxInclusive = taxInclusive;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public long getLineNetAmount() {
        return lineNetAmount;
    }

    public void setLineNetAmount(long lineNetAmount) {
        this.lineNetAmount = lineNetAmount;
    }

    public long getLineTotalAmount() {
        return lineTotalAmount;
    }

    public void setLineTotalAmount(long lineTotalAmount) {
        this.lineTotalAmount = lineTotalAmount;
    }

    public boolean isRestricted18() {
        return restricted18;
    }

    public void setRestricted18(boolean restricted18) {
        this.restricted18 = restricted18;
    }

    public boolean isRestricted21() {
        return restricted21;
    }

    public void setRestricted21(boolean restricted21) {
        this.restricted21 = restricted21;
    }

    public boolean isRestrictedControlledDisplay() {
        return restrictedControlledDisplay;
    }

    public void setRestrictedControlledDisplay(boolean restrictedControlledDisplay) {
        this.restrictedControlledDisplay = restrictedControlledDisplay;
    }

    public String getRefundOfLineId() {
        return refundOfLineId;
    }

    public void setRefundOfLineId(String refundOfLineId) {
        this.refundOfLineId = refundOfLineId;
    }

    public BigDecimal getRefundedQty() {
        return refundedQty;
    }

    public void setRefundedQty(BigDecimal refundedQty) {
        this.refundedQty = refundedQty;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public String getRewardPromotionId() {
        return rewardPromotionId;
    }

    public void setRewardPromotionId(String rewardPromotionId) {
        this.rewardPromotionId = rewardPromotionId;
    }

    public BigDecimal getWeighedQtyKg() {
        return weighedQtyKg;
    }

    public void setWeighedQtyKg(BigDecimal weighedQtyKg) {
        this.weighedQtyKg = weighedQtyKg;
    }

    public Long getUnitCostAmount() {
        return unitCostAmount;
    }

    public void setUnitCostAmount(Long unitCostAmount) {
        this.unitCostAmount = unitCostAmount;
    }
}
