package com.springboot.erp.modules.finance.coa.domain;

import com.springboot.erp.platform.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ENT-016 ChartOfAccounts (reference {@code app.finance.models.Account}, US-043 /
 * FR-223-227) — one node in the company's chart-of-accounts NESTED-SET tree
 * (Joe Celko modified preorder traversal). {@code lft}/{@code rgt}/{@code depth}
 * are maintained ONLY by {@link com.springboot.erp.modules.finance.coa.service.NestedSetService}'s
 * incremental shifts — nothing else in this slice ever writes them directly.
 *
 * <p>Invariant for every node A and descendant D: {@code A.lft < D.lft < D.rgt < A.rgt}.
 * Renames / status flips / currency / posting_type changes do NOT touch lft/rgt;
 * only an actual re-parent (move) does.
 *
 * <p><b>BaseEntity + soft delete note:</b> Account extends {@link BaseEntity} for
 * platform consistency (its {@code @SQLRestriction("deleted_at is null")} keeps
 * deleted rows out of ordinary reads, same as every other entity), but the
 * reference's delete path is a HARD delete (FR-226 / AC-043-4: only a childless,
 * posting-free leaf may be deleted at all) that immediately triggers
 * {@code close_delete_gap}. The nested-set shift SQL in {@code NestedSetService}
 * still defensively filters {@code deleted_at is null} — mirroring the
 * reference's own predicate in {@code nested_set.py} — so that IF a row were
 * ever soft-deleted instead, it could never silently corrupt a lft/rgt shift.
 */
@Entity
@Table(name = "accounts")
public class Account extends BaseEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "company_id", nullable = false, length = 26, columnDefinition = "char(26)")
    private String companyId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AccountType type;

    /** Same-slice self-referencing FK — the nested-set parent. Null for a root node. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Account parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "posting_type", nullable = false, length = 16)
    private AccountPostingType postingType = AccountPostingType.POSTING;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3, columnDefinition = "char(3)")
    private String currency;

    @Convert(converter = AccountStatusConverter.class)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "opening_debit_amount", nullable = false)
    private long openingDebitAmount = 0;

    @Column(name = "opening_credit_amount", nullable = false)
    private long openingCreditAmount = 0;

    @Column(name = "opening_date")
    private Instant openingDate;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "opening_location_id", length = 26, columnDefinition = "char(26)")
    private String openingLocationId;

    @Column(name = "lft", nullable = false)
    private int lft = 0;

    @Column(name = "rgt", nullable = false)
    private int rgt = 0;

    @Column(name = "depth", nullable = false)
    private int depth = 0;

    public Account() {
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
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

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public Account getParent() {
        return parent;
    }

    public void setParent(Account parent) {
        this.parent = parent;
    }

    public AccountPostingType getPostingType() {
        return postingType;
    }

    public void setPostingType(AccountPostingType postingType) {
        this.postingType = postingType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public long getOpeningDebitAmount() {
        return openingDebitAmount;
    }

    public void setOpeningDebitAmount(long openingDebitAmount) {
        this.openingDebitAmount = openingDebitAmount;
    }

    public long getOpeningCreditAmount() {
        return openingCreditAmount;
    }

    public void setOpeningCreditAmount(long openingCreditAmount) {
        this.openingCreditAmount = openingCreditAmount;
    }

    public Instant getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(Instant openingDate) {
        this.openingDate = openingDate;
    }

    public String getOpeningLocationId() {
        return openingLocationId;
    }

    public void setOpeningLocationId(String openingLocationId) {
        this.openingLocationId = openingLocationId;
    }

    public int getLft() {
        return lft;
    }

    public void setLft(int lft) {
        this.lft = lft;
    }

    public int getRgt() {
        return rgt;
    }

    public void setRgt(int rgt) {
        this.rgt = rgt;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }
}
