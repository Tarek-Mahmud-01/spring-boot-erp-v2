package com.springboot.erp.modules.product.pricing.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-013b ProductLocationOverride — FR-063. A product (optionally a variant)
 * may carry a different price at a given location, resolved ahead of the base
 * price / price lists. Product, variant and location all belong to other slices
 * and are held by ULID publicId (char(26)); no DB FK — resolved at the
 * application layer.
 *
 * <p>Money is {@link Money} (long minor units + ISO-4217), mapped to the
 * reference column names {@code price_amount} / {@code price_currency}.
 *
 * <p>Table constraints reproduced in the migration:
 * <ul>
 *   <li>{@code ck_product_location_overrides_price_non_negative} —
 *       price_amount &gt;= 0.</li>
 *   <li>{@code uq_product_location_override} — partial unique index on
 *       (product_id, location_id, variant_id) where deleted_at is null.</li>
 * </ul>
 */
@Entity
@Table(name = "product_location_overrides")
public class ProductLocationOverride extends BaseEntity {

    /** ULID publicId of the product (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String productId;

    /** ULID publicId of the location (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "location_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String locationId;

    /** ULID publicId of the variant, or null (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "variant_id", length = 26, columnDefinition = "char(26)")
    private String variantId;

    @Embedded
    @AttributeOverride(name = "amountMinor", column = @Column(name = "price_amount", nullable = false))
    @AttributeOverride(name = "currency",
        column = @Column(name = "price_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    private Money price;

    public ProductLocationOverride() {
    }

    public ProductLocationOverride(String productId, String locationId, String variantId, Money price) {
        this.productId = productId;
        this.locationId = locationId;
        this.variantId = variantId;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public String getLocationId() {
        return locationId;
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
}
