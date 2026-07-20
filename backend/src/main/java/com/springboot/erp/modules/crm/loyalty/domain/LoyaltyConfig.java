package com.springboot.erp.modules.crm.loyalty.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-072 LoyaltyConfig — one loyalty program per company (reference
 * {@code LoyaltyConfig}, US-039 / FR-205-207). Exactly one row per company
 * ({@code unique(company_id)}), mirroring the per-company singleton pattern
 * used by {@code InventoryValuationConfig}.
 *
 * <p>{@code companyId} is a loose ULID cross-slice ref (settings module) — no
 * hard cross-slice FK, per the vertical-slice rule.
 *
 * <p>{@code earnRule} = {@code {"currencyUnitPerPoint": number, "eligibleCategoryIds": [ids]}}
 * (reference FR-205 earn rule). {@code redeemRule} = {@code {"pointsPerCurrencyUnit": int,
 * "minBalanceForRedemption": int}} (reference FR-205 redeem rule). {@code expiryMonths}
 * is nullable — {@code null} means points never expire (reference FR-207).
 * {@code tiers} is a JSON list of {@code {"id","code","name","minSpendAmount","currency",
 * "earnMultiplier"}} tier definitions (reference FR-206) — kept as one JSON column,
 * not a child table, matching the reference model's design (tier CRUD only ever
 * rewrites the whole list under one row lock).
 *
 * <p>Constraints reproduced in V61__crm_loyalty.sql:
 * <ul>
 *   <li>{@code uq_loyalty_configs_company} — one config per company.</li>
 * </ul>
 */
@Entity
@Table(name = "loyalty_configs")
public class LoyaltyConfig extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, unique = true, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "name", nullable = false, length = 120)
    private String name = "Loyalty Program";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "earn_rule", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> earnRule = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redeem_rule", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> redeemRule = Map.of();

    /** Months until earned points expire; {@code null} = never expires (FR-207). */
    @Column(name = "expiry_months")
    private Integer expiryMonths = 24;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tiers", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> tiers = List.of();

    public LoyaltyConfig() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, Object> getEarnRule() {
        return earnRule;
    }

    public void setEarnRule(Map<String, Object> earnRule) {
        this.earnRule = earnRule;
    }

    public Map<String, Object> getRedeemRule() {
        return redeemRule;
    }

    public void setRedeemRule(Map<String, Object> redeemRule) {
        this.redeemRule = redeemRule;
    }

    public Integer getExpiryMonths() {
        return expiryMonths;
    }

    public void setExpiryMonths(Integer expiryMonths) {
        this.expiryMonths = expiryMonths;
    }

    public List<Map<String, Object>> getTiers() {
        return tiers;
    }

    public void setTiers(List<Map<String, Object>> tiers) {
        this.tiers = tiers;
    }
}
