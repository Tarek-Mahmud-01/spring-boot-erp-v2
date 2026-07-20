package com.springboot.erp.modules.product.catalog.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import com.springboot.erp.platform.money.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-011a ProductVariant — FR-047 / AC-010-4. One inventoried record per
 * variant. {@code parentProductId} is the internal bigint FK to the parent
 * product (same slice, so a hard FK is fine). Money semantics mirror
 * {@link Product}.
 *
 * <p>{@code uq_product_variants_sku_ci} — partial unique on lower(sku) where
 * deleted_at is null. Non-negative amount checks reproduced in the migration.
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @Column(name = "parent_product_id", nullable = false)
    private Long parentProductId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = Map.of();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "cost_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money cost;

    @Column(name = "po_cost_amount", nullable = false)
    private long poCostAmount = 0;

    @Column(name = "landed_cost_amount", nullable = false)
    private long landedCostAmount = 0;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "sell_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "sell_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money sell;

    /** Per-variant VAT override; null = inherit the parent product's effective tax code. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    /** FR-046 (T6) — ordered list of stored image keys; index 0 is the primary image. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_keys", nullable = false, columnDefinition = "jsonb")
    private List<String> imageKeys = List.of();

    public ProductVariant() {
    }

    public Long getParentProductId() {
        return parentProductId;
    }

    public void setParentProductId(Long parentProductId) {
        this.parentProductId = parentProductId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Money getCost() {
        return cost;
    }

    public void setCost(Money cost) {
        this.cost = cost;
    }

    public long getPoCostAmount() {
        return poCostAmount;
    }

    public void setPoCostAmount(long poCostAmount) {
        this.poCostAmount = poCostAmount;
    }

    public long getLandedCostAmount() {
        return landedCostAmount;
    }

    public void setLandedCostAmount(long landedCostAmount) {
        this.landedCostAmount = landedCostAmount;
    }

    public long getCurrentCostAmount() {
        return poCostAmount + landedCostAmount;
    }

    public Money getSell() {
        return sell;
    }

    public void setSell(Money sell) {
        this.sell = sell;
    }

    public String getTaxCodeId() {
        return taxCodeId;
    }

    public void setTaxCodeId(String taxCodeId) {
        this.taxCodeId = taxCodeId;
    }

    public List<String> getImageKeys() {
        return imageKeys;
    }

    public void setImageKeys(List<String> imageKeys) {
        this.imageKeys = imageKeys;
    }
}
