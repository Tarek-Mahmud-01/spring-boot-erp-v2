package com.guru.erp.modules.inventory.counts.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-045 StockOpening — the opening inventory balance per
 * (company, product, variant, location) at go-live (US / FR opening balances).
 *
 * <p>Two-stage lifecycle Draft -&gt; Posted ({@link StockOpeningStatus}). A
 * POSTED row is authoritative and immutable; on post the reference also writes a
 * StockLedger row and a JournalEntry. Those live in sibling slices, so this slice
 * publishes an outbox event (documented in the service) rather than taking a hard
 * cross-slice compile dependency; {@code journalEntryId} records the resulting
 * voucher's ULID once known.
 *
 * <p>Cross-slice references ({@code companyId}, {@code productId},
 * {@code variantId}, {@code locationId}, {@code glAccountId}) are loose ULID
 * {@code char(26)} public ids. {@code openingQty} is NUMERIC(18,6) (fractional
 * / weighed units); {@code unitCostAmount} is integer minor units + a 3-char
 * currency (money rule — long, never double).
 *
 * <p>Constraints reproduced in V32__inventory_counts.sql:
 * partial unique {@code uq_stock_opening_company_product_variant_location}
 * on (company_id, coalesce(variant_id,'0'), product_id, location_id) where
 * deleted_at is null; {@code ck_stock_opening_status};
 * {@code ck_stock_opening_qty_nonneg (opening_qty >= 0)};
 * {@code ck_stock_opening_cost_nonneg (unit_cost_amount >= 0)}.
 */
@Entity
@Table(name = "stock_opening_entries")
public class StockOpening extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** Optional variant this opening is for; null = product has no variants. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Column(name = "opening_qty", nullable = false, precision = 18, scale = 6)
    private BigDecimal openingQty;

    /** Unit cost minor units (money rule — long, never double). */
    @Column(name = "unit_cost_amount", nullable = false)
    private long unitCostAmount = 0;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "unit_cost_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String unitCostCurrency = "USD";

    /** ULID public id of the counter GL account (usually Owner's Equity). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "gl_account_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String glAccountId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Convert(converter = StockOpeningStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private StockOpeningStatus status = StockOpeningStatus.DRAFT;

    @Column(name = "posted_at")
    private Instant postedAt;

    /** ULID of the actor who posted (loose ref to user_management). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "posted_by", length = 26, columnDefinition = "char(26)")
    private String postedBy;

    /** ULID public id of the JournalEntry created at post-time. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "journal_entry_id", length = 26, columnDefinition = "char(26)")
    private String journalEntryId;

    public StockOpening() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getOpeningQty() {
        return openingQty;
    }

    public void setOpeningQty(BigDecimal openingQty) {
        this.openingQty = openingQty;
    }

    public long getUnitCostAmount() {
        return unitCostAmount;
    }

    public void setUnitCostAmount(long unitCostAmount) {
        this.unitCostAmount = unitCostAmount;
    }

    public String getUnitCostCurrency() {
        return unitCostCurrency;
    }

    public void setUnitCostCurrency(String unitCostCurrency) {
        this.unitCostCurrency = unitCostCurrency;
    }

    public String getGlAccountId() {
        return glAccountId;
    }

    public void setGlAccountId(String glAccountId) {
        this.glAccountId = glAccountId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public StockOpeningStatus getStatus() {
        return status;
    }

    public void setStatus(StockOpeningStatus status) {
        this.status = status;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(String journalEntryId) {
        this.journalEntryId = journalEntryId;
    }
}
