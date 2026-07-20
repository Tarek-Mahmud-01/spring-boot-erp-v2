package com.springboot.erp.modules.settings.location.domain;

import com.springboot.erp.modules.settings.company.domain.Company;
import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-002 Location — a physical branch (store / warehouse / office / …) that a
 * company operates from. Ported from the reference {@code app.locations} module.
 *
 * <p>A location belongs to exactly one {@link Company} (reference: bigint FK to
 * {@code companies.id} with {@code on delete restrict}). Uniqueness of
 * {@code code} is scoped to the owning company.
 *
 * <p>Domain columns only — id / publicId / audit / soft-delete / version come from
 * {@link BaseEntity}.
 *
 * <p>Table constraints (reproduced in the Flyway migration):
 * <ul>
 *   <li>unique {@code (company_id, code)} — code unique within a company.</li>
 *   <li>FK {@code company_id → companies(id)} on delete restrict.</li>
 *   <li>check {@code type in (…)} — one of the eight {@link LocationType} values.</li>
 *   <li>check {@code status in ('active','inactive')}.</li>
 *   <li>check {@code price_display_mode is null or in ('INCLUSIVE','EXCLUSIVE')}.</li>
 * </ul>
 */
@Entity
@Table(
    name = "locations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_locations_company_code",
        columnNames = {"company_id", "code"}))
public class Location extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address", nullable = false, columnDefinition = "jsonb")
    private Address address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "public_email", length = 200)
    private String publicEmail;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_price_list_id", length = 26, columnDefinition = "char(26)")
    private String defaultPriceListId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_tax_code_id", length = 26, columnDefinition = "char(26)")
    private String defaultTaxCodeId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = LocationStatus.ACTIVE.value();

    @Column(name = "price_display_mode", length = 16)
    private String priceDisplayMode;

    public Location() {
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPublicEmail() {
        return publicEmail;
    }

    public void setPublicEmail(String publicEmail) {
        this.publicEmail = publicEmail;
    }

    public String getDefaultPriceListId() {
        return defaultPriceListId;
    }

    public void setDefaultPriceListId(String defaultPriceListId) {
        this.defaultPriceListId = defaultPriceListId;
    }

    public String getDefaultTaxCodeId() {
        return defaultTaxCodeId;
    }

    public void setDefaultTaxCodeId(String defaultTaxCodeId) {
        this.defaultTaxCodeId = defaultTaxCodeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriceDisplayMode() {
        return priceDisplayMode;
    }

    public void setPriceDisplayMode(String priceDisplayMode) {
        this.priceDisplayMode = priceDisplayMode;
    }
}
