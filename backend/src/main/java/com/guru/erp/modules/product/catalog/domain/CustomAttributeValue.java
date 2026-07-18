package com.guru.erp.modules.product.catalog.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * ENT-012 CustomAttributeValue — extension key/value pairs on a product or one
 * of its variants. {@code scope} is {@code product} | {@code variant}
 * ({@code ck_product_custom_attribute_values_scope}); a (product,variant,scope,
 * key) tuple is unique ({@code uq_product_custom_attribute_values_key}).
 * {@code productId}/{@code variantId} are internal bigint FKs (same slice).
 */
@Entity
@Table(
    name = "product_custom_attribute_values",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_product_custom_attribute_values_key",
        columnNames = {"product_id", "variant_id", "scope", "key"})
)
public class CustomAttributeValue extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "scope", nullable = false, length = 16)
    private String scope = "product";

    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(name = "value", columnDefinition = "text")
    private String value;

    public CustomAttributeValue() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
