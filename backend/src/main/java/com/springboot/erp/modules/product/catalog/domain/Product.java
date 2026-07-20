package com.springboot.erp.modules.product.catalog.domain;

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
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-011 Product — the core product master aggregate (FR-045–049, FR-070–074,
 * CR-001 weighed goods FR-283). Domain columns only; id/publicId/audit/version/
 * soft-delete come from {@link BaseEntity}.
 *
 * <p>Cross-slice references (category, uom, supplier, tax code) are held loosely
 * as ULID {@code char(26)} public ids — no hard cross-slice FK (app-layer
 * resolution per the vertical-slice rule). {@code cost}/{@code sell} are
 * {@link Money} (long minor units + currency); {@code po_cost}/{@code landed_cost}
 * are plain minor-unit longs owned by the procurement chain.
 *
 * <p>Constraints reproduced in V20__product_catalog.sql:
 * <ul>
 *   <li>{@code uq_products_sku_ci} — partial unique on lower(sku) where deleted_at is null.</li>
 *   <li>{@code uq_products_plu} — partial unique on plu where live and not null.</li>
 *   <li>{@code ck_products_lifecycle_state} and the non-negative amount checks.</li>
 * </ul>
 */
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** ULID public id of the owning Category (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "category_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String categoryId;

    @Column(name = "brand", length = 100)
    private String brand;

    /** ULID public id of the Supplier (E-004), loose ref. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "supplier_id", length = 26, columnDefinition = "char(26)")
    private String supplierId;

    /** ULID public id of the UnitOfMeasure (cross-slice, resolved app-side). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uom_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String uomId;

    /** ULID public id of the own tax code override; null = inherit category default. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tax_code_id", length = 26, columnDefinition = "char(26)")
    private String taxCodeId;

    /** Admin-maintained Base Cost (FR-060). */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "cost_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "cost_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money cost;

    /** Latest supplier purchase price per unit (procurement-owned), minor units. */
    @Column(name = "po_cost_amount", nullable = false)
    private long poCostAmount = 0;

    /** Allocated freight/duty/insurance per unit (procurement-owned), minor units. */
    @Column(name = "landed_cost_amount", nullable = false)
    private long landedCostAmount = 0;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amountMinor", column = @Column(name = "sell_amount", nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "sell_currency", nullable = false, length = 3, columnDefinition = "char(3)"))
    })
    private Money sell;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimensions", columnDefinition = "jsonb")
    private Map<String, Object> dimensions;

    // CR-001 (FR-283) — weighed goods / label-printing scale integration.
    @Column(name = "sold_by_weight", nullable = false)
    private boolean soldByWeight = false;

    @Column(name = "plu", length = 20)
    private String plu;

    @Column(name = "price_per_kg_amount")
    private Long pricePerKgAmount;

    @Column(name = "price_per_kg_synced_at")
    private Instant pricePerKgSyncedAt;

    @Column(name = "has_variants", nullable = false)
    private boolean hasVariants = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 16)
    private LifecycleState lifecycleState = LifecycleState.DRAFT;

    // FR-AU-008 / FR-AU-009 — independent restriction flags.
    @Column(name = "restriction_age_18", nullable = false)
    private boolean restrictionAge18 = false;

    @Column(name = "restriction_age_21", nullable = false)
    private boolean restrictionAge21 = false;

    @Column(name = "restriction_controlled_display", nullable = false)
    private boolean restrictionControlledDisplay = false;

    @Column(name = "restriction_note", length = 500)
    private String restrictionNote;

    public Product() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }

    public String getTaxCodeId() {
        return taxCodeId;
    }

    public void setTaxCodeId(String taxCodeId) {
        this.taxCodeId = taxCodeId;
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

    /** System-computed current all-in cost = po_cost + landed_cost (minor units). */
    public long getCurrentCostAmount() {
        return poCostAmount + landedCostAmount;
    }

    public Money getSell() {
        return sell;
    }

    public void setSell(Money sell) {
        this.sell = sell;
    }

    public Integer getWeightGrams() {
        return weightGrams;
    }

    public void setWeightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
    }

    public Map<String, Object> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Map<String, Object> dimensions) {
        this.dimensions = dimensions;
    }

    public boolean isSoldByWeight() {
        return soldByWeight;
    }

    public void setSoldByWeight(boolean soldByWeight) {
        this.soldByWeight = soldByWeight;
    }

    public String getPlu() {
        return plu;
    }

    public void setPlu(String plu) {
        this.plu = plu;
    }

    public Long getPricePerKgAmount() {
        return pricePerKgAmount;
    }

    public void setPricePerKgAmount(Long pricePerKgAmount) {
        this.pricePerKgAmount = pricePerKgAmount;
    }

    public Instant getPricePerKgSyncedAt() {
        return pricePerKgSyncedAt;
    }

    public void setPricePerKgSyncedAt(Instant pricePerKgSyncedAt) {
        this.pricePerKgSyncedAt = pricePerKgSyncedAt;
    }

    public boolean isHasVariants() {
        return hasVariants;
    }

    public void setHasVariants(boolean hasVariants) {
        this.hasVariants = hasVariants;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(LifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public boolean isRestrictionAge18() {
        return restrictionAge18;
    }

    public void setRestrictionAge18(boolean restrictionAge18) {
        this.restrictionAge18 = restrictionAge18;
    }

    public boolean isRestrictionAge21() {
        return restrictionAge21;
    }

    public void setRestrictionAge21(boolean restrictionAge21) {
        this.restrictionAge21 = restrictionAge21;
    }

    public boolean isRestrictionControlledDisplay() {
        return restrictionControlledDisplay;
    }

    public void setRestrictionControlledDisplay(boolean restrictionControlledDisplay) {
        this.restrictionControlledDisplay = restrictionControlledDisplay;
    }

    public String getRestrictionNote() {
        return restrictionNote;
    }

    public void setRestrictionNote(String restrictionNote) {
        this.restrictionNote = restrictionNote;
    }
}
