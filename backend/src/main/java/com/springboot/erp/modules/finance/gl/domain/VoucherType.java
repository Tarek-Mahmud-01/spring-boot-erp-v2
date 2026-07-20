package com.springboot.erp.modules.finance.gl.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ENT-046b VoucherType — drives the Voucher Register and constrains {@link JournalEntry#getVoucherType()}
 * (reference {@code app.finance.models.VoucherType}). Global (not per-company) so accounting codes stay
 * consistent across the whole deployment.
 *
 * <p>{@code operational=true} types (e.g. V-001 Sale) are auto-posted by an owning ERP module and cannot
 * be chosen for a manual journal entry; {@code operational=false} types (e.g. V-006 General Journal) are
 * manual journal categories a user may pick from the Journal Entry UI. The reference's
 * {@code ck_voucher_types_operational_xor_module} check constraint (operational implies a non-null
 * {@code operationalModule}, and vice versa) is reproduced in the migration.
 */
@Entity
@Table(name = "voucher_types")
public class VoucherType extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    /** Voucher-number prefix, e.g. "JV", "PRV", "GRN" — drives {@code <PREFIX>-<YYYY>-<NNNNNN>} numbering. */
    @Column(name = "prefix", nullable = false, length = 8)
    private String prefix = "JV";

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description = "";

    @Column(name = "operational", nullable = false)
    private boolean operational = false;

    @Column(name = "operational_module", length = 40)
    private String operationalModule;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public VoucherType() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public String getOperationalModule() {
        return operationalModule;
    }

    public void setOperationalModule(String operationalModule) {
        this.operationalModule = operationalModule;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
