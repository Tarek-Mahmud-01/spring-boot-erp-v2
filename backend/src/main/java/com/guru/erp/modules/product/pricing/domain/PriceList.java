package com.guru.erp.modules.product.pricing.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-013 PriceList — US-013 / FR-061. A named, optional list of prices scoped
 * to one company. The company is referenced across slice boundaries by its ULID
 * {@code companyId} (char(26)); there is no hard cross-slice FK — the service
 * resolves and validates it at the application layer.
 *
 * <p>Domain columns only — id/publicId/audit/version come from {@link BaseEntity}.
 * {@code currency} is a bare ISO-4217 code (a price list has no amount of its
 * own, so {@link com.guru.erp.platform.money.Money} does not apply here).
 *
 * <p>Table constraints reproduced in the Flyway migration:
 * <ul>
 *   <li>{@code uq_price_lists_company_name} — unique on (company_id, name).</li>
 *   <li>{@code ck_price_lists_status} — status in ('active','inactive').</li>
 * </ul>
 */
@Entity
@Table(
    name = "price_lists",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_price_lists_company_name", columnNames = {"company_id", "name"})
)
public class PriceList extends BaseEntity {

    /** ULID publicId of the owning company (cross-slice ref, no DB FK). */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    /** Stored as the lowercase wire value of {@link PriceListStatus}. */
    @Column(name = "status", nullable = false, length = 16)
    private String status = PriceListStatus.ACTIVE.value();

    @Column(name = "price_display_mode", length = 16)
    private String priceDisplayMode;

    public PriceList() {
    }

    public PriceList(String companyId, String name, String currency, String priceDisplayMode) {
        this.companyId = companyId;
        this.name = name;
        this.currency = currency;
        this.status = PriceListStatus.ACTIVE.value();
        this.priceDisplayMode = priceDisplayMode;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PriceListStatus getStatusEnum() {
        return PriceListStatus.fromValue(status);
    }

    public void setStatusEnum(PriceListStatus status) {
        this.status = status.value();
    }

    public String getPriceDisplayMode() {
        return priceDisplayMode;
    }

    public void setPriceDisplayMode(String priceDisplayMode) {
        this.priceDisplayMode = priceDisplayMode;
    }
}
