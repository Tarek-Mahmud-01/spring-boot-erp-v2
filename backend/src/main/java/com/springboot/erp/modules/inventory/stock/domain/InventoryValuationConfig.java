package com.springboot.erp.modules.inventory.stock.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-041 InventoryValuationConfig — the per-company inventory valuation method
 * (reference {@code InventoryValuationConfig}, US-021 / FR-116). Exactly one row
 * per company ({@code unique(company_id)}). The method (FIFO / MOVING_AVERAGE /
 * STANDARD) is chosen before any stock moves; once the first movement is posted
 * the config {@code locks} and the method can no longer change (AC-021-3).
 *
 * <p>{@code companyId} is a loose ULID cross-slice ref (settings module). The
 * method enum names are already the wire values, so {@code @Enumerated(STRING)}
 * matches {@code ck_valuation_config_method} directly.
 */
@Entity
@Table(name = "inventory_valuation_configs")
public class InventoryValuationConfig extends BaseEntity {

    /** ULID public id of the owning Company (cross-slice, loose ref). Unique. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, unique = true, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private ValuationMethod method = ValuationMethod.MOVING_AVERAGE;

    /** True once a stock movement exists — locks the valuation method (FR-116). */
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    public InventoryValuationConfig() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public ValuationMethod getMethod() {
        return method;
    }

    public void setMethod(ValuationMethod method) {
        this.method = method;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
