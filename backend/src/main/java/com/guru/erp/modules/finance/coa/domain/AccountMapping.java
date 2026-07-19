package com.guru.erp.modules.finance.coa.domain;

import com.guru.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps an ERP module + semantic purpose to a concrete GL account (reference
 * {@code app.finance.models.AccountMapping} / {@code account_mapping_service.py}).
 * Allows modules (Inventory, Procurement, Sales, POS) to resolve account ids
 * dynamically at runtime instead of hard-coding names or codes. One row per
 * company per (module, purpose) pair, enforced by
 * {@code uq_account_mappings_company_module_purpose}.
 */
@Entity
@Table(name = "account_mappings")
public class AccountMapping extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private AccountModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private AccountPurpose purpose;

    /** Same-slice FK to the target GL account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    public AccountMapping() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public AccountModule getModule() {
        return module;
    }

    public void setModule(AccountModule module) {
        this.module = module;
    }

    public AccountPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(AccountPurpose purpose) {
        this.purpose = purpose;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
