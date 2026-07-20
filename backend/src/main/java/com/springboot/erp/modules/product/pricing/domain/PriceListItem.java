package com.springboot.erp.modules.product.pricing.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-013a PriceListItem — FR-061 / FR-062 (scheduled effective-dated prices).
 *
 * <p>The parent {@link PriceList} lives in this same slice, so it is a real
 * {@code @ManyToOne} FK ({@code price_list_id}). The referenced product/variant
 * belong to the catalog slice and are held by their ULID publicId (char(26))
 * with no DB FK — resolved at the application layer.
 *
 * <p>Money is {@link Money} (long minor units + ISO-4217), mapped to the
 * reference column names {@code price_amount} / {@code price_currency}.
 *
 * <p>Table constraints reproduced in the migration:
 * <ul>
 *   <li>{@code ck_price_list_items_price_non_negative} — price_amount &gt;= 0.</li>
 *   <li>{@code ck_price_list_items_effective_range} —
 *       effective_to is null or effective_to &gt; effective_from.</li>
 *   <li>{@code ix_price_list_items_lookup} — (price_list_id, product_id,
 *       variant_id, effective_from).</li>
 * </ul>
 */
@Entity
@Table(name = "price_list_items")
public class PriceListItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    /** ULID publicId of the product (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID publicId of the variant, or null (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Embedded
    @AttributeOverride(name = "amountMinor", column = @Column(name = "price_amount", nullable = false))
    @AttributeOverride(name = "currency",
        column = @Column(name = "price_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    private Money price;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    public PriceListItem() {
    }

    public PriceListItem(PriceList priceList, String productId, String variantId,
                         Money price, Instant effectiveFrom, Instant effectiveTo) {
        this.priceList = priceList;
        this.productId = productId;
        this.variantId = variantId;
        this.price = price;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public PriceList getPriceList() {
        return priceList;
    }

    public String getProductId() {
        return productId;
    }

    public String getVariantId() {
        return variantId;
    }

    public Money getPrice() {
        return price;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }
}
