package com.springboot.erp.modules.inventory.stock.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-040 StockLedger — the append-only movement ledger (reference
 * {@code StockLedger}, US-021 / FR-114–118). Every stock change is one immutable
 * row; corrections go through ADJUSTMENT / REVALUATION movements, never mutation
 * of an existing row. On-hand and availability are pure projections over this
 * table ({@code Σ qty_signed} grouped by product / variant / location / status);
 * inventory value is {@code Σ(qty_signed × unit_cost) + Σ value_delta}.
 *
 * <p>Cross-slice references (product, variant, location, batch) are held loosely
 * as ULID {@code char(26)} public ids — no hard cross-slice FK, per the
 * vertical-slice rule. {@code qtySigned} is {@code numeric(18,6)} (fixed
 * precision, never {@code double}); {@code unitCost} is {@link Money}.
 *
 * <p>Constraints reproduced in V30__inventory_stock.sql:
 * <ul>
 *   <li>{@code ck_stock_ledger_status} — status in the four buckets.</li>
 *   <li>Indexes on (product, location, occurred_at), (source_doc_type,
 *       source_doc_id), and the covering (src, product, movement_type) index.</li>
 * </ul>
 */
@Entity
@Table(name = "stock_ledger")
public class StockLedger extends BaseEntity {

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** ULID public id of the product this movement is for (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID public id of the variant; null for non-variant products. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    /** ULID public id of the location the movement occurred at (cross-slice, loose ref). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StockStatus status = StockStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 15)
    private MovementType movementType;

    /** Signed movement quantity, {@code numeric(18,6)}. Positive = in, negative = out. */
    @Column(name = "qty_signed", nullable = false, precision = 18, scale = 6)
    private BigDecimal qtySigned;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "unit_cost_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "unit_cost_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money unitCost;

    /**
     * Value-only revaluation delta (minor units, base currency). Non-zero only on
     * REVALUATION rows, which carry {@code qtySigned = 0} so on-hand quantity is
     * untouched while inventory value shifts to the latest approved bill cost.
     */
    @Column(name = "value_delta_amount", nullable = false)
    private long valueDeltaAmount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_doc_type", nullable = false, length = 20)
    private SourceDocType sourceDocType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_doc_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String sourceDocId;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    /** ULID public id of the {@link ProductBatch} this movement came from; null for non-batch rows. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "batch_id", length = 26, columnDefinition = "char(26)")
    private String batchId;

    /** ULID public id of the acting user; null when system-generated. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actor_user_id", length = 26, columnDefinition = "char(26)")
    private String actorUserId;

    @Column(name = "notes", length = 500)
    private String notes;

    public StockLedger() {
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
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

    public StockStatus getStatus() {
        return status;
    }

    public void setStatus(StockStatus status) {
        this.status = status;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQtySigned() {
        return qtySigned;
    }

    public void setQtySigned(BigDecimal qtySigned) {
        this.qtySigned = qtySigned;
    }

    public Money getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Money unitCost) {
        this.unitCost = unitCost;
    }

    public long getValueDeltaAmount() {
        return valueDeltaAmount;
    }

    public void setValueDeltaAmount(long valueDeltaAmount) {
        this.valueDeltaAmount = valueDeltaAmount;
    }

    public SourceDocType getSourceDocType() {
        return sourceDocType;
    }

    public void setSourceDocType(SourceDocType sourceDocType) {
        this.sourceDocType = sourceDocType;
    }

    public String getSourceDocId() {
        return sourceDocId;
    }

    public void setSourceDocId(String sourceDocId) {
        this.sourceDocId = sourceDocId;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
