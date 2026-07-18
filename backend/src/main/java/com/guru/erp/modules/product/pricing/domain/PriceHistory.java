package com.guru.erp.modules.product.pricing.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * FR-064 — append-only price history per product / variant. Written by the
 * service on every base-price change (and to schedule a future change: a row
 * with {@code effectiveFrom} in the future). Rows must never be updated or
 * deleted by application code.
 *
 * <p>The product/variant are catalog-slice entities, held by ULID publicId
 * (char(26)); no DB FK. {@code oldAmount} is nullable (the very first row for a
 * field has no prior value). Money is kept as raw minor-unit amount + currency
 * columns matching the reference — {@link com.guru.erp.platform.money.Money} is
 * not used here because {@code oldAmount} is independently nullable, which the
 * embeddable cannot express.
 *
 * <p>Table constraint reproduced in the migration:
 * {@code ck_product_price_history_field} — field in ('cost','sell').
 */
@Entity
@Table(name = "product_price_history")
public class PriceHistory extends BaseEntity {

    /** ULID publicId of the product (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID publicId of the variant, or null (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    /** Stored as the wire value of {@link PriceField} ('cost' | 'sell'). */
    @Column(name = "field", nullable = false, length = 20)
    private String field;

    /** Prior amount in minor units, or null for the first record of a field. */
    @Column(name = "old_amount")
    private Long oldAmount;

    @Column(name = "new_amount", nullable = false)
    private long newAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    /** When the recorded price takes effect (may be in the future for FR-062). */
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    /** When the change was recorded (wall-clock at write time). */
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    /** ULID publicId of the acting user, or null. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "changed_by", length = 26, columnDefinition = "char(26)")
    private String changedBy;

    public PriceHistory() {
    }

    public PriceHistory(String productId, String variantId, PriceField field, Long oldAmount,
                        long newAmount, String currency, Instant effectiveFrom,
                        Instant changedAt, String changedBy) {
        this.productId = productId;
        this.variantId = variantId;
        this.field = field.value();
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.currency = currency;
        this.effectiveFrom = effectiveFrom;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public String getProductId() {
        return productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getField() {
        return field;
    }

    public Long getOldAmount() {
        return oldAmount;
    }

    public long getNewAmount() {
        return newAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }
}
