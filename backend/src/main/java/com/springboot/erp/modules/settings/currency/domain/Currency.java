package com.springboot.erp.modules.settings.currency.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-018 Currency — global reference data shared across all companies
 * (intentionally not tenant-scoped). Exactly one row may carry
 * {@code isDefault = true}, guarded by a partial unique index in the
 * migration (Postgres has no declarative partial-unique constraint, so it
 * lives in the DDL, not here).
 *
 * <p>Domain columns only — id/publicId/audit/version come from
 * {@link BaseEntity}. {@code code} is an immutable ISO-4217 identifier once
 * issued (enforced in the service, not by JPA).
 *
 * <p>Table constraints reproduced in the Flyway migration:
 * <ul>
 *   <li>{@code uq_currencies_code} — unique on {@code code}.</li>
 *   <li>{@code ck_currencies_decimal_places_range} — {@code 0..4}.</li>
 *   <li>{@code uq_currencies_default} — partial unique index on
 *       {@code is_default} where {@code is_default = true}.</li>
 * </ul>
 */
@Entity
@Table(
    name = "currencies",
    uniqueConstraints = @UniqueConstraint(name = "uq_currencies_code", columnNames = "code")
)
public class Currency extends BaseEntity {

    /** ISO 4217 — exactly 3 uppercase letters. Immutable after creation. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code", nullable = false, updatable = false, length = 3, columnDefinition = "char(3)")
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "short_name", nullable = false, length = 20)
    private String shortName;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "symbol", nullable = false, length = 8)
    private String symbol;

    /** Range 0..4, enforced by ck_currencies_decimal_places_range in the DDL. */
    @Column(name = "decimal_places", nullable = false)
    private int decimalPlaces = 2;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected Currency() {
    }

    public Currency(String code, String name, String shortName, String country,
                    String symbol, int decimalPlaces, boolean isActive, boolean isDefault) {
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        this.country = country;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
        this.isActive = isActive;
        this.isDefault = isDefault;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /** Derived status shown to clients: {@code "active"} | {@code "inactive"}. */
    public String getStatus() {
        return isActive ? "active" : "inactive";
    }
}
