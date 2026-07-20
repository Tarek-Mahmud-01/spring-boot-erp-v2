package com.springboot.erp.modules.crm.loyalty.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-070 LoyaltyAccount — 1:1 with a customer (reference {@code LoyaltyAccount},
 * FR-208 / FR-209). Created lazily on first loyalty activity (earn / spend
 * update), mirroring the reference {@code _get_or_create_account}.
 *
 * <p>{@code customerId} is a loose ULID cross-slice ref (this slice does not
 * own the Customer aggregate) — no hard cross-slice FK, per the vertical-slice
 * rule; uniqueness is enforced at the DB level regardless. {@code companyId} is
 * denormalized onto the account at creation time (supplied by the caller, who
 * already knows which company the customer belongs to) so the loyalty config
 * lookup never needs to hard-call the CRM customer sub-slice.
 *
 * <p>{@code tierId} references a tier entry inside the company's
 * {@link LoyaltyConfig#getTiers()} JSON list; {@code null} when not enrolled /
 * below the first tier threshold.
 *
 * <p>Constraints reproduced in V61__crm_loyalty.sql:
 * <ul>
 *   <li>{@code uq_loyalty_accounts_customer} — one account per customer.</li>
 *   <li>{@code ck_loyalty_accounts_balance_non_negative} — {@code points_balance >= 0}.</li>
 * </ul>
 */
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "customer_id", nullable = false, unique = true, length = 26, columnDefinition = "char(26)")
    private String customerId;

    /** ULID of the owning company (cross-slice, loose ref) — denormalized at account creation. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "points_balance", nullable = false)
    private long pointsBalance = 0;

    /** Tier id from the company's {@code LoyaltyConfig.tiers}; null = not enrolled. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tier_id", length = 26, columnDefinition = "char(26)")
    private String tierId;

    @Column(name = "lifetime_spend_amount", nullable = false)
    private long lifetimeSpendAmount = 0;

    @Column(name = "rolling_12m_spend_amount", nullable = false)
    private long rolling12mSpendAmount = 0;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "spend_currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String spendCurrency;

    @Column(name = "tier_recalculated_at")
    private Instant tierRecalculatedAt;

    public LoyaltyAccount() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public long getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(long pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public String getTierId() {
        return tierId;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public long getLifetimeSpendAmount() {
        return lifetimeSpendAmount;
    }

    public void setLifetimeSpendAmount(long lifetimeSpendAmount) {
        this.lifetimeSpendAmount = lifetimeSpendAmount;
    }

    public long getRolling12mSpendAmount() {
        return rolling12mSpendAmount;
    }

    public void setRolling12mSpendAmount(long rolling12mSpendAmount) {
        this.rolling12mSpendAmount = rolling12mSpendAmount;
    }

    public String getSpendCurrency() {
        return spendCurrency;
    }

    public void setSpendCurrency(String spendCurrency) {
        this.spendCurrency = spendCurrency;
    }

    public Instant getTierRecalculatedAt() {
        return tierRecalculatedAt;
    }

    public void setTierRecalculatedAt(Instant tierRecalculatedAt) {
        this.tierRecalculatedAt = tierRecalculatedAt;
    }
}
